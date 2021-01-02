package main.java.il.ac.tau.cs.hanukcoin.node;


import main.java.il.ac.tau.cs.hanukcoin.HanukCoinUtils;
import main.java.il.ac.tau.cs.hanukcoin.HostPortPair;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Class that stores a HashMap of all nodes in the network.
 */
public class LocalNodeList {
    /**
     * HashMap that stores all known nodes.
     */
    public static Map<HostPortPair, Node> localList = new HashMap<>();

    public static int countNewNodes() {
        return (int) localList.values()
                .stream()
                .filter(Node::getIsNew)
                .count();
    }

    public static int countTrustedNodes() {
        return (int) localList.values()
                .stream()
                .filter(Node::getIsOld)
                .count();
    }

    /**
     * Creates and stores initial node in the list
     *
     * @param name the name of the group
     * @param host the domain/ip address
     * @param port port
     */
    public static void init(String name, String host, Character port) {
        Node node = new Node(name, host, port);
        node.setIsNew(false);
        localList.put(new HostPortPair(host, port), node);
    }

    /**
     * Updates current list with a new list
     * according to the following rules specified <a href="http://bit.ly/HanukCoin">here.</a>
     *
     * @param otherList The new list
     * @return whether we added a node to the local list
     */
    public synchronized static boolean update(Map<HostPortPair, Node> otherList) {
        boolean flag = false;
        int time;
        for (HostPortPair pair : otherList.keySet()) {
            if (localList.containsKey(pair)) {
                time = Math.min(HanukCoinUtils.getUnixTimestamp(),
                        Math.max(otherList.get(pair).getLastSeenTimeStamp(),
                                localList.get(pair).getLastSeenTimeStamp()));
                localList.get(pair).updateTimeSignature(time);
            } else {
                localList.put(pair, otherList.get(pair));
                flag = true;
            }
        }
        return flag;
    }

    /**
     * Adds a single node to the list
     *
     * @param node The node we want to add.
     * @return whether we added the node to the local list
     */
    public synchronized static boolean update(Node node) {
        return null == localList.putIfAbsent(new HostPortPair(node.getHost(), node.getPort()), node);
    }

    /**
     * calculates the sum of the lengths of the nodes in a HashMap
     *
     * @param nodeList The list of nodes.
     * @return sum of the lengths
     */
    public static int nodeListLengthSum(Map<HostPortPair, Node> nodeList) {
        return nodeList.values()
                .stream()
                .mapToInt(Node::getDataLength)
                .sum();
    }

    /**
     * Saves {@link LocalNodeList#localList} to memory in the following format:
     * for each node, at first will be 4 bytes describing its size,
     * then 00000000 if old, 00000001 if new,
     * and then the bytes corresponding to {@link Node#getData()}
     */
    public static synchronized void save() {
        try (BufferedOutputStream outFile = new BufferedOutputStream(
                new FileOutputStream(
                        new File("src/resources/node_list")))) {

            for (Node node : localList.values()) {
                writeInt(outFile, node.getDataLength());
                outFile.write(node.getIsNew() ? 1 : 0);
                outFile.write(node.getData());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes 4 bytes to {@code out}, from {@code num}
     *
     * @param out An {@link BufferedOutputStream} to which we write the int
     * @param num An int representing the 4 bytes we want to write
     * @throws IOException If unable to write to {@code out}
     */
    private static void writeInt(BufferedOutputStream out, int num) throws IOException {
        out.write(num << 24);
        out.write(num << 16);
        out.write(num << 8);
        out.write(num);
    }

    /**
     * loads nodes to {@link LocalNodeList#localList} from the file.
     */
    public static void load() {
        localList = new HashMap<>();
        byte[] bytes;
        int nextSize;
        try (BufferedInputStream inFile = new BufferedInputStream(
                new FileInputStream(
                        new File("src/resources/node_list")))) {
            while ((nextSize = readInt(inFile)) != -1) {
                boolean isNew = inFile.read() == 1;
                bytes = new byte[nextSize];
                //noinspection ResultOfMethodCallIgnored
                inFile.read(bytes);
                Node node = new Node(bytes);
                node.setIsNew(isNew);
                localList.put(new HostPortPair(node.getHost(), node.getPort()), node);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * reads 4 bytes from {@code in} to an int. returns -1 if EOF reached.
     *
     * @param in an {@link BufferedInputStream} from which we read.
     * @return The first 4 bytes as an int, -1 if EOF reached.
     * @throws IOException If unable to read from {@code in}
     */
    private static int readInt(BufferedInputStream in) throws IOException {
        int res = 0;
        int next;
        for (int i = 0; i < 4; i++) {
            if ((next = in.read()) == -1) return -1;
            res |= next << ((3 - i) * 8);
        }
        return res;
    }

    /**
     * deletes nodes that were last seen more than 30 minutes ago
     */
    public static void deleteOldNodes() {
        localList.keySet().removeIf(Node::isDeletable);
    }
}
