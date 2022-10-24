package p2p;

import java.io.IOException;

public class PeerInfo {
    private int peerid;
    private String hostName;
    private int portno;
    private boolean hasFile;

    public PeerInfo(int peerid, String hostName, int portno, boolean hasFile) {
        this.peerid = peerid;
        this.hostName = hostName;
        this.portno = portno;
        this.hasFile = hasFile;
    }


    public int getPeerid() {
        return peerid;
    }

    public void setPeerid(int peerid) {
        this.peerid = peerid;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public int getPortno() {
        return portno;
    }

    public void setPortno(int portno) {
        this.portno = portno;
    }

    public boolean hasFile() {
        return hasFile;
    }

    public void setHasFile(boolean hasFile) {
        this.hasFile = hasFile;
    }
}
