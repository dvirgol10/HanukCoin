import main.java.il.ac.tau.cs.hanukcoin.HanukCoinUtils;
import main.java.il.ac.tau.cs.hanukcoin.block.LocalBlockChain;
import main.java.il.ac.tau.cs.hanukcoin.block.Miner;
import main.java.il.ac.tau.cs.server.Server;

public class MinerTest {
    public static void main(String[] args) {
        HanukCoinUtils.loadFromMemory();
        new Thread(Miner::startMining).start();
        System.out.println("started mining");
        Runtime.getRuntime().addShutdownHook(new Thread(Server::exitProgram, "Exit Thread"));
//
//        while (true) {
//            printIsValid();
//            try {
//                TimeUnit.SECONDS.sleep(5);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }

        while (true) {
            if (LocalBlockChain.blockChain.size() > 1)
                LocalBlockChain.isValidBLockChain(LocalBlockChain.blockChain);
        }
    }

    private static void printIsValid() {
        System.out.println(LocalBlockChain.isValidBLockChain(LocalBlockChain.blockChain));
    }
}
