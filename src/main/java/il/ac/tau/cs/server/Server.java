package main.java.il.ac.tau.cs.server;


import main.java.il.ac.tau.cs.hanukcoin.Groups;
import main.java.il.ac.tau.cs.hanukcoin.HanukCoinUtils;
import main.java.il.ac.tau.cs.hanukcoin.HostPortPair;
import main.java.il.ac.tau.cs.hanukcoin.block.LocalBlockChain;
import main.java.il.ac.tau.cs.hanukcoin.block.Miner;
import main.java.il.ac.tau.cs.hanukcoin.block.WalletToName;
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
    public static final Set<String> bannedDomains = new HashSet<>();//TODO can add multiple banned domains
    public static final boolean toCheck = false; //TODO change for the start
    /**
     * The group name
     */
    public static String NAME = "Copper";
    /**
     * The groups's wallet number
     */
    public static int WALLET_NUM = HanukCoinUtils.walletCode(NAME);
    /**
     * Host to access the specific node
     */
    public static String HOST = "copper-coin.3utilities.com";
    /**
     * Port to access the specific node
     */
    public static char PORT = 42069;
    /**
     * A whitelist of all the valid wallets
     */
    public static Map<String, Integer> whitelist = HanukCoinUtils.loadWhitelistFromMemory();
    /**
     * The inner port to access all of the incoming transmission
     */
    protected static int acceptPort = 8080;

    /**
     * Loading the node list and blockchain from  <a href="file:../../resources/blockChain">HanukCoinCopper\src\resources\blockChain</a>, <a href="file:../../resources/node_list">HanukCoinCopper\src\resources\node_list</a>
     * Allowing the change {@link #NAME}, {@link #HOST}, {@link #PORT}, {@link #acceptPort}, {@link HanukCoinUtils#MAX_MINING_THREADS}, {@link HanukCoinUtils#BLOCKSTOSHOW}
     * Adding ourselves to the node list which is in our possession (you know, just in case...)
     * Updating {@link WalletToName#walletToNameMap}, {@link Groups#blockCount}
     */
    public static void main(String[] args) {
        HanukCoinUtils.loadFromMemory();
        if (args.length > 0) {
            // allow changing accept port
            NAME = args[0];
            WALLET_NUM = HanukCoinUtils.walletCode(NAME);
            HOST = args[1];
            PORT = (char) Integer.parseInt(args[2]);
            acceptPort = Integer.parseInt(args[3]);
            if (args.length > 4)
                HanukCoinUtils.MAX_MINING_THREADS = Integer.parseInt(args[4]);
            if (args.length > 5)
                HanukCoinUtils.BLOCKSTOSHOW = Integer.parseInt(args[5]);
            if (args.length > 6)
                HanukCoinUtils.HideHTML = Integer.parseInt(args[6]) > 0;
        }
        LocalNodeList.init(Server.NAME, Server.HOST, Server.PORT);
        WalletToName.update(LocalNodeList.localList.values());
        Groups.calcBlockCount();

        Runtime.getRuntime().addShutdownHook(new Thread(Server::exitProgram, "Exit Thread"));

        new Thread(ServerPassive::runServerRead, "Handler incoming").start();
        new Thread(ServerActive::validatingThread, "New nodes validator/five minute updater").start();
        new Thread(Server::sendHandler, "Handling queue").start();
        new Thread(Miner::startMining, "Mining Manager").start();
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
        System.out.println("Noso why you bully me?");
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
        public ClientConnection(Socket connectionSocket) { //the const is just fine
            this.connectionSocket = connectionSocket;
            try {
                dataInput = new DataInputStream(connectionSocket.getInputStream());
                dataOutput = new DataOutputStream(connectionSocket.getOutputStream());
            } catch (IOException e) {
                // connectionThread would fail and kill thread
            }
        }

        /**
         * Creates the socket to wrap given a host and port
         *
         * @param host The host of the desired address
         * @param port The host of the desired address
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

        public static void dos(Node node) {
            ClientConnection s = new Server.ClientConnection(node);
            s.sendRequest();
        }

        /**
         * Implementing the reading of incoming communication in a new thread
         */
        public void readRunInThread() {
            new Thread(ClientConnection.this::connectionThread).start();
        }

        /**
         * Parse the incoming transmission using {@link Message#parseMessage(Socket, DataInputStream)}
         * Updates the timestamp of ourselves to send most recent update to the sender
         * Send back a response to the incoming request using  {@link #sendResponse()}
         * <p>
         * If the incoming message is an HTTP request for the web page we call {@link #sendHtml(String)} with {@link Message#buildHtml()}
         */
        private void connectionThread() {
            //This function runs in a separate thread to handle the connection
            try {
                //System.out.printf("New client!: %s:%d%n", connectionSocket.getInetAddress(), connectionSocket.getPort());
                Message.parseMessage(this.connectionSocket, this.dataInput);
                LocalNodeList.localList.get(new HostPortPair(HOST, PORT)).updateTS();
                //System.out.println("Sending message as response");
                sendResponse();
            } catch (Message.IsHTTPException e) {
                if (!HanukCoinUtils.HideHTML) {
                    try {
                        System.out.println("Sending HTML");
                        sendHtml(Message.buildHtml());
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
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
                try {
                    //System.out.println("Sending message as request");
                    if (HanukCoinUtils.notDoS) {
                        LocalNodeList.localList.get(new HostPortPair(HOST, PORT)).updateTS();
                    }
                    node.setIsActive(true);
                    sendRequest();
                    //this.sendBin(Message.buildMessage(true));

                    boolean isChanged = Message.parseMessage(this.connectionSocket, this.dataInput);
                    LocalNodeList.deleteOldNodes();
                    node.setIsNew(false);
                    if (isChanged) {
                        System.out.println("The node list and blockchain have changed");
                        ServerActive.chooseAndSend();
                    }
                    node.setIsActive(false);
                    this.connectionSocket.close();
                    Thread.currentThread().interrupt();
                } catch (TimeoutException e) {
                    if (node.getIsNew()) {
                        node.incCounterOfTries();
                    }
                } catch (IOException | Message.IsHTTPException e) {
                    e.printStackTrace();
                }
            } else {
                throw new UnInitializedSocket();
            }
        }

        /**
         * Wraps {@link #sendMessage(int)} and sends with the cmd = 0 (response)
         */
        private void sendResponse() {
            try {
                sendMessage(2);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Incrementally sends the message according to the specified binary format
         *
         * @param cmd indicates if the message is a response or a request
         * @throws IOException if something went wrong
         */
        private void sendMessage(int cmd) throws IOException {
            this.dataOutput.writeInt(cmd);
            this.dataOutput.writeInt(0xBEEF_BEEF);
            this.dataOutput.writeInt(LocalNodeList.countTrustedNodes());
            LocalNodeList.localList.values()
                    .stream()
                    .filter(Node::getIsOld)
                    .forEach(node -> {
                        try {
                            dataOutput.write(node.getData());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

            this.dataOutput.writeInt(0xDEAD_DEAD);
            this.dataOutput.writeInt(LocalBlockChain.blockChain.size());
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
        protected void sendHtml(String body) throws IOException {
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

    public static class UnInitializedSocket extends Throwable {

    }
}