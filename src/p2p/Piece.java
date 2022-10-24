package p2p;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Piece {
    private static final CommonConfig commonConfig = CommonConfig.getInstance();
    private static final Peer currentPeer = peerProcess.getCurrentPeer();
    private static final Logger logger = Logging.getLOGGER();
    private static final List<Integer> received = new ArrayList<>();
    public static byte[] get(int index) {
        int offset = index * commonConfig.getPieceSize();
        int pieceSize = Math.min(commonConfig.getPieceSize(),
                commonConfig.getFileSize() - index * commonConfig.getPieceSize());
        return currentPeer.getFileHandler().get(offset, pieceSize);
    }

    public static void store(byte[] piece, Integer index)  {
        received.add(index);
        logger.log(Level.FINE, "index " + index + " " + received.size());
        int offset = index * commonConfig.getPieceSize();
        currentPeer.getFileHandler().put(piece, offset);
        currentPeer.getBitField().setBit(index);
        currentPeer.removeNeededPiece(index);

    }

    public static Integer getPieceIndex(byte[] piece) {
        byte[] indexBytes = new byte[4];
        System.arraycopy(piece, 0, indexBytes, 0, indexBytes.length);
        return ByteConversionUtil.bytesToInt(indexBytes);
    }

    public static byte[] getPieceContent(byte[] piece) {
        byte[] indexBytes = new byte[piece.length - 4];
        System.arraycopy(piece, 4, indexBytes, 0, indexBytes.length);
        return indexBytes;
    }

    public static byte[] requestPiece(int remotePeerid) {
        Integer pieceIndex = currentPeer.selectPiece(remotePeerid);

        if (pieceIndex == null) {
            return null;
        }

        byte[] pieceIndexByte = ByteConversionUtil.intToBytes(pieceIndex);
        byte[] pieceContent = get(pieceIndex);
        byte[] piece = new byte[pieceIndexByte.length + pieceContent.length];

        System.arraycopy(pieceIndexByte, 0, piece, 0, pieceIndexByte.length);
        System.arraycopy(pieceContent, 0, piece, pieceIndexByte.length, piece.length);
        return piece;
    }
}
