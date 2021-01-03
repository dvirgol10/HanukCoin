package main.java.il.ac.tau.cs.server;


import main.java.il.ac.tau.cs.hanukcoin.GroupsBlockCount;
import main.java.il.ac.tau.cs.hanukcoin.HanukCoinUtils;
import main.java.il.ac.tau.cs.hanukcoin.HostPortPair;
import main.java.il.ac.tau.cs.hanukcoin.block.LocalBlockChain;
import main.java.il.ac.tau.cs.hanukcoin.block.Miner;
import main.java.il.ac.tau.cs.hanukcoin.block.WalletCodeToGroupName;
import main.java.il.ac.tau.cs.hanukcoin.node.LocalNodeList;
import main.java.il.ac.tau.cs.hanukcoin.node.Node;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;


//TODO add thread to handle accessing the javadoc
public class Server {
    /**
     * Holds the nodes which we would like to send a request to
     */
    public static final SynchronousQueue<Node> sendQueue = new SynchronousQueue<>();
    /**
     * A set of all the domains we would like to filter
     */
    public static final Set<String> bannedDomains = new HashSet<>(); //TODO can add multiple banned domains
    public static final int NUMBER = 0; //TODO update according to the network status
    /**
     * The group name
     */
    public static String NAME = "Copper";
    /**
     * The groups' wallet number
     */
    public static int WALLET_NUM = HanukCoinUtils.groupNameToWalletCode(NAME);
    /**
     * Host to access the specific node
     */
    public static String HOST = "copper-coin.3utilities.com";
    /**
     * Port to access the specific node
     */
    public static char PORT = 54321;
    /**
     * A whitelist of all the valid wallets
     */
    public static Map<String, Integer> whitelist = HanukCoinUtils.loadWhitelistFromMemory();
    /**
     * The inner port to access all of the incoming transmission
     */
    protected static int acceptPort = 8080;

    /**
     * Loading the node list and blockchain from <a href="file:../../resources/node_list">HanukCoinCopper\src\resources\node_list</a>, <a href="file:../../resources/blockChain">HanukCoinCopper\src\resources\blockChain</a>
     * Allowing the change {@link #NAME}, {@link #HOST}, {@link #PORT}, {@link #acceptPort}, {@link HanukCoinUtils#MAX_MINING_THREADS}, {@link HanukCoinUtils#BLOCKS_TO_SHOW}
     * Adding ourselves to the node list which is in our possession (you know, just in case...)
     * Updating {@link WalletCodeToGroupName#walletCodeToGroupNameMap}, {@link GroupsBlockCount#blockCount}
     */
    public static void main(String[] args) {
        if (args.length > 0) {
            parseArguments(args);
        }
        HanukCoinUtils.loadFromMemory();
        LocalNodeList.init(Server.NAME, Server.HOST, Server.PORT);
        WalletCodeToGroupName.update(LocalNodeList.localList.values());
        GroupsBlockCount.calcBlockCount();

        Runtime.getRuntime().addShutdownHook(new Thread(Server::exitProgram, "Exit Thread"));

        new Thread(ServerPassive::runServerRead, "Handler incoming").start();
        new Thread(ServerActive::validatingThread, "New nodes validator/five minute updater").start();
        new Thread(Server::sendHandler, "Handling queue").start();
        new Thread(Miner::startMining, "Mining Manager").start();
    }

    private static void parseArguments(String[] args) {
        NAME = args[0]; //TODO create a data structure for the server info
        WALLET_NUM = HanukCoinUtils.groupNameToWalletCode(NAME);
        HOST = args[1];
        PORT = (char) Integer.parseInt(args[2]);
        acceptPort = Integer.parseInt(args[3]);
        if (args.length > 4)
            HanukCoinUtils.MAX_MINING_THREADS = Integer.parseInt(args[4]);
        if (args.length > 5)
            HanukCoinUtils.BLOCKS_TO_SHOW = Integer.parseInt(args[5]);
        if (args.length > 6)
            HanukCoinUtils.SHOW_INFO_PAGE = Integer.parseInt(args[6]) > 0;
    }

