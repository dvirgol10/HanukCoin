package main.java.il.ac.tau.cs.hanukcoin;

import main.java.il.ac.tau.cs.hanukcoin.block.LocalBlockChain;

import java.util.HashMap;
import java.util.Map;

/**
 * class that contains each group's block count
 */
public class GroupsBlockCount {
    public static final Map<String, Integer> blockCount = new HashMap<>();

    public static void calcBlockCount() {
        blockCount.clear();
        LocalBlockChain.blockChain.forEach(block ->
                blockCount.compute(block.getName(),
                        (str, count) -> (count == null) ? 1 : count + 1)); // increments or sets up the appropriate group for each block

    }
}
