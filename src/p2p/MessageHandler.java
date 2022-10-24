package p2p;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

public class MessageHandler extends Thread {
    private final MessageType messageType;
    private final byte[] messagePayload;
    private final PeerHandler peerHandler;
    private final Peer currentPeer;
    private final int remotePeerid;
    Logger logger = Logging.getLOGGER();
    CommonConfig commonConfig = CommonConfig.getInstance();

    public MessageHandler(PeerHandler peerHandler, Peer currentPeer, int remotePeerid, String messageType, byte[] messagePayload) {
        this.peerHandler = peerHandler;
        this.currentPeer = currentPeer;
        this.messageType = MessageType.values()[Integer.parseInt(messageType)];
        this.messagePayload = messagePayload;
        this.remotePeerid = remotePeerid;
    }

    public void run() {
        switch (messageType) {
            case HAVE: {
                int pieceIndex = ByteConversionUtil.bytesToInt(messagePayload);
                logger.log(Level.INFO, "Peer [" + currentPeer.getPeerid() + "] received the 'have' Message from " +
                        "[" + remotePeerid + "] for the piece [" + pieceIndex + "]");

                //update remote peer bitfield
                BitField remoteBitField = currentPeer.getPeerBitField(remotePeerid);
                remoteBitField.setBit(pieceIndex);
                logger.log(Level.FINE, "No of piece with peer [" + remotePeerid + "] = "
                        + remoteBitField.getNumOfSetBit());
                if (remoteBitField.getNumOfSetBit() == commonConfig.getNumOfPieces()) {
                    peerProcess.addPeerWithFile(remotePeerid);
                }

                evaluateRemoteBitField(pieceIndex);
                break;
            }

            case CHOKE:
                logger.log(Level.INFO, "Peer [" + currentPeer.getPeerid() + "] is choked by [" + remotePeerid + "]");
                peerHandler.setIsCurrentPeerChoked(true);
                currentPeer.removeRequestedPiece(peerHandler.getRequestedPieceIndex());
                peerHandler.setPieceRequestStartTime(0.0f);
                break;

            case UNCHOKE:
                logger.log(Level.INFO, "Peer [" + currentPeer.getPeerid() + "] is unchoked by [" + remotePeerid + "]");
                peerHandler.setIsCurrentPeerChoked(false);
                //send request message for piece if any else send not interested
                peerHandler.sendRequestMessage();
                logger.log(Level.FINE, "Unchoke request piece remote peerid " + remotePeerid);
                break;

            case PIECE: {
                // process piece and store
                int pieceIndex = Piece.getPieceIndex(messagePayload);
                Piece.store(Piece.getPieceContent(messagePayload), pieceIndex);

                logger.log(Level.INFO, "Peer [" + currentPeer.getPeerid()
                        + "] has downloaded the piece [" + pieceIndex + "] from [" + remotePeerid + "]");

                MulticastMessage multicastHaveMessage = new MulticastMessage(Message.haveMessage(pieceIndex));
                multicastHaveMessage.start();

                //evaluate bit field of interesting neighbours[pending]
                Set<Integer> interestingPeers = currentPeer.getInterestingPeers();

                Set<Integer> nonInterestingPeers = interestingPeers.stream()
                        .filter(peerid -> !currentPeer.getBitField().containsInterestedPieces
                                (currentPeer.getPeerBitField(peerid).getBitFieldString()))
                        .collect(Collectors.toSet());

                currentPeer.removeInterestingPeers(nonInterestingPeers);

                new MulticastMessage(MessageType.NOTINTRESTED, nonInterestingPeers).start();


                //send request message for piece if any or not choked (if no piece send not interested) [pending]
                peerHandler.sendRequestMessage();

                if (currentPeer.getNeededPieces().size() == 0) {
                    logger.log(Level.INFO, "Peer [" + currentPeer.getPeerid() + "] has downloaded the complete file.");

                    if (!multicastHaveMessage.hasCompleted) {
                        logger.log(Level.INFO, "Waiting for compeletion");
                        //wait until all the have messages are sent
                        try {
                            TimeUnit.SECONDS.sleep(5);
                        } catch (InterruptedException ignored) {

                        }
                    }

                    peerProcess.addPeerWithFile(currentPeer.getPeerid());
                    currentPeer.setHasFile(true);
                }
                break;
            }

            case REQUEST:
                int requestedPieceIndex = ByteConversionUtil.bytesToInt(messagePayload);
                logger.log(Level.INFO, "Peer [" + currentPeer.getPeerid() + "] received 'request' message from ["
                        + remotePeerid + "] for the piece [" + requestedPieceIndex + "]");

                //send piece
                byte[] piece = Message.pieceMessage(requestedPieceIndex);
                float stopTime = System.nanoTime();
                float startTime = peerHandler.getPieceRequestStartTime();
                if (startTime != 0.0f) {
                    currentPeer.addDownloadSpeed(remotePeerid, startTime, stopTime, piece.length);
                }
                peerHandler.setPieceRequestStartTime(stopTime);
                peerHandler.sendMessage(piece);
                break;

            case INTERESTED:
                logger.log(Level.INFO, "Peer [" + currentPeer.getPeerid()
                        + "] received the 'interested' message from [" + remotePeerid + "]");
                currentPeer.addInterestedPeers(remotePeerid);
                break;

            case NOTINTRESTED:
                logger.log(Level.INFO, "Peer [" + currentPeer.getPeerid()
                        + "] received the 'not interested' message from [" + remotePeerid + "]");
                currentPeer.removeInterestedPeers(remotePeerid);
                break;

            default:
                logger.log(Level.INFO, "Peer [" + currentPeer.getPeerid()
                        + "]received 'unknown' Message from [" + remotePeerid + "]");
                break;
        }
    }

    private void evaluateRemoteBitField(int bitIndex) {
        BitField bitField = currentPeer.getBitField();
        //respond interested/not-interested message
        if (bitField.getBitFieldString().charAt(bitIndex) == '0' && !currentPeer.getInterestingPeers().contains(remotePeerid)) {
            peerHandler.sendInterestedMessage();
        }
    }
}
