package main.java.il.ac.tau.cs.hanukcoin.block;

import main.java.il.ac.tau.cs.hanukcoin.HanukCoinUtils;
import main.java.il.ac.tau.cs.server.Server;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LocalBlockChain {
    /**
     * <a href="https://en.meming.world/images/en/thumb/a/a9/Communist_Bugs_Bunny.jpg/300px-Communist_Bugs_Bunny.jpg"> Our Blockchain </a>
     */
    public static List<Block> blockChain = new ArrayList<>();

    /**
     * Checks whether a give BlockChain is valid,
     * according to the rules specified <a href="http://bit.ly/HanukCoin">here.</a>
     *
     * @param otherBlockChain the BlockChain to check
     * @return whether the BlockChain is valid.
     */
    private static boolean isValidBLockChainEfficient(List<Block> otherBlockChain) {
        if (Server.whitelist.containsValue(otherBlockChain.get(otherBlockChain.size() - 1).getWalletNumber())) {
            int i = 0;
            while (i < blockChain.size() && blockChain.get(i).equals(otherBlockChain.get(i)))
                i++;
            if (i == 0) {
                return isValidBLockChain(otherBlockChain);
            }
            return isValidBLockChainFrom(otherBlockChain, i);
        }
        return false;
    }


    public static boolean isValidBLockChain(List<Block> blockChain) {
        return blockChain.size() > 0 && blockChain.get(0).isGenBlock() && isValidBLockChainFrom(blockChain, 1);
    }

    private static boolean isValidBLockChainFrom(List<Block> blockChain, int ind) {
        if (blockChain.size() == 1 && blockChain.get(0).equals(HanukCoinUtils.createBlock0forTestStage()))
            return true;
        for (int i = ind; i < blockChain.size(); i++) {
            if (blockChain.get(i).getSerialNumber() != blockChain.get(i - 1).getSerialNumber() + 1)
                return false;
            if (blockChain.get(i).getWalletNumber() == blockChain.get(i - 1).getWalletNumber())
                return false;
            if (blockChain.get(i).checkSignature() != Block.BlockError.OK)
                return false;
            if (blockChain.get(i).checkValidNext(blockChain.get(i - 1)) != Block.BlockError.OK)
                return false;
        }
        return true;
    }

    /**
     * Saves the local BlockChain to memory.
     */
    public static void save() { //TODO add overload
        try (BufferedOutputStream outFile = new BufferedOutputStream(
                new FileOutputStream(new File("src/resources/blockChain")))) { //TODO allow the user choose the blockchain path through the arguments parsing
            for (Block block : blockChain) {
                outFile.write(block.getBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads the BlockChain stored in memory to {@link LocalBlockChain#blockChain}
     */
    public static void load(String path) {
        blockChain = new ArrayList<>();

        try (BufferedInputStream inFile = new BufferedInputStream(new FileInputStream(new File(path)))) {
            byte[] bytes = new byte[36];
            while (inFile.read(bytes) != -1) {
                blockChain.add(new Block(bytes));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!isValidBLockChain(blockChain)) {
            blockChain = Collections.singletonList(HanukCoinUtils.createBlock0forTestStage());
        }
    }

    public static void load() {
        load("src/resources/blockChain"); //TODO allow the user choose the blockchain path through the arguments parsing
    }

    /**
     * Updates the local BlockChain with a new chain,
     * according to the rules specified <a href="http://bit.ly/HanukCoin">here.</a>
     *
     * @param otherBlockChain The new BlockChain
     * @return whether the BlockChain was updated.
     */
    public static boolean updateLocalBlockChain(List<Block> otherBlockChain) {
        if (otherBlockChain.size() < blockChain.size() || !isValidBLockChainEfficient(otherBlockChain))
            return false;
        if (blockChain.size() == otherBlockChain.size()) {
            if (blockChain.get(blockChain.size() - 1).getPuzzle()
                    > otherBlockChain.get(otherBlockChain.size() - 1).getPuzzle()) {
                blockChain = otherBlockChain;
                return true;
            }
            return false;
        }
        blockChain = otherBlockChain;
        HanukCoinUtils.nZeros = HanukCoinUtils.numberOfZerosForPuzzle(LocalBlockChain.lastSN() + 1);
        return true;

    }

    public static Block getLastBlock() {
        return blockChain.get(blockChain.size() - 1);
    }

    public static int lastSN() {
        return blockChain.size() - 1;
    }

    public static synchronized void addBlock(Block b) {
        LocalBlockChain.blockChain.add(b);
    }
}
