package main.java.il.ac.tau.cs.hanukcoin;

import main.java.il.ac.tau.cs.hanukcoin.block.LocalBlockChain;

import java.util.HashMap;
import java.util.Map;

/**
 * class tha contains the
 */
public class Groups {
    public static final Map<String, Integer> blockCount = new HashMap<>();

    public static void calcBlockCount() {
        blockCount.clear();
        LocalBlockChain.blockChain.forEach(block ->
                blockCount.compute(block.getName(),
                        (str, count) -> (count == null) ? 1 : count + 1));
    }
}
