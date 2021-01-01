

import main.java.il.ac.tau.cs.hanukcoin.HanukCoinUtils;
import main.java.il.ac.tau.cs.hanukcoin.block.LocalBlockChain;
import main.java.il.ac.tau.cs.hanukcoin.node.LocalNodeList;
import main.java.il.ac.tau.cs.server.Server;

import java.util.ArrayList;
import java.util.Collections;

public class ServerTest {
    public static void main(String[] args) {
        HanukCoinUtils.loadFromMemory();
        System.out.println(LocalBlockChain.blockChain.size());


        LocalNodeList.init(Server.NAME, Server.HOST, Server.PORT);
        LocalNodeList.init(Server.NAME, Server.HOST, (char) 42070);
        //LocalNodeList.init("Copper", "176.229.208.21", (char) 13375 );
        LocalBlockChain.updateLocalBlockChain(new ArrayList<>(Collections.singletonList(HanukCoinUtils.createBlock0forTestStage())));


        //LocalNodeList.init("Earth", "35.246.17.73", (char) 8080);
        HanukCoinUtils.saveToMemory();

        //Runtime.getRuntime().addShutdownHook(new Thread(Server::exitProgram, "Exit Thread"));

        //ServerActive.runServerWrite(LocalNodeList.localList.get(new HostPortPair("35.246.17.73", (char) 8080)), false);
        //Miner.startMining();
        //ServerPassive.runServerRead();
        //ServerActive.runServerWrite(LocalNodeList.localList.get(new HostPortPair<>("35.246.17.73", (char) 8082)), false);
        //LocalBlockList.blockChain.add(HanukCoinUtils.createBlock0forTestStage());
        //LocalBlockList.saveToMemory();
        //ServerActive.fiveMinutesUpdate();
    }
}
