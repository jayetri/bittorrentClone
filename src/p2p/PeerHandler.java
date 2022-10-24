package p2p;

import java.io.IOException;

public interface PeerHandler {

    float getPieceRequestStartTime();

    void setPieceRequestStartTime(float startTime);

    void sendMessage(byte[] msg);

    void setIsCurrentPeerChoked (boolean isCurrentPeerChoked);

    boolean isCurrentPeerChoked();

    void sendInterestedMessage();

    void sendNotInterestedMessage();

    void sendRequestMessage();

    Integer getRequestedPieceIndex();

    void close();
}
