
import main.java.il.ac.tau.cs.hanukcoin.HostPortPair;
import main.java.il.ac.tau.cs.hanukcoin.node.LocalNodeList;
import main.java.il.ac.tau.cs.hanukcoin.node.Node;

import java.util.HashMap;
import java.util.Map;


public class LocalNodeListTest {
    public static void main(String[] args) {
        Map<HostPortPair, Node> nodeList = new HashMap<>();
        byte[] name = {(byte) 'C', (byte) 'o', (byte) 'p', (byte) 'p', (byte) 'e', (byte) 'r'};
        byte[] host = "copper-coin.3utilities.com".getBytes();
        char port = 2000;
        Node node = new Node(name, host, port);
        for (int i = 0; i < 5; i++) {
            nodeList.put(new HostPortPair("a", (char) i), node);
        }
        System.out.println(LocalNodeList.nodeListLengthSum(nodeList) == node.getDataLength() * 5);

    }
}
