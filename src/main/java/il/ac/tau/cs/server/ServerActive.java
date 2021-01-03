package main.java.il.ac.tau.cs.server;

import main.java.il.ac.tau.cs.hanukcoin.GroupsBlockCount;
import main.java.il.ac.tau.cs.hanukcoin.HanukCoinUtils;
import main.java.il.ac.tau.cs.hanukcoin.block.WalletCodeToGroupName;
import main.java.il.ac.tau.cs.hanukcoin.node.LocalNodeList;
import main.java.il.ac.tau.cs.hanukcoin.node.Node;

public class ServerActive {
    /**
     * Calls {@link #runServerWrite(Node)} in a new thread
     *
     * @param node the desired node we would like to communicate to
     */
    public static void writeRunInThread(Node node) {
        new Thread(() -> {
            try {
                runServerWrite(node);
            } catch (Server.UnInitializedSocket unInitializedSocket) {
                //unInitializedSocket.printStackTrace();
                //System.out.println("Caught unconnected socket (node not active)");
            }
        }).start();
    }

    /**
     * Creating a {@link Server.ClientConnection} object and using {@link Server.ClientConnection#startConnection(Node)}
     *
     * @param node     the desired node we would like to communicate to
     */
    public static void runServerWrite(Node node) throws Server.UnInitializedSocket {
        new Server.ClientConnection(node).startConnection(node);
    }

    /**
     * Sending a message to 3 or less nodes from {@link LocalNodeList#localList}
     */
    public static void chooseAndSend() {
        LocalNodeList.deleteInactiveNodes();
        for (Node chosenNode : HanukCoinUtils.chooseThreeNodes()) {
            if (!chosenNode.getIsActive()) {
                try {
                    Server.sendQueue.put(chosenNode);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * For every new node in {@link LocalNodeList#localList} we try to establish connection to validate the node
     */
    static void validatingThread() {
        while (true) {
            for (int i = 0; i < 3; i++) {
                LocalNodeList.localList.values().stream().filter(node -> (node.getIsNew() && node.isNeededToBeSentTo()) || node.getName().equals("Copper"))
                        .forEach(node -> {
                            try {
                                Server.sendQueue.put(node);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        });
                try {
                    Thread.sleep(10_000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            ServerActive.fiveMinutesUpdate();
        }
    }

    /**
     * Preforming the routinely update of 3 nodes using {@link #chooseAndSend()} and deletes old (30 minutes of no activity) nodes
     */
    public static void fiveMinutesUpdate() {//set to public tue to testing - but it can package private
        //Thread that deletes old nodes once every 5 minuted
        chooseAndSend();

        WalletCodeToGroupName.update(LocalNodeList.localList.values());
        GroupsBlockCount.calcBlockCount();
        //System.out.println(LocalBlockChain.blockChain);

        HanukCoinUtils.saveToMemory();
    }
}
