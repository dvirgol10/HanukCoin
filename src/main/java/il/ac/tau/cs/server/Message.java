package main.java.il.ac.tau.cs.server;


import main.java.il.ac.tau.cs.hanukcoin.Groups;
import main.java.il.ac.tau.cs.hanukcoin.HanukCoinUtils;
import main.java.il.ac.tau.cs.hanukcoin.HostPortPair;
import main.java.il.ac.tau.cs.hanukcoin.block.Block;
import main.java.il.ac.tau.cs.hanukcoin.block.LocalBlockChain;
import main.java.il.ac.tau.cs.hanukcoin.node.LocalNodeList;
import main.java.il.ac.tau.cs.hanukcoin.node.Node;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/*
 * Message is at least 20 byte/160 bit long.
 * message format:
 * 32      bit represent request or response (1 for request, 2 for response)
 * 32      bit start of nodes, value is 0xBeefBeef
 * 32      bit nodes count
 * 64      bit or more, nodes
 * 32      bit start blocks, value is 0xDeadDead
 * 32      bit blocks count
 * 288 * x bit blocks (x is blocks count)
 */

/**
 * useful functions for handling messages
 */
public class Message {


    /**
     * builds a message according to the format
     *
     * @param nodeList   the list of up to 30 minutes active nodes
     * @param isRequest  whether the message os a request or not
     * @param blockChain the current BlockChain
     * @return message as a byte array
     */
    private static byte[] buildMessage(Map<HostPortPair, Node> nodeList, boolean isRequest, List<Block> blockChain) {
        byte[] data = new byte[14 + LocalNodeList.nodeListLengthSum(nodeList) + 36 * blockChain.size()];
        data[3] = (byte) (isRequest ? 1 : 2);
        HanukCoinUtils.intIntoBytes(data, 4, 0xBeefBeef);
        data[8] = (byte) nodeList.size();
        int index = 9;
        for (Node value : nodeList.values()) {
            if (!value.getIsNew()) {
                System.arraycopy(value.getData(), 0, data, index, value.getDataLength());
                index += value.getDataLength();
            }
        }
        HanukCoinUtils.intIntoBytes(data, index, 0xDeadDead);
        data[index += 4] = (byte) blockChain.size();
        index++;
        for (Block block : blockChain) {
            System.arraycopy(block.getBytes(), 0, data, index, 36);
            index += 36;
        }
        return data;
    }

    /**
     * Builds a message according to the format, using the local nodes and blocks.
     *
     * @param isRequest whether the message os a request or not
     * @return message as a byte array
     */
    public static byte[] buildMessage(boolean isRequest) {
        return buildMessage(LocalNodeList.localList, isRequest, LocalBlockChain.blockChain);
    }


    /**
     * Creates new list of nodes out of sent message
     *
     * @param nodesArr An array of bytes representing a message
     * @return new list of nodes that were sent in the message
     * @throws WrongFormatException if the incoming data is of the wrong format
     */
    public static Map<HostPortPair, Node> messageIntoNodeList(byte[] nodesArr) throws WrongFormatException {
        Map<HostPortPair, Node> nodeList = new HashMap<>();
        int index = 0;
        for (int i = 0; i < nodesArr.length; i++) {
            byte nameLen = nodesArr[index];
            byte hostLen = nodesArr[index + nameLen + 1];
            Node node = new Node(Arrays.copyOfRange(nodesArr, index, index + nameLen + hostLen + 8));
            nodeList.put(new HostPortPair(node.getHost(), node.getPort()), node);
            index += nameLen + hostLen + 8;
        }
        return nodeList;
    }

