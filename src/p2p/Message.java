package p2p;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Message {
    private static final String HEADER = "P2PFILESHARINGPROJ";
    private static final String ENCODING = "UTF-8";
    private static final int NUM_OF_ZERO_BITS = 10;
    static Logger logger = Logging.getLOGGER();

    public byte[] handshakeMessage(int peerid) {
        byte[] headerBytes = ByteConversionUtil.stringToBytes(HEADER);
        byte[] zeroBytes = new byte[NUM_OF_ZERO_BITS];
        byte[] peeridByte = ByteConversionUtil.intToBytes(peerid);
        return createMessage(headerBytes, zeroBytes, peeridByte);
    }

    public boolean verifyHandshakeMessage(byte[] receivedHandshakeMessage, int expectedPeerId) {
        int receivedPeerId = verifyHandshakeMessage(receivedHandshakeMessage);
        return receivedPeerId == expectedPeerId;
    }

    public int verifyHandshakeMessage(byte[] receivedHandshakeMessage) {
        byte[] headerByte = new byte[HEADER.length()];
        byte[] peeridByte = new byte[4];
        System.arraycopy(receivedHandshakeMessage, 0, headerByte, 0, 18);
        System.arraycopy(receivedHandshakeMessage, HEADER.length() + NUM_OF_ZERO_BITS, peeridByte, 0, 4);

        int peerid = ByteConversionUtil.bytesToInt(peeridByte);
        String header = ByteConversionUtil.bytesToString(headerByte);
        if (!header.equals(HEADER)) {
            logger.log(Level.INFO, "Received wrong header message from peer id: [" + peerid + "]");
        }
        return peerid;
    }

    public static byte[] haveMessage(int index) {
        byte[] indexBytes = ByteConversionUtil.intToBytes(index);
        return message(MessageType.HAVE, indexBytes);
    }

    public static byte[] unchokeMessage() {
        return message(MessageType.UNCHOKE);
    }

    public static byte[] chokeMessage() {
        return message(MessageType.CHOKE);
    }

    public static byte[] requestMessage(int index) {
        byte[] indexBytes = ByteConversionUtil.intToBytes(index);
        return message(MessageType.REQUEST, indexBytes);
    }

    public static byte[] pieceMessage(int pieceIndex) {
        byte[] pieceIndexByte = ByteConversionUtil.intToBytes(pieceIndex);
        byte[] pieceContent = Piece.get(pieceIndex);
        byte[] piece = new byte[pieceIndexByte.length + pieceContent.length];
        System.arraycopy(pieceIndexByte, 0, piece, 0, pieceIndexByte.length);
        System.arraycopy(pieceContent, 0, piece, pieceIndexByte.length, pieceContent.length);
        logger.log(Level.FINE, "PM " + piece.length + " pi " + pieceIndexByte.length + " pc " + pieceContent.length);
        return message(MessageType.PIECE, piece);
    }

    public static MessageType getMessageType(byte[] message) {
        byte[] messageType = new byte[1];
        System.arraycopy(message, 4, messageType, 0, messageType.length);
        return MessageType.values()[Integer.parseInt(ByteConversionUtil.bytesToString(messageType))];
    }

    //choke, unchoke, interested, not interested will use this method as they dont have payload
    public static byte[] message(MessageType messageType) {
        byte[] messageTypeByte = ByteConversionUtil.stringToBytes(Integer.toString(messageType.getValue()));
        byte[] payloadBytes = new byte[0];
        byte[] messageLengthBytes = ByteConversionUtil.intToBytes(messageTypeByte.length + payloadBytes.length);
        return createMessage(messageLengthBytes, messageTypeByte, payloadBytes);
    }

    public static byte[] message(MessageType messageType, String payload) {
        byte[] messageTypeByte = ByteConversionUtil.stringToBytes(Integer.toString(messageType.getValue()));
        byte[] payloadBytes = ByteConversionUtil.stringToBytes(payload);
        byte[] messageLengthBytes = ByteConversionUtil.intToBytes(messageTypeByte.length + payloadBytes.length);
        logger.log(Level.FINE, "Message Type : " + ByteConversionUtil.bytesToString(messageTypeByte) + " Len "
                + ByteConversionUtil.bytesToInt(messageLengthBytes));
        return createMessage(messageLengthBytes, messageTypeByte, payloadBytes);
    }

    public static byte[] message(MessageType messageType, byte[] payload) {
        byte[] messageTypeByte = ByteConversionUtil.stringToBytes(Integer.toString(messageType.getValue()));
        byte[] messageLengthBytes = ByteConversionUtil.intToBytes(messageTypeByte.length + payload.length);
        return createMessage(messageLengthBytes, messageTypeByte, payload);
    }

    private static byte[] createMessage(byte[] fistPart, byte[] middlePart, byte[] lastPart) {
        byte[] messageBytes = new byte[fistPart.length + middlePart.length + lastPart.length];
        System.arraycopy(fistPart, 0, messageBytes, 0, fistPart.length);
        System.arraycopy(middlePart, 0, messageBytes, fistPart.length, middlePart.length);
        System.arraycopy(lastPart, 0, messageBytes, fistPart.length + middlePart.length, lastPart.length);
        return messageBytes;
    }
}
