package p2p;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Iterator;

import java.util.*;
import java.lang.*;
import java.io.*;

public class Peer {
    private int peerid;
    private String hostName;
    private int portno;
    private boolean hasFile;
    private Set<Integer> neighbours;
    private Integer optimisticNeighbour;
    private final BitField bitField;
    private Map<Integer, BitField> peersBitField;
    private Set<Integer> interestedPeers; // peers which are interested in this peer data
    private final Set<Integer> interestingPeers; // peers in which this peer is interested
    private final List<Integer> neededPieces;
    private Set<Integer> requestedPieces;
    private final FileHandler fileHandler;
    private final Map<Integer, PeerHandler> connections;
    private final ScheduledThreadPoolExecutor prefferedNeighbourScheduler;
    private final ScheduledThreadPoolExecutor optimisticNeighbourScheduler;
    private final ReentrantLock reentrantLock;
    public  Map<Integer, Float> downloadSpeed;

    CommonConfig commonConfig = CommonConfig.getInstance();
    Logger logger = Logging.getLOGGER();

    public Peer(PeerInfo peerInfo) throws IOException {
        this.peerid = peerInfo.getPeerid();
        this.hostName = peerInfo.getHostName();
        this.portno = peerInfo.getPortno();
        this.hasFile = peerInfo.hasFile();
        this.connections = new HashMap<>();
        this.interestedPeers = new HashSet<>();
        this.peersBitField = new HashMap<>();
        this.neighbours = new HashSet<>();
        this.interestedPeers = new HashSet<>();
        this.interestingPeers = new HashSet<>();
        this.requestedPieces = new HashSet<>();
        this.neededPieces = new ArrayList<>();
        this.downloadSpeed = new HashMap<>();

        bitField = new BitField(hasFile);
        peersBitField = new HashMap<>();
        fileHandler = new FileHandler(this);
        prefferedNeighbourScheduler = new ScheduledThreadPoolExecutor(1);
        optimisticNeighbourScheduler = new ScheduledThreadPoolExecutor(1);
        reentrantLock = new ReentrantLock();
        setNeededPieces();
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

    public Set<Integer> getNeighbours() {
        return neighbours;
    }

    synchronized public void setNeighbours(Set<Integer> neighbours) {
        this.neighbours = neighbours;
    }


    public Map<Integer, PeerHandler> getConnections() {
        return connections;
    }

    synchronized public void addConnection(int remotePeerid, PeerHandler peerHandler) {
        this.connections.put(remotePeerid, peerHandler);
    }

    public PeerHandler getConnection(int remotePeerid) {
        return connections.get(remotePeerid);
    }

    public void addNeededPiece(int pieceIndex) {
        reentrantLock.lock();
        try {
            this.neededPieces.add(pieceIndex);
        } finally {
            reentrantLock.unlock();
        }
    }

    public void setNeededPieces() {
        String bitFieldString = bitField.getBitFieldString();
        reentrantLock.lock();
        try {
            for (int i = 0; i < bitFieldString.length(); i++) {
                if (bitFieldString.charAt(i) == '0') {
                    this.neededPieces.add(i);
                }
            }
        } finally {
            reentrantLock.unlock();
        }

    }

    public float getDownloadSpeed(int remotePeerid) {
        reentrantLock.lock();
        try {
            return downloadSpeed.getOrDefault(remotePeerid, 0.0f);
        } finally {
            reentrantLock.unlock();
        }
    }

    public void setDownloadSpeed(Map<Integer, Float> downloadSpeed) {
        reentrantLock.lock();
        try {
            this.downloadSpeed = downloadSpeed;
        } finally {
            reentrantLock.unlock();
        }

    }

    public void removeNeededPiece(Integer pieceIndex) {
        reentrantLock.lock();
        try {
            this.neededPieces.remove(pieceIndex);
            this.requestedPieces.remove(pieceIndex);
        } finally {
            reentrantLock.unlock();
        }
    }

     public void addDownloadSpeed(int remotePeerid, float startTime, float stopTime, int piecelength) {
        reentrantLock.lock();
        try {
            float diff = stopTime - startTime;
            float elapsed = TimeUnit.MILLISECONDS.convert((long)diff,TimeUnit.NANOSECONDS) / 1000.0f;

            float downloadRate = 0.0f;
            if(elapsed != 0) {
                downloadRate = piecelength / elapsed;
            }
            downloadSpeed.put(remotePeerid, downloadRate);
        } finally {
            reentrantLock.unlock();
        }
    }


    public Integer getNeededPiece(int index) {
        reentrantLock.lock();
        try {
            return this.neededPieces.get(index);
        } finally {
            reentrantLock.unlock();
        }
    }

    public List<Integer> getNeededPieces() {
        reentrantLock.lock();
        try {
            return this.neededPieces;
        } finally {
            reentrantLock.unlock();
        }

    }

    public void setRequestedPieces(Set<Integer> requestedPieces) {
        reentrantLock.lock();
        try {
            this.requestedPieces = requestedPieces;
        } finally {
            reentrantLock.unlock();
        }
    }

    public void clearRequestedPieces() {
        reentrantLock.lock();
        try {
            this.requestedPieces.clear();
        } finally {
            reentrantLock.unlock();
        }
    }

    public void removeRequestedPiece(Integer requestedPieceIndex) {
        reentrantLock.lock();
        try {
            this.requestedPieces.remove(requestedPieceIndex);
        } finally {
            reentrantLock.unlock();
        }
    }

    public Set<Integer> getRequestedPieces() {
        return this.requestedPieces;
    }

    synchronized public Integer selectPiece(int remotePeerid) {
        reentrantLock.lock();
        Integer randomIndex = null;
        try {
            if (neededPieces.size() == 0 || (neededPieces.size() == requestedPieces.size())) {
                return randomIndex;
            }

            BitField remoteBitField = this.getPeerBitField(remotePeerid);
            Set<Integer> remotePeerPieces = new HashSet<>(remoteBitField.getHavePieces());
            remotePeerPieces.removeAll(requestedPieces);
            remotePeerPieces.retainAll(neededPieces);

            if (remotePeerPieces.size() == 0) {
                return randomIndex;
            }

            randomIndex = ThreadLocalRandom.current().nextInt(remotePeerPieces.size());
            int i = 0;
            for (Integer pieceIndex : remotePeerPieces) {
                if (i == randomIndex) {
                    requestedPieces.add(pieceIndex);
                    logger.log(Level.FINE, "Inside Peer Requested Piece " + pieceIndex + " remote id " + remotePeerid);
                    return pieceIndex;
                }

                i++;
            }

            return null;
        } finally {
            reentrantLock.unlock();
        }
    }

    public void addBitField(int peerid, BitField bitField) {
        peersBitField.put(peerid, bitField);
    }

    public Map<Integer, BitField> getPeersBitField() {
        return peersBitField;
    }

    public BitField getPeerBitField(int remotePeerid) {
        return peersBitField.get(remotePeerid);
    }

    synchronized public void addPeersBitField(int remotePeerid, BitField peerBitField) {
        peersBitField.put(remotePeerid, peerBitField);
    }

    synchronized public void addInterestedPeers(int remotePeerid) {
        interestedPeers.add(remotePeerid);
    }

    synchronized public void removeInterestedPeers(int remotePeerid) {
        interestedPeers.remove(remotePeerid);
    }

    public Set<Integer> getInterestingPeers() {
        reentrantLock.lock();
        try {
            return interestingPeers;
        } finally {
            reentrantLock.unlock();
        }
    }

    synchronized public void addInterestingPeer(Integer remotePeerid) {
        this.interestingPeers.add(remotePeerid);
    }

    public void removeInterestingPeer(Integer remotePeerid) {
        reentrantLock.lock();
        try {
            this.interestingPeers.remove(remotePeerid);

            if (this.interestingPeers.size() == 0) {
                this.setHasFile(true);
            }
        } finally {
            reentrantLock.unlock();
        }

    }

    public void removeInterestingPeers(Set<Integer> nonInterestingPeers) {
        reentrantLock.lock();
        try {
            this.interestingPeers.removeAll(nonInterestingPeers);

            if (this.interestingPeers.size() == 0) {
                this.setHasFile(true);
            }
        } finally {
            reentrantLock.unlock();
        }

    }


    public void selectPreferredNeighbors() {
        prefferedNeighbourScheduler.scheduleAtFixedRate(() -> {
            logger.log(Level.FINE, "selectPreferredNeighbors");
            List<Integer> neighboursId = new ArrayList<>(interestedPeers);
            Set<Integer> prevNeighbour = new HashSet<>(this.neighbours);
            List<Integer> newNeighbour;
            clearRequestedPieces();
            if (hasFile) {
                newNeighbour = randomKElements(neighboursId, commonConfig.getNumberOfPreferredNeighbors());
            } else {
                logger.log(Level.INFO, "Inside");
                neighboursId.sort((Integer p1, Integer p2) -> {
                    //use download speed function here for sort
                    return Float.compare(this.getDownloadSpeed(p2), this.getDownloadSpeed(p1));
                });
                newNeighbour = neighboursId.subList(0, Math.min(commonConfig.getNumberOfPreferredNeighbors(), neighboursId.size()));
            }
            this.setNeighbours(new HashSet<>(newNeighbour));

            if (!prevNeighbour.equals(this.neighbours)) {
                logger.log(Level.INFO, "Peer [" + peerid + "] has the preferred neighbors " + newNeighbour);
            }

            //unchoke neighbours if not already
            new MulticastMessage(Message.unchokeMessage(), this.neighbours
                    .stream()
                    .filter(peerid -> !(prevNeighbour.contains(peerid)))
                    .collect(Collectors.toSet())).start();

            prevNeighbour.removeAll(this.neighbours);
            prevNeighbour.remove(this.optimisticNeighbour);

            //choke old neighbours if not selected again
            new MulticastMessage(Message.chokeMessage(), prevNeighbour).start();

        }, 1, commonConfig.getUnchokingInterval(), TimeUnit.SECONDS);


    }

    public void selectOptimisticUnchokedNeighbor() {

        optimisticNeighbourScheduler.scheduleAtFixedRate(() -> {
            logger.log(Level.FINE, "selectOptimisticUnchokedNeighbor");
            Integer prevOptimisticNeighbour = this.optimisticNeighbour;
            Set<Integer> potentialNeighbours = new HashSet<>(interestedPeers);
            potentialNeighbours.removeAll(getNeighbours());
            int index = ThreadLocalRandom.current().nextInt(0, potentialNeighbours.size());

            int i = 0;
            for (Integer potentialNeighbour : potentialNeighbours) {
                if (i == index) {
                    this.optimisticNeighbour = potentialNeighbour;
                }
            }

            if ((prevOptimisticNeighbour == null & this.optimisticNeighbour != null) ||
                    (prevOptimisticNeighbour != null && !prevOptimisticNeighbour.equals(this.optimisticNeighbour))) {
                logger.log(Level.INFO, "Peer [" + peerid + "] has the optimistically unchoked neighbor ["
                        + this.optimisticNeighbour + "]");
            }
            Set<Integer> optimisticNeighbourSet = new HashSet<>();
            optimisticNeighbourSet.add(optimisticNeighbour);
            Set<Integer> prevOptimisticNeighbourSet = new HashSet<>();
            prevOptimisticNeighbourSet.add(prevOptimisticNeighbour);
            new MulticastMessage(Message.unchokeMessage(), optimisticNeighbourSet.stream()
                    .filter(peerid -> !(peerid.equals(prevOptimisticNeighbour)))
                    .collect(Collectors.toSet())).start();
            prevOptimisticNeighbourSet.removeAll(optimisticNeighbourSet);
            //choke old neighbours if not selected again
            new MulticastMessage(Message.chokeMessage(), prevOptimisticNeighbourSet).start();

        }, 1, commonConfig.getOptimisticUnchokingInterval(), TimeUnit.SECONDS);
    }

    public BitField getBitField() {
        return bitField;
    }

    public FileHandler getFileHandler() {
        return fileHandler;
    }

    public void cleanup() {
        logger.log(Level.FINE, "cleanup");
        try {
            prefferedNeighbourScheduler.shutdownNow();
            optimisticNeighbourScheduler.shutdownNow();
            fileHandler.clean();
        } catch (Exception ignored) {

        }

        logger.log(Level.FINE, "Number of connections : " + connections.size());
        for (Map.Entry<Integer, PeerHandler> entry : connections.entrySet()) {
            logger.log(Level.FINE, "Closing " + entry.getKey());
            entry.getValue().close();
        }
    }

    private List<Integer> randomKElements(List<Integer> list, int K) {
        // create a temporary list for storing
        // selected element
        List<Integer> newList = new ArrayList<>();
        for (int i = 0; i < K && i < list.size(); i++) {
            // take a random index between 0 to size
            // of given List
            int randomIndex = ThreadLocalRandom.current().nextInt(list.size());

            // add element in temporary list
            newList.add(list.get(randomIndex));
            // Remove selected element from orginal list
            list.remove(randomIndex);
        }
        return newList;
    }
}
