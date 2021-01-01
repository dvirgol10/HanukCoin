
import main.java.il.ac.tau.cs.hanukcoin.HanukCoinUtils;
import main.java.il.ac.tau.cs.hanukcoin.node.Node;

import java.util.Arrays;

public class NodeTests {
    public static void main(String[] args) {
        nodeTest();
    }


    public static void nodeTest() {
        byte[] name = {(byte) 'C', (byte) 'o', (byte) 'p', (byte) 'p', (byte) 'e', (byte) 'r'};
        byte[] host = "http://copper-coin.3utilities.com/".getBytes();
        char port = 2000;
        Node node = new Node(name, host, port);
        System.out.println(HanukCoinUtils.getUnixTimestamp());
        System.out.println(Arrays.toString(node.getData()));

        System.out.println(node.getName());
        System.out.println(node.getHost());
        System.out.println((int) node.getPort());
    }
}