    /**
     * Creates new list of blocks out of sent message
     *
     * @param data An array of bytes representing a message
     * @return new list of blocks that were sent in the message
     * @throws WrongFormatException if the incoming data is of the wrong format
     */
    public static ArrayList<Block> messageIntoBlocks(byte[] data) throws WrongFormatException {
        ArrayList<Block> blockChain = new ArrayList<>();
        int nodesCount = HanukCoinUtils.intFromBytes(data, 8);
        int index = 12;
        for (int i = 0; i < nodesCount; i++) {
            byte nameLen = data[index];
            byte hostLen = data[index + nameLen + 1];
            index += nameLen + hostLen + 8;
        }
        if (HanukCoinUtils.intFromBytes(data, index) != 0xDeadDead) {
            System.out.println("#Parsing error in messageIntoBlockList");
            throw new WrongFormatException();
        }
        index += 4;
        int blockCount = HanukCoinUtils.intFromBytes(data, index);
        for (int i = 0; i < blockCount; i++) {
            blockChain.add(new Block(Arrays.copyOfRange(data, index, index + 36)));
            index += 36;
        }
        if (HanukCoinUtils.intFromBytes(data, index) != 0xBeafBeaf) {
            System.out.println("#Parsing error in messageIntoBlockList");
            throw new WrongFormatException();
        }
        return blockChain;

    }

    /**
     * Parsing the timestamp into a date in ht format: yyyy-MM-dd HH:mm:ss
     *
     * @param ts The timestamp in seconds
     * @return A string representation of the date and time
     */
    private static String tsDisplay(int ts) {
        Date date = new Date((long) ts * 1000);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(date);
    }

    //TODO finish dark mode

    /**
     * Creating an HTML file that can be sent to get information about the node through a web browser
     *
     * @param nodeList   Node list for visual representation
     * @param blockChain Block chain for visual representation
     * @return The HTML of the returned page
     */
    private static String buildHtml(Map<HostPortPair, Node> nodeList,
                                    List<Block> blockChain) {
        StringBuilder htmlString = new StringBuilder(
                "<!--suppress SpellCheckingInspection --><html lang='en' style='font-family: Consolas,monospace'>\n" +
                        "   <head>\n" +
                        "        <link rel=\"shortcut icon\" href=\"http://www.iconj.com/ico/4/7/47qii4lk6j.ico\" type=\"image/x-icon\" />\n" +
                        "<style>\n" +
                        "           table {\n" +
                        "               font-family: Consolas;\n" +
                        "               border-collapse: collapse;\n" +
                        "               width: 100%;\n" +
                        "           }\n" +


                        "       td, th {\n" +
                        "           border: 1px solid #210101;\n" +
                        "           text-align: left;\n" +
                        "           padding: 8px;\n" +
                        "       }\n" +
                        "\n" +
                        "        tr:nth-child(even) {\n" +
                        "            background-color: #582b01;\n" +
                        "        }\n" +
                        "        tr:nth-child(odd) {\n" +
                        "            background-color: #8f4b04;\n" +
                        "        }\n" +
                        "        </style><title>" + Server.NAME + " Node #" + (Server.NUMBER) + "</title>\n" +
                        "</head>\n" +
                        "<body style='background-color:#582b01;color:rgba(243,215,192,0.73);'>\n" +
                        "<h2>Group List</h2><br/><table class='sortable'>\n" +
                        "<tr>\n" +
                        " <th>Name</th>\n" +
                        " <th>Block Count</th>" +
                        " <th>Hold percentage</th>" +
                        "</tr>");

        htmlString.append(Groups.blockCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .filter(entry -> !entry.getKey().equals("0x0"))
                .map(entry ->
                        String.format("<tr><td>%s</td><td>%s</td><td>%.2f%%</td></tr>",
                                entry.getKey(),
                                entry.getValue(),
                                100.0 * entry.getValue() / (LocalBlockChain.blockChain.size() - 1)))
                .collect(Collectors.joining("\n"))).append("</table>\n" +
                "<h2>Node List</h2><br/><table class='sortable'>\n" +
                "<tr>\n" +
                " <th>host</th>\n" +
                " <th>port</th>\n" +
                " <th>name</th>\n" +
                " <th>seen ts</th>\n" +
                " </tr>");


        nodeList.values().forEach(node ->
                htmlString.append(String.format("<tr><td>%s</td><td>%d</td><td>%s</td><td>%s </td></tr>",
                        node.getHost(),
                        (int) node.getPort(),
                        node.getName() + (node.getIsNew() ? "[New]" : ""),
                        tsDisplay(node.getLastSeenTimeStamp()))));

        htmlString.append("</table>\n" + "<h2>Last Blocks: ").append(HanukCoinUtils.BLOCKS_TO_SHOW).append("/").append(blockChain.size()).append("</h2><br/><table>\n").append("<tr>\n").append(" <th>blk#</th>\n").append(" <th>Wallet</th>\n").append(" <th>bin</th>\n").append(" </tr>");
        htmlString.append(
                blockChain.stream()
                        .filter(block -> block.getSerialNumber() > blockChain.size() - HanukCoinUtils.BLOCKS_TO_SHOW - 1)
                        .map(block -> String.format(
                                "<tr><td>%d</td><td>%s</td><td style= 'height: fit-content'>%s</td></tr>",
                                block.getSerialNumber(),
                                block.getName(),
                                block.binDump()))
                        .collect(Collectors.joining("\n")));

        htmlString.append("</table >\n" +
                "</body>\n" +
                "</html>");


        return htmlString.toString();
    }