    /**
     * Maneges the sending messages for any node in the {@link #sendQueue}
     */
    public static void sendHandler() {
        while (true) {
            try {
                Node node = sendQueue.take();
                ServerActive.writeRunInThread(node);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Program closing routine
     */
    public static void exitProgram() {
        HanukCoinUtils.saveToMemory();
        //we can add more stuff to do at the end of the code
        System.out.println("Finished gracefully");
    }

    /**
     * Class that wraps a socket ands its IO
     */
    public static class ClientConnection {
        private final Socket connectionSocket;
        private DataInputStream dataInput;
        private DataOutputStream dataOutput;

        //Constructors

        /**
         * The empty constructor
         */
        public ClientConnection() {
            this.connectionSocket = new Socket();
        }

        /**
         * Creates the object given the socket to wrap
         *
         * @param connectionSocket The socket to wrap
         */
        public ClientConnection(Socket connectionSocket) {
            this.connectionSocket = connectionSocket;
            try {
                dataInput = new DataInputStream(connectionSocket.getInputStream());
                dataOutput = new DataOutputStream(connectionSocket.getOutputStream());
            } catch (IOException e) {
                // handleIncomingConnection will fail and kill thread
            }
        }

        /**
         * Creates the socket to wrap given a host and port
         *
         * @param host The host of the desired address
         * @param port The port of the desired address
         */
        public ClientConnection(String host, char port) {
            this(setUpSocket(host, port));
        }

        /**
         * Creates the characteristic ClientConnection object of the node
         *
         * @param node The node to wrap its connection values
         */
        public ClientConnection(Node node) {
            this(node.getHost(), node.getPort());
        }


        private static Socket setUpSocket(String host, char port) {
            try {
                return new Socket(host, port);
            } catch (IOException e) {
                //System.out.println(String.format("ERROR connecting to port %d", (int) port));
                return new Socket();
            }
        }

        /**
         * Implementing the reading of incoming communication in a new thread
         */
        public void runCommunicationHandlerInThread() {
            new Thread(ClientConnection.this::handleIncomingConnection).start();
        }

        /**
         * Parse the incoming transmission using {@link Message#parseMessageAndUpdateLists(Socket, DataInputStream)}
         * Updates the timestamp of ourselves to send most recent update to the sender
         * Send back a response to the incoming request using  {@link #sendResponse()}
         * <p>
         * If the incoming message is an HTTP request for the web page we call {@link #sendHtml(String)} with {@link Message#buildHtml()}
         */
        private void handleIncomingConnection() {
            //This function runs in a separate thread to handle the connection
            try {
                acceptIncomingAndTryToRespond();
            } catch (Message.IsHTTPException e) {
                if (HanukCoinUtils.SHOW_INFO_PAGE) {
                    sendInfoPage();
                }
            } catch (IOException | TimeoutException e) {
                e.printStackTrace();
            } finally {
                try {
                    connectionSocket.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        private void sendInfoPage() {
            try {
                System.out.println("Sending HTML");
                sendHtml(Message.buildHtml());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        private void acceptIncomingAndTryToRespond() throws Message.IsHTTPException, IOException, TimeoutException {
            //System.out.printf("New client!: %s:%d%n", connectionSocket.getInetAddress(), connectionSocket.getPort());
            Message.parseMessageAndUpdateLists(this.connectionSocket, this.dataInput);
            updateSelfTimeSignature();
            //System.out.println("Sending message as response");
            sendResponse();
        }

        /**
         * Sending raw binary data
         *
         * @param data the binary data to send as a byte array
         * @throws IOException if the sending have a problem
         */
        protected void sendBin(byte[] data) throws IOException {
            dataOutput.write(data);
            dataOutput.flush();
        }

        /**
         * Wraps {@link #sendMessage(int)} and sends with the cmd = 1 (request)
         */
        private void sendRequest() {
            try {
                sendMessage(1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Implementing the outgoing communication using {@link #sendRequest()}
         * <p>
         * if the returned message has new information we will update 3 random nodes from our {@link LocalNodeList#localList} using  {@link ServerActive#chooseAndSend()}
         * <p>
         * if the node is new and we didn't manage to get a response we are incrementing the tries counter
         */

        public void startConnection(Node node) throws UnInitializedSocket {
            if (this.connectionSocket.isConnected()) {
                funcIfSocketIsConnected(node);
            } else {
                throw new UnInitializedSocket();
            }
        }

        private void funcIfSocketIsConnected(Node node) {
            try {
                updateNodeInfo(node);
                sendRequest();
                //this.sendBin(Message.buildMessage(true));

                boolean isChanged = Message.parseMessageAndUpdateLists(this.connectionSocket, this.dataInput);
                LocalNodeList.deleteInactiveNodes();
                node.setIsNew(false); // TODO understand
                if (isChanged) {
                    System.out.println("The node list and blockchain have changed");
                    ServerActive.chooseAndSend();
                }
                node.setIsActive(false); // TODO understand
                this.connectionSocket.close();
                Thread.currentThread().interrupt();
            } catch (TimeoutException e) {
                if (node.getIsNew()) {
                    node.incCounterOfTries();
                }
            } catch (IOException | Message.IsHTTPException e) {
                e.printStackTrace();
            }
        }

        private void updateNodeInfo(Node node) {
            //System.out.println("Sending message as request");
            updateSelfTimeSignature();
            node.setIsActive(true);
        }

        /**
         * Wraps {@link #sendMessage(int)} and sends with the cmd = 0 (response)
         */
        private void sendResponse() {
            try {
                sendMessage(2); // TODO change to global constant
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Incrementally sends the message according to the specified binary format
         *
         * @param cmd indicates if the message is a response or a request (1 for request, 2 for response)
         * @throws IOException if something went wrong
         */
        private void sendMessage(int cmd) throws IOException {
            this.dataOutput.writeInt(cmd);
            this.dataOutput.writeInt(0xBEEF_BEEF); // TODO change to global constant
            writeNodeListInfoToDataOutputStream();

            this.dataOutput.writeInt(0xDEAD_DEAD); // TODO change to global constant
            writeBlockChainInfoToDataOutputStream();
        }

        private void writeNodeListInfoToDataOutputStream() throws IOException {
            this.dataOutput.writeInt(LocalNodeList.countOldValidatedNodes());
            writeOldValidatedNodesToDataOutputStream();
        }

        private void writeOldValidatedNodesToDataOutputStream() {
            LocalNodeList.getOldValidatedNodeStream()
                    .forEach(node -> {
                        try {
                            dataOutput.write(node.getData());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }

        private void writeBlockChainInfoToDataOutputStream() throws IOException {
            this.dataOutput.writeInt(LocalBlockChain.blockChain.size());
            writeBlocksToDataOutputStream();
        }

        private void writeBlocksToDataOutputStream() {
            LocalBlockChain.blockChain.forEach(block -> {
                try {
                    dataOutput.write(block.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        /**
         * handle the wrapping of an HTML massage
         *
         * @param body The content - actual html page
         * @throws IOException if something in the sending fails
         */
        protected void sendHtml(String body) throws IOException { //TODO organize
            int contentLen = body.length();
            HashMap<String, String> header = new HashMap<>();
            header.put("Content-Length", Integer.toString(contentLen));
            header.put("Content-Type", "text/html");
            header.put("Connection", "closed");
            String responseLine = "HTTP/1.1 200 OK\r\n";
            String headerText = header.entrySet().stream()
                    .map(e -> e.getKey() + ": " + e.getValue())
                    .collect(Collectors.joining("\r\n"));
            String headerFull = responseLine + headerText + "\r\n\r\n";
            //System.out.println(headerFull);
            dataOutput.writeBytes(headerFull);
            dataOutput.writeBytes(body);
            dataOutput.flush();
        }
    }

    private static void updateSelfTimeSignature() {
        HostPortPair selfHostPortPair = new HostPortPair(HOST, PORT);
        Node selfNode = LocalNodeList.localList.get(selfHostPortPair);
        selfNode.updateTimeSignature();
    }

    public static class UnInitializedSocket extends Throwable {

    }
}