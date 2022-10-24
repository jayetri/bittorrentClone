package p2p;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


public class LoadConfig {
    final static String COMMON_CONFIG_FILE = "Common.cfg";
    final static String PEER_INFO_FILE = "PeerInfo.cfg";
    final static String NUMBER_OF_PREFERRED_NEIGHBORS = "NumberOfPreferredNeighbors";
    final static String UNCHOKING_INTERVAL = "UnchokingInterval";
    final static String OPTIMISTIC_UNCHOKING_INTERVAL = "OptimisticUnchokingInterval";
    final static String FILENAME = "FileName";
    final static String FILESIZE = "FileSize";
    final static String PIECESIZE = "PieceSize";

    public static CommonConfig loadCommonConfig() throws IOException {
        File file = new File(COMMON_CONFIG_FILE);
        CommonConfig commonConfig = CommonConfig.getInstance();
        Map<String, String> commonInfo = new HashMap<>();
        BufferedReader br = new BufferedReader(new FileReader(file));

        String info;
        String[] splitInfo;
        while ((info = br.readLine()) != null) {
            if (info.equals("")) {
                continue;
            }
            info= info.trim();
            splitInfo = info.split("\\s+", 2);
            commonInfo.put(splitInfo[0].trim(), splitInfo[1].trim());
        }

        if (commonInfo.containsKey(NUMBER_OF_PREFERRED_NEIGHBORS)) {
            commonConfig.setNumberOfPreferredNeighbors(Integer.parseInt(commonInfo.get(NUMBER_OF_PREFERRED_NEIGHBORS)));
        } else {
            //throw exception or error message
        }

        if (commonInfo.containsKey(UNCHOKING_INTERVAL)) {
            commonConfig.setUnchokingInterval(Integer.parseInt(commonInfo.get(UNCHOKING_INTERVAL)));
        } else {
            //throw exception or error message
        }

        if (commonInfo.containsKey(OPTIMISTIC_UNCHOKING_INTERVAL)) {
            commonConfig.setOptimisticUnchokingInterval(Integer.parseInt(commonInfo.get(OPTIMISTIC_UNCHOKING_INTERVAL)));
        } else {
            //throw exception or error message
        }

        if (commonInfo.containsKey(FILENAME)) {
            commonConfig.setFileName(commonInfo.get(FILENAME));
        } else {
            //throw exception or error message
        }

        if (commonInfo.containsKey(FILESIZE)) {
            commonConfig.setFileSize(Integer.parseInt(commonInfo.get(FILESIZE)));
        } else {
            //throw exception or error message
        }

        if (commonInfo.containsKey(PIECESIZE)) {
            commonConfig.setPieceSize(Integer.parseInt(commonInfo.get(PIECESIZE)));
        } else {
            //throw exception or error message
        }

        return commonConfig;
    }

    public static List<PeerInfo> loadPeersInfo() throws IOException {
        File file = new File(PEER_INFO_FILE);
        List<PeerInfo> peersList = new ArrayList<>();

        BufferedReader br = new BufferedReader(new FileReader(file));

        String info;
        String[] peerInfoArray;
        while ((info = br.readLine()) != null) {
            if (info.equals("")) {
                continue;
            }

            info= info.trim();
            peerInfoArray = info.split("\\s+", 4);
            int peerId = Integer.parseInt(peerInfoArray[0].trim());
            String host = peerInfoArray[1].trim();
            int portNo = Integer.parseInt(peerInfoArray[2].trim());
            boolean hasFile = Integer.parseInt(peerInfoArray[3].trim()) == 1;
            PeerInfo peerInfo = new PeerInfo(peerId, host, portNo, hasFile);
            peersList.add(peerInfo);
        }

        return peersList;
    }

    public static PeerInfo getCurrentPeer(int peerid) throws IOException {
        List<PeerInfo> peers = loadPeersInfo();

        for (PeerInfo peer : peers) {
            if (peer.getPeerid() == peerid) {
                return peer;
            }
        }

        return null;
    }
}
