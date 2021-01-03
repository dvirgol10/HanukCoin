package main.java.il.ac.tau.cs.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class ServerPassive {

    /**
     * Handles incoming transmission by binding the node and calling {@link Server.ClientConnection#runCommunicationHandlerInThread()}
     */
    public static void runServerRead() {
        ServerSocketChannel acceptSocket;
        try {
            acceptSocket = ServerSocketChannel.open();
            acceptSocket.socket().bind(new InetSocketAddress(Server.acceptPort));
        } catch (IOException e) {
            System.out.println(String.format("ERROR accepting at port %d", Server.acceptPort));
            return;
        }

        while (true) {
            SocketChannel connectionSocket;
            try {
                connectionSocket = acceptSocket.accept(); // this blocks
                if (connectionSocket != null && !Server.bannedDomains.contains(connectionSocket.socket().getInetAddress().getHostName().toLowerCase()))
                    new Server.ClientConnection(connectionSocket.socket()).runCommunicationHandlerInThread();
                Thread.sleep(1000);
            } catch (IOException e) {
                System.out.println(String.format("ERROR accept:\n  %s", e.toString()));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
