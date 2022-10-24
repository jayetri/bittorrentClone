package p2p;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A handler thread class.  Handlers are spawned from the listening
 * loop and are responsible for dealing with a single client's requests.
 */
    class ServerHandler extends Thread implements PeerHandler {
    Socket requestSocket;           //socket connect to the server
    ObjectOutputStream out;         //stream write to the socket
    ObjectInputStream in;          //stream read from the socket
    String serverHostname;                // hostname of the target server
    int serverPort;                  //port name of the target server
    Peer currentPeer;
    Integer remotePeerid;
    boolean currentPeerChoked;
    Integer requestedPieceIndex;
    int totalByteSent;
    int totalByteReceived;
    private float pieceRequestStartTime;
    Logger logger = Logging.getLOGGER();
    CommonConfig commonConfig = CommonConfig.getInstance();
    public ServerHandler(Peer currentPeer, String hostName, int serverPort, int remotePeerid) {
        this.currentPeer = currentPeer;
        this.serverHostname = hostName;
        this.serverPort = serverPort;
        this.remotePeerid = remotePeerid;
        this.pieceRequestStartTime = 0.0f;
    }

    public void run() {
        try {
            Message message = new Message();
            //create a socket to connect to the server
            requestSocket = new Socket(serverHostname, serverPort);

            //initialize inputStream and outputStream
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(requestSocket.getInputStream());

            //send handshake message
            sendMessage(message.handshakeMessage(currentPeer.getPeerid()));

            byte[] receivedHandshakeByte = new byte[32];

            //receive handshake message
            readMessage(receivedHandshakeByte);

           if (!message.verifyHandshakeMessage(receivedHandshakeByte, this.remotePeerid)) {
               logger.log(Level.INFO, "Peer [" + currentPeer.getPeerid() + "] received incorrect handshake message "
                       + "closing the connection");
               return;
           }

            logger.log(Level.INFO, "Peer [" + currentPeer.getPeerid() + "] received 'handshake' message from ["
                    + this.remotePeerid + "]");

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
                //logger.log(Level.INFO, " Total Received Byte fo far : " + totalByteReceived);

                if (byteRead == -1) {
                    //currentPeer.cleanup();
                    break;
                }

                byteRead = readMessage(messageType);

                if (byteRead == -1) {
                    //currentPeer.cleanup();
                    break;
                }

                int messageLength = ByteConversionUtil.bytesToInt(messageLengthByte);
                byte[] messagePayload = new byte[messageLength - messageType.length];
                byteRead = readMessage(messagePayload);

                if (byteRead == -1) {
                    //currentPeer.cleanup();
                    break;
                }

                logger.log(Level.FINE, " Total Received Byte fo far : " + totalByteReceived);
                new MessageHandler(this, currentPeer, remotePeerid, ByteConversionUtil.bytesToString(messageType), messagePayload).start();
            }

        }
        catch (ConnectException e) {
            System.err.println("Connection refused. You need to initiate a server first.");
        } catch(UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        }
        catch(IOException ioException){
            //ioException.printStackTrace();
        }
        finally {
            //Close connections
            try{
                in.close();
                out.close();
                requestSocket.close();
            }
            catch(IOException ioException) {
                //ioException.printStackTrace();
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
        logger.log(Level.INFO, "Peer [" + currentPeer.getPeerid() + "] received 'Bitfield' Message from [" + remotePeerid + "]");
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

    void sendMessage(String msg) {
        try{
            //stream write the message
            out.writeObject(msg);
            out.flush();
        }
        catch(IOException ioException){
            //ioException.printStackTrace();
        }
    }

    public int readMessage(byte[] msg) {
        int bytesRead = 0;
        try {
            while (bytesRead != msg.length && bytesRead != -1) {
                int read;
                //System.out.print("msg len " + msg.length);
                read = in.read(msg, bytesRead, msg.length - bytesRead);
                bytesRead += read;
                //System.out.println(" off " + bytesRead
                  //      + " len " + (msg.length - bytesRead));
            }

            totalByteReceived += bytesRead;
            return bytesRead;
        } catch (IOException ioException) {
            //ioException.printStackTrace();
        }

        if (bytesRead == 0) {
            bytesRead = -1;
        }
        return bytesRead;
    }

    @Override
    public synchronized void sendMessage(byte[] msg) {
        try{
            totalByteSent += msg.length;
            //logger.log(Level.FINE, "send msg " + msg.length + " Total Bytes Sent so far" + totalByteSent);
            //stream write the message
            out.write(msg);
            out.flush();
        }
        catch(IOException ioException){
            //ioException.printStackTrace();
        }
    }

    @Override
    public synchronized void sendInterestedMessage() {
        sendMessage(Message.message(MessageType.INTERESTED));
        currentPeer.addInterestingPeer(remotePeerid);
    }

    @Override
    public synchronized void sendNotInterestedMessage() {
        logger.log(Level.INFO, "Sending not interested message to " + remotePeerid);
        currentPeer.removeInterestingPeer(remotePeerid);
        sendMessage(Message.message(MessageType.NOTINTRESTED));
    }

    @Override
    public synchronized void setIsCurrentPeerChoked(boolean isCurrentPeerChoked) {
        this.currentPeerChoked = isCurrentPeerChoked;
    }

    @Override
    public synchronized void sendRequestMessage() {
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
    public boolean isCurrentPeerChoked() {
        return currentPeerChoked;
    }

    @Override
    synchronized public void close() {
        try{
            in.close();
            out.close();
            requestSocket.close();
        }
        catch(IOException ioException) {
            //ioException.printStackTrace();
        }
    }

    public Integer getRequestedPieceIndex() {
        return requestedPieceIndex;
    }
}
