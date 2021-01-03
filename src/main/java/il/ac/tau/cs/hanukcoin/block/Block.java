package main.java.il.ac.tau.cs.hanukcoin.block;

/*
 * Block is 36 byte/288bit long.
 * record/block format:
 * 32 bit serial number
 * 32 bit wallet number
 * 64 bit prev_sig[:8]highest bits  (first half ) of previous block's signature (including all the block)
 * 64 bit puzzle answer
 * 96 bit sig[:12] - md5 - the first 12 bytes of md5 of above fields. need to make last N bits zero
 */


import main.java.il.ac.tau.cs.hanukcoin.HanukCoinUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Class that represents one block in the block chain.
 * The block holds a 36-bytes array and all operations are performed directly on this array.
 */
public class Block {
    /**
     * The size of the block in bytes according to the specification <a href="http://bit.ly/HanukCoin">here.</a>
     */
    public static final int BLOCK_SZ = 36;
    /**
     * The data that represents the block
     */
    protected byte[] data;


    /**
     * Copy constructor, constructs a block from already existing data.
     *
     * @param bytes bytes corresponding to a Block.
     */
    public Block(byte[] bytes) {
        this.data = Arrays.copyOf(bytes, 36);
    }

    private Block() {
    }

    /**
     * Creates a block without a signature or puzzle fields.
     *
     * @param serialNumber The block's serial number
     * @param walletNumber Wallet number of the group
     * @param prevSig8     last 8 bytes of the signature of the previous block.
     * @return new block with the according parameters
     */
    public static Block createNoSig(int serialNumber, int walletNumber, byte[] prevSig8) {
        Block b = new Block();
        b.data = new byte[36];
        HanukCoinUtils.intIntoBytes(b.data, 0, serialNumber);
        HanukCoinUtils.intIntoBytes(b.data, 4, walletNumber);
        System.arraycopy(prevSig8, 0, b.data, 8, 8);
        return b;
    }

    /**
     * Creates the according block from all the fields.
     *
     * @param serialNumber The block's serial number.
     * @param walletNumber Wallet number of the group.
     * @param prevSig8     Last 8 bytes of the signature of the previous block.
     * @param puzzle8      the puzzle for this block.
     * @param sig12        Last 12 bytes of this block's signature.
     * @return A new block according to the parameters.
     */
    public static Block create(int serialNumber, int walletNumber, byte[] prevSig8, byte[] puzzle8, byte[] sig12) {
        Block b = createNoSig(serialNumber, walletNumber, prevSig8);
        System.arraycopy(sig12, 0, b.data, 24, 12);
        System.arraycopy(puzzle8, 0, b.data, 16, 8);
        return b;
    }

    public static Block readFrom(DataInputStream dis) throws IOException {
        Block b = new Block();
        b.data = new byte[BLOCK_SZ];
        dis.readFully(b.data);
        return b;
    }

    /**
     * takes a block without a signature and returns the fill block if signature is good, else null
     *
     * @param attemptBlock A block without a signature
     * @param md           md5 algorithm
     * @return next block is sig good, else null
     */
    public static Block blockIfSigCorrect(byte[] attemptBlock, MessageDigest md) {
        byte[] sig = md.digest(attemptBlock);
        if (HanukCoinUtils.checkSignatureZeros(sig, HanukCoinUtils.nZeros)) {
            Block ret = new Block();
            ret.data = new byte[BLOCK_SZ];
            System.arraycopy(attemptBlock, 0, ret.data, 0, 24);
            System.arraycopy(sig, 0, ret.data, 24, 12);
            return ret;
        }
        return null;
    }

    /**
     * Get the name of the group that mined the block, using {@link WalletCodeToGroupName}
     *
     * @return the name of the group
     */
    public String getName() {
        return WalletCodeToGroupName.walletCodeToGroupNameMap.getOrDefault(
                this.getWalletNumber(), "0x" + Integer.toHexString(this.getWalletNumber()));
    }

    /**
     * Gets the serial number of the block
     *
     * @return block's serial number
     */
    public int getSerialNumber() {
        return HanukCoinUtils.intFromBytes(data, 0);
    }

    /**
     * Sets this block's serial number to a new one
     *
     * @param newSerialNumber the new serial number.
     */
    public void setSerialNumber(int newSerialNumber) {
        HanukCoinUtils.intIntoBytes(this.data, 0, newSerialNumber);
    }

    /**
     * Gets this block's wallet number
     *
     * @return the wallet number.
     */
    public int getWalletNumber() {
        return HanukCoinUtils.intFromBytes(data, 4);
    }

