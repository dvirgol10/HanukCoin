package main.java.il.ac.tau.cs.hanukcoin;

import main.java.il.ac.tau.cs.hanukcoin.block.Block;
import main.java.il.ac.tau.cs.hanukcoin.block.LocalBlockChain;
import main.java.il.ac.tau.cs.hanukcoin.node.LocalNodeList;
import main.java.il.ac.tau.cs.hanukcoin.node.Node;
import main.java.il.ac.tau.cs.server.Server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class HanukCoinUtils {
    public static final int UPDATE_TIME = 300_000;//300_000;
    private static final int PUZZLE_BITS0 = 20;  // Note - we can make it lower for quick testing
    public static int BLOCKS_TO_SHOW = 100;
    public static int MAX_MINING_THREADS = 500;
    public static boolean SHOW_INFO_PAGE = true;
    public static int nZeros = HanukCoinUtils.numberOfZerosForPuzzle(LocalBlockChain.lastSN() + 1);


    /**
     * Calculate how many time n can be divided by 2 to get zero
     *
     * @param n it's an integer, isn't it?
     * @return base 2 log of n plus 1
     */
    public static int numBits(long n) {
        for (int i = 0; i < 32; i++) {
            long mask = (1L << i) - 1;
            if ((n & mask) == n) {
                return i;
            }
        }
        return 99; //error?
    }

    /**
     * Given a block serial number - how many zeros should be at the end of its signature
     *
     * @param blockSerialNumber the serial number of the block
     * @return number of required zero at end
     */
    public static int numberOfZerosForPuzzle(int blockSerialNumber) {
        return PUZZLE_BITS0 + numBits(blockSerialNumber);
    }

    /**
     * Read 4 bytes big endian integer from data[offset]
     *
     * @param data   - block of bytes
     * @param offset - where to start reading 4 bytes
     * @return 4 bytes integer
     */
    public static int intFromBytes(byte[] data, int offset) {
        //return data[offset] << 24 | data[offset + 1] << 16 | data[offset + 2] << 8 | data[offset + 3];
        int b1 = (data[offset] & 0xFF) << 24;
        int b2 = (data[offset + 1] & 0xFF) << 16;
        int b3 = (data[offset + 2] & 0xFF) << 8;
        int b4 = (data[offset + 3] & 0xFF);
        return b1 | b2 | b3 | b4;
    }


    /**
     * Read 2 bytes big endian short from data[offset]
     *
     * @param data   - block of bytes
     * @param offset - where to start reading 4 bytes
     * @return 2 bytes short
     */
    public static char charFromBytes(byte[] data, int offset) {
        char b1 = (char) ((data[offset] & 0xFF) << 8);
        char b2 = (char) ((data[offset + 1] & 0xFF));

        return (char) (b1 | b2);
    }

    public static long longFromBytes(byte[] data, int offset) {
        long res = 0;
        for (int i = 0; i < 8; i++) {
            res |= (data[offset + i] & 0xFF) << (56 - i * 8);
        }
        return res;
    }

    /**
     * put value in big-endian format into data[offset]
     *
     * @param data   - bytes array
     * @param offset - offset into data[] where to write value
     * @param value  - 32 bit value
     */
    public static void intIntoBytes(byte[] data, int offset, int value) {
        //return data[offset] << 24 | data[offset + 1] << 16 | data[offset + 2] << 8 | data[offset + 3];
        data[offset] = (byte) ((value >> 24) & 0xFF);
        data[offset + 1] = (byte) ((value >> 16) & 0xFF);
        data[offset + 2] = (byte) ((value >> 8) & 0xFF);
        data[offset + 3] = (byte) ((value) & 0xFF);
    }

    /**
     * put value in big-endian format into data[offset]
     *
     * @param data   - bytes array
     * @param offset - offset into data[] where to write value
     * @param value  - 16 bit value
     */
    public static void charIntoBytes(byte[] data, int offset, char value) {
        data[offset] = (byte) ((value >> 8) & 0xFF);
        data[offset + 1] = (byte) ((value) & 0xFF);
    }

    public static void longIntoBytes(byte[] data, int offset, long value) {
        intIntoBytes(data, offset, (int) (value >> 32));
        intIntoBytes(data, offset + 4, (int) (value));
    }

    /**
     * Given a user/team name - return 32bit wallet code
     *
     * @return 32bit number
     */
    public static int groupNameToWalletCode(String teamName) {
        if (!isGenesisGroupName(teamName)) {
            return generateWalletCodeForValidGroup(teamName);
        } else {
            return 0;
        }
    }

    private static boolean isGenesisGroupName(String teamName) {
        return teamName.equals("0");
    }

    private static int generateWalletCodeForValidGroup(String teamName) {
        MessageDigest md; // may cause NoSuchAlgorithmException
        try {
            md = MessageDigest.getInstance("md5");
            md.update(teamName.getBytes());
            byte[] messageDigest = md.digest();
            return intFromBytes(messageDigest, 0);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Internal error - missing MD5");
        }
    }

    @SuppressWarnings("SameParameterValue")
    static private byte[] parseByteStr(String s) {
        ArrayList<Byte> a = new ArrayList<>();
        for (String hex : s.split("\\s+")) {
            byte b = (byte) Integer.parseInt(hex, 16);
            a.add(b);
        }
        byte[] result = new byte[a.size()];
        for (int i = 0; i < a.size(); i++) {
            result[i] = a.get(i);
        }
        return result;
    }

    public static Block createBlock0forTestStage() {
        return new Block(parseByteStr(
                "00 00 00 00  00 00 00 00  \n" +
                        "43 4F 4E 54  45 53 54 30  \n" +
                        "6C E4 BA AA  70 1C E0 FC  \n" +
                        "4B 72 9D 93  A2 28 FB 27  \n" +
                        "4D 11 E7 25 "
        ));
    }


    /**
     * Check that the last nZeros bits of sig[16] are all zeros
     *
     * @param sig    - MD5 16 bytes signature
     * @param nZeros - number of required zeros at the end
     * @return true if last bits are zeros
     */
    public static boolean checkSignatureZeros(byte[] sig, int nZeros) {
        if (sig.length != 16) {
            return false; // bad signature
        }
        int sigIndex = 15;  // start from last byte of MD5
        // First check in chunks of 8 bits - full bytes
        while (nZeros >= 8) {
            if (sig[sigIndex] != 0)
                return false;
            sigIndex -= 1;
            nZeros -= 8;
        }
        if (nZeros == 0) {
            return true;
        }
        //We have several bits to check for zero
        int mask = (1 << nZeros) - 1;  // mask for the last bits
        return (sig[sigIndex] & mask) == 0;
    }

    public static int getUnixTimestamp() {
        return (int) (System.currentTimeMillis() / 1000);
    }

    /**
     * @param len     - number of bytes to compare
     * @param a       - first array
     * @param a_start - start offset for a
     * @param b       - second array
     * @param b_start - start offset for b
     * @return true if array parts are equal
     */
    public static boolean ArraysPartEquals(int len, byte[] a, int a_start, byte[] b, int b_start) {
        for (int i = 0; i < len; i++) {
            if (a[a_start + i] != b[b_start + i]) {
                return true;
            }
        }
        return false;
    }

    /**
     * Do several attempts at solving the puzzle
     *
     * @param myWalletNum   - wallet number to mine for
     * @param prevBlock     - the previous block in the chain
     * @param attemptsCount - number of attempts
     * @return a new block OR null if failed
     */
    public static Block mineCoinAttempt(int myWalletNum, Block prevBlock, int attemptsCount) {
        int newSerialNum = prevBlock.getSerialNumber() + 1;
        byte[] prevSig = new byte[8];
        System.arraycopy(prevBlock.getBytes(), 24, prevSig, 0, 8);
        Block newBlock = Block.createNoSig(newSerialNum, myWalletNum, prevSig);
        Random rand = new Random();
        for (int attempt = 0; attempt < attemptsCount; attempt++) {
            long puzzle = rand.nextLong();
            newBlock.setLongPuzzle(puzzle);
            Block.BlockError result = newBlock.checkSignature();
            if (result != Block.BlockError.SIG_NO_ZEROS) {
                // if enough zeros - we got error because of other reason - e.g. sig field not set yet
                byte[] sig = newBlock.calcSignature();
                newBlock.setSignaturePart(sig);
                // recheck block
                result = newBlock.checkSignature();
                if (result != Block.BlockError.OK) {
                    return null; //failed
                }
                return newBlock;
            }
        }
        return null;
    }

    /*public static void main(String[] args) {
        int numCoins = Integer.parseInt(args[0]);
        System.out.println(String.format("Mining %d coins...", numCoins));
        ArrayList<Block> chain = new ArrayList<>();
        Block genesis = HanukCoinUtils.createBlock0forTestStage();
        chain.add(genesis);
        int wallet1 = HanukCoinUtils.groupNameToWalletCode("TEST1");
        int wallet2 = HanukCoinUtils.groupNameToWalletCode("TEST2");

        for(int i = 0; i < numCoins; i++) {
            long t1 = System.nanoTime();
            Block newBlock = null;
            Block prevBlock = chain.get(i);
            while (newBlock == null) {
                newBlock = mineCoinAttempt(wallet1, prevBlock, 10000000);
            }
            int tmp = wallet1;
            wallet1 = wallet2;
            wallet2 = tmp;
            if (newBlock.checkValidNext(prevBlock) != Block.BlockError.OK) {
                throw new RuntimeException("BAD BLOCK");
            }
            chain.add(newBlock);
            long t2 = System.nanoTime();
            System.out.println(String.format("mining took =%d milli", (int) ((t2 - t1) / 10000000)));
            System.out.println(newBlock.binDump());

        }


    }*/

    /**
     * Converts an {@link ArrayList} of {@link Byte} to a {@code byte[]}
     *
     * @param byteArray An {@link ArrayList} of {@link Byte}
     * @return An array of bytes from the {@link ArrayList}
     * @see ArrayList
     */
    public static byte[] byteArrFromByteAL(ArrayList<Byte> byteArray) {
        byte[] byteArr = new byte[byteArray.size()];
        for (int i = 0; i < byteArray.size(); i++) {
            byteArr[i] = byteArray.get(i);
        }
        return byteArr;
    }

    public static void loadFromMemory() {
        LocalBlockChain.load();
        LocalNodeList.load();
    }

    public static void saveToMemory() {
        LocalBlockChain.save();
        LocalNodeList.save();
    }

    public static Node[] chooseThreeNodes() {
        int numOfNodes = (int) Math.min(3, LocalNodeList.localList.values().stream().filter(node -> Server.whitelist.containsKey(node.getName()) && !node.getIsNew()).count());
        Node[] nodeArr = new Node[numOfNodes];
        int numOfChosenNodes = 0, i;
        boolean flag;
        while (numOfChosenNodes < numOfNodes) {
            flag = true;
            i = (new Random()).nextInt(numOfNodes);
            @SuppressWarnings("OptionalGetWithoutIsPresent")
            Node node = LocalNodeList.localList.values()
                    .stream()
                    .filter(aNode -> !aNode.getIsNew() && !(aNode.getHost().equals(Server.HOST) && aNode.getPort() == Server.PORT) && Server.whitelist.containsKey(aNode.getName()))
                    .skip(i)
                    .findFirst()
                    .get(); //gets i'th element of localList.values()
            for (int j = 0; j < numOfChosenNodes; j++)
                if (node.equals(nodeArr[j])) {
                    flag = false;
                }
            if (flag) {
                nodeArr[numOfChosenNodes] = node;
                numOfChosenNodes++;
            }
        }

        return nodeArr;
    }

    public static Map<String, Integer> loadWhitelistFromMemory() {
        try {
            return new BufferedReader(
                    new FileReader(
                            new File("src/resources/whitelist.txt")))
                    .lines()
                    .collect(
                            Collectors.toMap(teamName -> teamName, HanukCoinUtils::groupNameToWalletCode));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
