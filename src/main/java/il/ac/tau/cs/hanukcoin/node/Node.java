package main.java.il.ac.tau.cs.hanukcoin.node;

/*
 * Node is at least 8 byte/64 bit long.
 * record/node format:
 * 8   bit name length
 * 8*x bit team name (x is name length)
 * 8   bit host length
 * 8*x bit host (x is host length) - FQDN/ip of the server
 * 16  bit port
 * 32  bit last seen timestamp
 */


import main.java.il.ac.tau.cs.hanukcoin.HanukCoinUtils;
import main.java.il.ac.tau.cs.hanukcoin.HostPortPair;
import main.java.il.ac.tau.cs.server.Server;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;


/**
 * Class that represents one node in the network.
 * The node holds a array of minimum length 8-bytes and all operations are performed directly on this array.
 */
public class Node {
    private final byte[] data;
    private final byte nameLen;
    private final byte hostLen;
    private boolean isNew = true;
    private int counterOfTries = 1;
    private int counterOfPassedIterations = 0;

    /**
     * Creates a new node from existing node that has a data array, but different timestamp
     *
     * @param data The existing node as a {@code byte[]}
     */
    public Node(byte[] data) {
        this.data = Arrays.copyOf(data, data.length);
        this.nameLen = this.data[0];
        this.hostLen = this.data[1 + this.nameLen];
    }

    /**
     * Creates a new node from a name, a host, and a port
     *
     * @param name name of the group represented by a byte array
     * @param host domain of the node represented by a byte array
     * @param port port of the node represented by a 2-byte short
     */
    public Node(byte[] name, byte[] host, char port) {
        this.nameLen = (byte) name.length;
        this.hostLen = (byte) host.length;
        this.data = new byte[1 + this.nameLen + 1 + this.hostLen + 2 + 4];
        this.data[0] = nameLen;
        System.arraycopy(name, 0, this.data, 1, this.nameLen);
        this.data[1 + this.nameLen] = this.hostLen;
        System.arraycopy(host, 0, this.data, 2 + this.nameLen, this.hostLen);
        HanukCoinUtils.charIntoBytes(data, this.data.length - 6, port);
        this.updateTimeSignature();
    }

    /**
     * Creates a new node from a name, a host, and a port
     *
     * @param name name of the group represented by a String
     * @param host domain of the node represented by a String
     * @param port port of the node represented by a 2-byte short
     */
    public Node(String name, String host, char port) {
        this(name.getBytes(), host.getBytes(), port);
    }

    public static Node readFrom(DataInputStream dataInput) throws IOException {
        byte nameLen = dataInput.readByte();
        byte[] name = new byte[nameLen];
        for (int j = 0; j < nameLen; j++) {
            name[j] = dataInput.readByte();
        }
        byte hostLen = dataInput.readByte();
        byte[] host = new byte[hostLen];
        for (int k = 0; k < hostLen; k++) {
            host[k] = dataInput.readByte();
        }
        char port = dataInput.readChar();
        return new Node(name, host, port);
    }

    public static boolean isDeletable(HostPortPair pair) {
        return !pair.equals(new HostPortPair(Server.HOST, Server.PORT)) &&
                LocalNodeList.localList.get(pair).getLastSeenTimeStamp() + 1800 < HanukCoinUtils.getUnixTimestamp();
    }

    public boolean getIsNew() {
        return isNew;
    }

    public void setIsNew(boolean aNew) {
        isNew = aNew;
    }

    public boolean getIsOld() {
        return !isNew;
    }

    public int getDataLength() {
        return this.data.length;
    }

    /**
     * @return domain of the node as a String
     */
    public String getHost() {
        return new String(Arrays.copyOfRange(this.data, 2 + this.nameLen, 2 + this.nameLen + this.hostLen));
    }

    /**
     * @return port of the node as a 2-byte short
     */
    public char getPort() {
        return HanukCoinUtils.charFromBytes(this.data, this.getDataLength() - 6);
    }

    /**
     * @return int representing timestamp
     */
    public int getLastSeenTimeStamp() {
        return HanukCoinUtils.intFromBytes(this.data, this.data.length - 4);
    }

    /**
     * @return name of group as a String
     */
    public String getName() {
        return new String(Arrays.copyOfRange(this.data, 1, 1 + this.nameLen));
    }

    /**
     * @return data stored in the node
     */
    public byte[] getData() {
        return this.data;
    }

    /**
     * @return number of times we tried connect to this node
     */
    public int getCounterOfTries() {
        return this.counterOfTries;
    }

    /**
     * increases the number of times we tried connect to this node
     */
    public void incCounterOfTries() {
        ++this.counterOfTries;
    }

    /**
     * @return whether we need to create a connection with this node
     */
    public boolean isNeededToBeSentTo() {
        return (counterOfPassedIterations++) % getCounterOfTries() == 0;
    }

    /**
     * update timestamp of the node if needed
     */
    public void updateTimeSignature() {
        int timestamp = HanukCoinUtils.getUnixTimestamp();
        if (timestamp > getLastSeenTimeStamp()) {
            HanukCoinUtils.intIntoBytes(this.data, this.getDataLength() - 4, timestamp);
        }
    }

    /**
     * changes the timestamp to a specified number
     *
     * @param newTS new timestamp
     */
    public void updateTimeSignature(int newTS) {
        HanukCoinUtils.intIntoBytes(this.data, this.getDataLength() - 4, newTS);
    }

    public boolean equals(Node other) {
        return this.getName().equals(other.getName())
                && this.getHost().equals(other.getHost())
                && (this.getPort() == other.getPort());
    }

    /**
     * Creates a Visually pleasant representation of a node
     *
     * @return A "human language" representation of the node
     */
    public String toString() {
        return "Name - " + this.getName() + " | Host:Port - " + this.getHost() + ":" + (int) this.getPort() + " | Is new - " + this.getIsNew();
    }

    public int getHostLen() {
        return this.hostLen;
    }

    public int getNameLen() {
        return nameLen;
    }
}