    /**
     * @return The signature stored in the block.
     */
    public byte[] getSig() {
        return Arrays.copyOfRange(data, 24, 36);
    }

    /**
     * Gets the blocks puzzle, used to determine witch blockChain to use when there are two of the same length.
     *
     * @return the puzzle of this block.
     */
    public long getPuzzle() {
        return HanukCoinUtils.longFromBytes(data, 16);
    }

    /**
     * put 8 bytes data into puzzle field
     *
     * @param longPuzzle - 64 bit puzzle
     */
    public void setLongPuzzle(long longPuzzle) {
        // Treat it as 2 32bit integers
        HanukCoinUtils.intIntoBytes(data, 16, (int) (longPuzzle >> 32));
        HanukCoinUtils.intIntoBytes(data, 20, (int) (longPuzzle));
    }

    @Override
    public String toString() {
        return String.format("SN: %d | WN: %s | PS: %s | Puzzle: %d | Sig: %s",
                this.getSerialNumber(),
                WalletCodeToGroupName.walletCodeToGroupNameMap.getOrDefault(this.getWalletNumber(), String.valueOf(this.getWalletNumber())),
                Arrays.toString(this.getPrevSig()),
                this.getPuzzle(),
                Arrays.toString(this.getSig()));
    }

    /**
     * given a block signature - take first 12 bytes of it and put into the signature field of this block
     */
    public void setSignaturePart(byte[] sig) {
        System.arraycopy(sig, 0, data, 24, 12);
    }


    /**
     * calc block signature based on all fields besides signature itself.
     *
     * @return 16 byte MD5 signature
     */
    public byte[] calcSignature() {
        MessageDigest md;// may cause NoSuchAlgorithmException
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Internal error - missing MD5");
        }
        md.update(data, 0, 24);
        return md.digest();
    }

    /**
     * getter for internal bytes of block
     *
     * @return the data
     */
    public byte[] getBytes() {
        return data;
    }

    /**
     * calc signature for the block and see if in has the required number of zeros and matches signature written to the block
     *
     * @return BlockError: SIG_NO_ZEROS or SIG_BAD or OK
     */
    public BlockError checkSignature() {
        byte[] sig = calcSignature();
        int serialNum = getSerialNumber();
        int nZeros = HanukCoinUtils.numberOfZerosForPuzzle(serialNum);
        if (!HanukCoinUtils.checkSignatureZeros(sig, nZeros)) {
            return BlockError.SIG_NO_ZEROS;
        }
        if (HanukCoinUtils.ArraysPartEquals(12, data, 24, sig, 0)) {
            return BlockError.SIG_BAD;
        }
        return BlockError.OK;
    }

    /**
     * given a block previous to this one - check if this one is valid.
     *
     * @param prevBlock the previous block in the chain
     * @return BlockError
     */
    public BlockError checkValidNext(Block prevBlock) {
        if (getSerialNumber() != prevBlock.getSerialNumber() + 1) {
            return BlockError.BAD_SERIAL_NO;  // bad serial number - should be prev + 1
        }
        if (getWalletNumber() == prevBlock.getWalletNumber()) {
            return BlockError.SAME_WALLET_PREV;  // don't allow two consequent blocks with same wallet
        }
        if (HanukCoinUtils.ArraysPartEquals(8, data, 8, prevBlock.data, 24)) {
            return BlockError.NO_PREV_SIG;  // check prevSig field is indeed signature of prev block
        }
        return checkSignature();
    }

    /**
     * @return Whether the block is the genesis block
     */
    public boolean isGenBlock() {
        return Arrays.equals(this.data, HanukCoinUtils.createBlock0forTestStage().getBytes());
    }

    /**
     * String with HEX dump of block for debugging.
     *
     * @return string - hex dump
     */
    public String binDump() {
        StringBuilder dump = new StringBuilder();
        for (int i = 0; i < BLOCK_SZ; i++) {
            if ((i % 4) == 0) {
                dump.append(" ");
            }
            if ((i % 8) == 0) {
                dump.append("  ");
            }
            dump.append(String.format("%02X ", data[i]));
        }
        return dump.toString();
    }

    /**
     * Comparision function
     *
     * @param other other block
     * @return Whether the two blocks are the same
     */
    public boolean equals(Block other) {
        return Arrays.equals(this.data, other.getBytes());
    }

    public byte[] getPrevSig() {
        return Arrays.copyOfRange(this.data, 8, 16);
    }

    public enum BlockError {OK, BAD_SERIAL_NO, SAME_WALLET_PREV, NO_PREV_SIG, SIG_NO_ZEROS, SIG_BAD}
}

