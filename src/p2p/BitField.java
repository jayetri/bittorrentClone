package p2p;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

public class BitField {
    private Set<Integer> havePieces;
    private final boolean[] bitField;
    private int numOfSetBit;
    Peer currentPeer;
    ReentrantLock reentrantLock;
    private static final CommonConfig commonConfig = CommonConfig.getInstance();

    public BitField (String bitFieldString) {
        reentrantLock = new ReentrantLock();
        bitField = new boolean[commonConfig.getNumOfPieces()];
        havePieces = new HashSet<>();
        setBitField(bitFieldString);

    }

    public BitField (boolean hasFile) {
        reentrantLock = new ReentrantLock();
        bitField = new boolean[commonConfig.getNumOfPieces()];
        havePieces = new HashSet<>();
        setBitField(hasFile);

    }

    public Set<Integer> getHavePieces() {
        return havePieces;
    }

    public boolean containsInterestedPieces (String receivedBitFieldString) {
        reentrantLock.lock();
        try {
            for (int i = 0; i < bitField.length; i++) {
                if (!bitField[i] && receivedBitFieldString.charAt(i) == '1') {
                    return true;
                }
            }
            return false;
        } finally {
            reentrantLock.unlock();
        }

    }

    public void setBit (int pieceIndex) {
        reentrantLock.lock();
        try {
            if (!bitField[pieceIndex]) {
                numOfSetBit++;
                bitField[pieceIndex] = true;
                havePieces.add(pieceIndex);
            }
        } finally {
            reentrantLock.unlock();
        }

    }

    public boolean completed () {
        return numOfSetBit == bitField.length;
    }

    private void setBitField (String bitFieldString)  {
        for (int i = 0; i < bitFieldString.length(); i++) {
            if (bitFieldString.charAt(i) == '1') {
                bitField[i] = true;
                numOfSetBit++;
                havePieces.add(i);
            }
        }
    }

    private void setBitField (Boolean hasFile) {
        if (!hasFile) {
            return;
        }

        Arrays.fill(bitField, true);

        numOfSetBit = bitField.length;

        for (int i = 0; i < bitField.length; i++) {
            havePieces.add(i);
        }
    }

    public String getBitFieldString () {

        StringBuilder bitFieldString = new StringBuilder();

        for (boolean bit : bitField) {
            if (bit) {
                bitFieldString.append("1");
            } else {
                bitFieldString.append("0");
            }
        }

        return bitFieldString.toString();
    }

    public int getNumOfSetBit() {
        return numOfSetBit;
    }

    public boolean areAllBitsSet() {
        return numOfSetBit == bitField.length;
    }

    public void lock() {
        reentrantLock.lock();
    }

    public void unlock() {
        reentrantLock.unlock();
    }
}
