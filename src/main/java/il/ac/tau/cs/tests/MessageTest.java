

import main.java.il.ac.tau.cs.hanukcoin.HostPortPair;
import main.java.il.ac.tau.cs.hanukcoin.node.Node;
import main.java.il.ac.tau.cs.server.Message;
import main.java.il.ac.tau.cs.server.Server;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MessageTest {
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    public static void main(String[] args) {
        Map<HostPortPair, Node> nodeList = new HashMap<>();
        for (int i = 0; i < 5; i++) {
            byte[] name = {(byte) 'C', (byte) 'o', (byte) 'p', (byte) 'p', (byte) 'e', (byte) 'r'};
            byte[] host = "http://copper-coin.3utilities.com/".getBytes();
            char port = (char) (2000 + i);
            Node node = new Node(Server.NAME, Server.HOST, Server.PORT);
            node.setIsNew(false);
            nodeList.put(new HostPortPair("http://copper-coin.3utilities.com/", port), node);
        }
        byte[] message = Message.buildMessage(false);
        System.out.println(Arrays.toString(message));

        Map<HostPortPair, Node> newNodeList = Message.messageIntoNodeList(message);
        System.out.println(newNodeList);

    }
}
