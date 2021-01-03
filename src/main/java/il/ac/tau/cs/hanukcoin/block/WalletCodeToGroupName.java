package main.java.il.ac.tau.cs.hanukcoin.block;

import main.java.il.ac.tau.cs.hanukcoin.HanukCoinUtils;
import main.java.il.ac.tau.cs.hanukcoin.node.Node;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class WalletCodeToGroupName {
    public static final Map<Integer, String> walletCodeToGroupNameMap = new HashMap<>();

    /**
     * update the hashmap with all the nodes from our list
     *
     * @param nodeList our local node list
     */
    public static void update(Collection<Node> nodeList) {
        for (Node node : nodeList) {
            walletCodeToGroupNameMap.put(HanukCoinUtils.groupNameToWalletCode(node.getName()), node.getName());
        }
    }
}
