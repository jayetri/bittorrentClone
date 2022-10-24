package p2p;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * A handler thread class.  Handlers are spawned from the listening
 * loop and are responsible for dealing with a single client's requests.
 */
    class ClientHandler extends Thread implements PeerHandler {
    private final Socket connection;
    private ObjectInputStream in;    //stream read from the socket
    private ObjectOutputStream out;    //stream write to the socket
    private final Peer currentPeer;
    Integer remotePeerid;
    Logger logger = Logging.getLOGGER();
    CommonConfig commonConfig = CommonConfig.getInstance();
    private boolean currentPeerChoked;
    Integer requestedPieceIndex;
    int totalByteSent;
    int totalByteReceived;
    private float pieceRequestStartTime;

    public ClientHandler(Socket connection, Peer currentPeer) {
        this.connection = connection;
        this.currentPeer = currentPeer;
        this.pieceRequestStartTime = 0.0f;
    }

    public void run() {
        try {
            Message message = new Message();
            //initialize Input and Output streams
            out = new ObjectOutputStream(connection.getOutputStream());
            out.flush();
            in = new ObjectInputStream(connection.getInputStream());

            byte[] receivedHandshakeByte = new byte[32];
            readMessage(receivedHandshakeByte);
            int remotePeerid = message.verifyHandshakeMessage(receivedHandshakeByte);
            this.remotePeerid = remotePeerid;
            logger.log(Level.INFO, "Peer [" + currentPeer.getPeerid() +"] is connected from ["
                    + remotePeerid + "]");
            logger.log(Level.INFO, "Peer [" + currentPeer.getPeerid() + "] received handshake message : "
                    + ByteConversionUtil.bytesToString(receivedHandshakeByte));

            //reply with handshake message
            sendMessage(message.handshakeMessage(currentPeer.getPeerid()));

            BitField bitField = currentPeer.getBitField();

            bitField.lock();
            sendMessage(Message.message(MessageType.BITFIELD, bitField.getBitFieldString()));
            currentPeer.addConnection(remotePeerid, this);
            bitField.unlock();
            //read bit field message
            byte[] bitFieldMessage = new byte[5 + commonConfig.getNumOfPieces()];
            readMessage(bitFieldMessage);

            handleBitFieldMessage(bitFieldMessage);

            while (!peerProcess.shouldTerminate()) {
                byte[] messageLengthByte = new byte[4];
                byte[] messageType = new byte[1];
                int byteRead = readMessage(messageLengthByte);

                if (byteRead == -1) {
                    break;
                }

                byteRead = readMessage(messageType);

                if (byteRead == -1) {
                    break;
                }

                int messageLength = ByteConversionUtil.bytesToInt(messageLengthByte);
                byte[] messagePayload = new byte[messageLength - messageType.length];
                byteRead = readMessage(messagePayload);

                if (byteRead == -1) {
                    break;
                }

                logger.log(Level.FINE, " Total Received Bytes so far : " + totalByteReceived);
                new MessageHandler(this, currentPeer, remotePeerid, ByteConversionUtil.bytesToString(messageType), messagePayload).start();
            }


        } catch (IOException ignored) {
        } finally {
            //Close connections
            try {
                in.close();
                out.close();
                connection.close();
            } catch (IOException ignored) {
            }
        }
    }

    synchronized public float getPieceRequestStartTime() {
        return pieceRequestStartTime;
    }

    synchronized public void setPieceRequestStartTime(float startTime) {
        this.pieceRequestStartTime = startTime;
    }

    private void handleBitFieldMessage(byte[] message) {
        logger.log(Level.INFO, "Received 'Bitfield' Message from [" + remotePeerid + "]");
        //add received bit field to current peer's remotePeer bitfield map
        byte[] messageLengthByte = new byte[4];
        byte[] messageTypeByte = new byte[1];

        System.arraycopy(message, 0, messageLengthByte, 0, messageLengthByte.length);
        int messageLength = ByteConversionUtil.bytesToInt(messageLengthByte);
        byte[] messagePayload = new byte[messageLength - messageTypeByte.length];
        System.arraycopy(message, messageLengthByte.length + messageTypeByte.length, messagePayload, 0, messagePayload.length);
        BitField receivedBitField = new BitField(ByteConversionUtil.bytesToString(messagePayload));
        if (receivedBitField.areAllBitsSet()) {
            peerProcess.addPeerWithFile(remotePeerid);
        }

        currentPeer.addPeersBitField(remotePeerid, receivedBitField);
        evaluateRemoteBitField(receivedBitField);
    }

    private void evaluateRemoteBitField(BitField remoteBitField) {
        BitField bitField = currentPeer.getBitField();

        //respond interested/not-interested message
        if (bitField.containsInterestedPieces(remoteBitField.getBitFieldString())) {
            sendInterestedMessage();
        } else {
            sendNotInterestedMessage();
        }
    }


    //send a message to the output stream
    public void sendMessage(String msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException ignored) {
        }
    }

    public int readMessage(byte[] msg) {
        int bytesRead = 0;
        try {
            while (bytesRead != msg.length && bytesRead != -1) {
                bytesRead += in.read(msg, bytesRead, msg.length - bytesRead);
            }
            totalByteReceived += bytesRead;
            return bytesRead;
        } catch (IOException ignored) {
        }

        if (bytesRead == 0) {
            bytesRead = -1;
        }
        return bytesRead;
    }
        @Override
    //send a message to the output stream
    synchronized public void sendMessage(byte[] msg) {
        try {
            totalByteSent += msg.length;
            logger.log(Level.FINE, "send msg " + msg.length + " Total Sent Bytes " + totalByteSent);
            out.write(msg);
            out.flush();
        } catch (IOException ignored) {

        }
    }

    @Override
    public synchronized void sendInterestedMessage() {
        sendMessage(Message.message(MessageType.INTERESTED));
        currentPeer.addInterestingPeer(remotePeerid);
    }

    @Override
    public synchronized void setIsCurrentPeerChoked(boolean isCurrentPeerChoked) {
        this.currentPeerChoked = isCurrentPeerChoked;
    }

    @Override
    public synchronized void sendNotInterestedMessage() {
        currentPeer.removeInterestingPeer(remotePeerid);
        sendMessage(Message.message(MessageType.NOTINTRESTED));
    }

    @Override
    public boolean isCurrentPeerChoked() {
        return currentPeerChoked;
    }

    @Override
    public void sendRequestMessage() {
        Integer nextPieceIndex = currentPeer.selectPiece(this.remotePeerid);
        logger.log(Level.FINE, " requested piece " + nextPieceIndex + "remote id " + remotePeerid);
        if (nextPieceIndex == null) {
            return;
        }

        requestedPieceIndex = nextPieceIndex;
        if (!this.isCurrentPeerChoked()) {
            this.sendMessage(Message.requestMessage(nextPieceIndex));
        }
    }

    @Override
    synchronized public void close() {
        try{
            in.close();
            out.close();
            connection.close();
        }
        catch(IOException ignored) {
        }
    }

    public Integer getRequestedPieceIndex() {
        return requestedPieceIndex;
    }
}

