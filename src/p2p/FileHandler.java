package p2p;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.locks.ReentrantLock;

public class FileHandler {
    private static CommonConfig commonConfig = CommonConfig.getInstance();
    private RandomAccessFile randomAccessFile;
    private Peer currentPeer;
    private ReentrantLock  reentrantLock;
    private final static String PATH = "./peer_";
    public FileHandler(Peer currentPeer) throws IOException {
        String fileName = PATH + currentPeer.getPeerid() + "/" + commonConfig.getFileName();
        File file = new File(fileName);
        file.createNewFile(); // if file already exists will do nothing
        randomAccessFile = new RandomAccessFile(fileName, "rw");
        randomAccessFile.setLength(commonConfig.getFileSize());
        reentrantLock = new ReentrantLock();
    }

    byte[] get(int offset, int len) {
        byte[] data = new byte[len];
        reentrantLock.lock();
        try {
            randomAccessFile.seek(offset);
            randomAccessFile.read(data);
        } catch (Exception ignored) {

        } finally {
            reentrantLock.unlock();
        }
        return data;
    }

    void put(byte[] data, int offset) {
        reentrantLock.lock();
        try {
            randomAccessFile.seek(offset);
            randomAccessFile.write(data);
        } catch (Exception ignored) {

        } finally {
            reentrantLock.unlock();
        }
    }

    void clean() throws IOException {
        randomAccessFile.close();
    }
}
