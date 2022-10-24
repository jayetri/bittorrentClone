package p2p;

public class CommonConfig {
    private int numberOfPreferredNeighbors;
    private int unchokingInterval;
    private int optimisticUnchokingInterval;
    private String fileName;
    private int fileSize;
    private int pieceSize;
    private static CommonConfig commonConfig = null;
    private CommonConfig() {

    }

    //static method to create instance of Singleton class
    public static CommonConfig getInstance() {
        if (commonConfig == null)
            commonConfig = new CommonConfig();

        return commonConfig;
    }
    public int getNumberOfPreferredNeighbors() {
        return commonConfig.numberOfPreferredNeighbors;
    }

    public void setNumberOfPreferredNeighbors(int numberOfPreferredNeighbors) {
        this.numberOfPreferredNeighbors = numberOfPreferredNeighbors;
    }

    public int getUnchokingInterval() {
        return commonConfig.unchokingInterval;
    }

    public void setUnchokingInterval(int unchokingInterval) {
        this.unchokingInterval = unchokingInterval;
    }

    public int getOptimisticUnchokingInterval() {
        return optimisticUnchokingInterval;
    }

    public void setOptimisticUnchokingInterval(int optimisticUnchokingInterval) {
        this.optimisticUnchokingInterval = optimisticUnchokingInterval;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        commonConfig.fileName = fileName;
    }

    public int getFileSize() { return commonConfig.fileSize; }

    public void setFileSize(int fileSize) {
        commonConfig.fileSize = fileSize;
    }

    public int getPieceSize() {
        return pieceSize;
    }

    public void setPieceSize(int pieceSize) {
        commonConfig.pieceSize = pieceSize;
    }

    public int getNumOfPieces() {
        return (int) Math.ceil((double) commonConfig.fileSize / (double) commonConfig.pieceSize);
    }

}
