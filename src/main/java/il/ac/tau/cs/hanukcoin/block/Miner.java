package main.java.il.ac.tau.cs.hanukcoin.block;

import main.java.il.ac.tau.cs.hanukcoin.ConsoleColors;
import main.java.il.ac.tau.cs.hanukcoin.HanukCoinUtils;
import main.java.il.ac.tau.cs.server.Server;
import main.java.il.ac.tau.cs.server.ServerActive;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import static main.java.il.ac.tau.cs.hanukcoin.block.LocalBlockChain.*;

/**
 * class that handles all the Hanukcoin mining related actions
 */
@SuppressWarnings("ALL")
public class Miner {

    private static boolean shouldMine = true;


    /**
     * Start the mining on {@link main.java.il.ac.tau.cs.hanukcoin.HanukCoinUtils#MAX_MINING_THREADS} threads
     */
    public static void startMining() {
        Thread[] threads = new Thread[HanukCoinUtils.MAX_MINING_THREADS];
        for (int i = 0; i < HanukCoinUtils.MAX_MINING_THREADS; i++) {
            final int j = i;
            threads[i] = new Thread(() -> Miner.mine(j), String.format("%d-th mining thread", j));
            threads[i].start();
        }
        while (true) {
            if (!shouldMine) {
                ServerActive.chooseAndSend();
                while (!shouldMine) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    /**
     * Mine as long as the last block isn't ours
     *
     * @param threadNum the number of this miner's thread.
     */
    @SuppressWarnings("UnusedAssignment")
    public static void mine(int threadNum) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        byte[] attemptBlock = new byte[24];
        reInitializeBlock(attemptBlock, threadNum);
        int SN = lastSN() + 1;
        if (blockChain.get(blockChain.size() - 1).getWalletNumber() == Server.WALLET_NUM) {
            shouldMine = false;
        }

        Random random = new Random();
        //long puzzleGuess = Long.MIN_VALUE + threadNum;
        long puzzleGuess = random.nextLong();
        int count = 0;
        while (true) {
            count++;
            if (shouldMine) {
                if (SN <= lastSN()) { //if there is a new block
                    SN = lastSN() + 1;
                    reInitializeBlock(attemptBlock, threadNum);
                    puzzleGuess = Long.MIN_VALUE + threadNum;
                }

                //System.out.println("Attempting mining");

                assert md != null;
                Block b = Block.blockIfSigCorrect(attemptBlock, md); //null if sig bad
                if (b != null) {
                    shouldMine = false; //Our block is done, stop mining while we wait for a new block to come.
                    blockChain.add(b);
                    ServerActive.chooseAndSend();
                    System.out.printf("Successfully mined block with new SN:" + ConsoleColors.RED_BACKGROUND_BRIGHT + ConsoleColors.BLUE_BOLD + "%d"
                                    + ConsoleColors.RESET + "%nFrom thread " + ConsoleColors.BLUE + ConsoleColors.BLACK_BACKGROUND_BRIGHT + "%s"
                                    + ConsoleColors.RESET + "%n",
                            lastSN(),
                            Thread.currentThread().getName());

                }
                //puzzleGuess += HanukCoinUtils.MAX_MINING_THREADS;
                puzzleGuess = random.nextLong();
                HanukCoinUtils.longIntoBytes(attemptBlock, 16, puzzleGuess); //previous guess didn't work, try next

            } else {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (blockChain.get(blockChain.size() - 1).getWalletNumber() != Server.WALLET_NUM) { //Waiting for a block that isn't ours
                    shouldMine = true;
                }
            }
            if (count > 1000) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                count = 0;
            }

        }

    }

    private static void reInitializeBlock(byte[] attemptBlock, int threadNum) {
        HanukCoinUtils.intIntoBytes(attemptBlock, 0, lastSN() + 1);
        HanukCoinUtils.intIntoBytes(attemptBlock, 4, Server.WALLET_NUM);
        System.arraycopy(getLastBlock().data, 24, attemptBlock, 8, 8); //maybe sould be 28
        HanukCoinUtils.longIntoBytes(attemptBlock, 16, Long.MIN_VALUE + threadNum);

    }

    public static boolean getShouldMine() {
        return shouldMine;
    }
}