    public static String buildHtml() {
        return buildHtml(LocalNodeList.localList, LocalBlockChain.blockChain);
    }

    public static String buildHtml(byte[] messageArr) {
        return buildHtml(Message.messageIntoNodeList(messageArr), Message.messageIntoBlocks(messageArr));
    }

    public static void sendNode(Node node, DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeByte(node.getNameLen());
        for (byte b : node.getName().getBytes()) {
            dataOutputStream.writeByte(b);
        }
        dataOutputStream.writeByte(node.getHostLen());
        for (byte b : node.getHost().getBytes()) {
            dataOutputStream.writeByte(b);
        }
        dataOutputStream.writeShort(node.getPort());
        dataOutputStream.writeInt(node.getLastSeenTimeStamp());
    }

    public static void sendBlock(Block block, DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeInt(block.getSerialNumber());
        dataOutputStream.writeInt(block.getWalletNumber());
        dataOutputStream.write(block.getPrevSig());
        dataOutputStream.writeLong(block.getPuzzle());
        dataOutputStream.write(block.getSig());
    }

    static boolean parseMessageAndUpdateLists(Socket connectionSocket, DataInputStream dataInput) throws IOException, IsHTTPException, TimeoutException {
        long time = HanukCoinUtils.getUnixTimestamp();
        int next;
        while ((next = dataInput.readInt()) == -1) {
            if (HanukCoinUtils.getUnixTimestamp() - time > HanukCoinUtils.UPDATE_TIME / 1000) {
                System.out.println("getMessageFromBuffer timeout");
                throw new TimeoutException();
            }
        }

        //System.out.println("Starting to read the " + (next == 1 ? "request from:  " : "response from: ") + this.connectionSocket.getInetAddress() + ":" + this.connectionSocket.getPort());
        if (next > 2)
            throw new IsHTTPException();
        next = dataInput.readInt();
        if (next != 0xBEEF_BEEF)
            throw new WrongFormatException();

        Map<HostPortPair, Node> nodeList = parseIntoNodeList(dataInput);


        if (dataInput.readInt() != 0xDeadDead) {
            System.out.println("#Parsing error in messageIntoNodeList");
            throw new WrongFormatException();
        }


        //System.out.println("Trying to read from: " + connectionSocket.getInetAddress().getHostName() + ":" + connectionSocket.getPort());
        ArrayList<Block> receivedBlocks = parseIntoBlockList(dataInput);

        return LocalBlockChain.updateLocalBlockChain(receivedBlocks) | LocalNodeList.update(nodeList);
    }

    public static Map<HostPortPair, Node> parseIntoNodeList(DataInputStream dataInput) throws IOException {
        int nodesLen = dataInput.readInt();
        Map<HostPortPair, Node> nodeList = new HashMap<>();

        for (int i = 0; i < nodesLen; i++) {
            Node node = Node.readFrom(dataInput);
            node.updateTimeSignature(dataInput.readInt());
            nodeList.put(new HostPortPair(node.getHost(), node.getPort()), node);
        }

        return nodeList;
    }

    public static ArrayList<Block>
    parseIntoBlockList(DataInputStream dataInput) throws IOException {
        int blockCount = dataInput.readInt();
        ArrayList<Block> receivedBlocks = new ArrayList<>();
        for (int bi = 0; bi < blockCount; bi++) {
            Block newBlock = Block.readFrom(dataInput);
            receivedBlocks.add(newBlock);
        }

        return receivedBlocks;
    }

    public static class WrongFormatException extends IllegalArgumentException {
    }

    static class IsHTTPException extends Exception {
    }
}
