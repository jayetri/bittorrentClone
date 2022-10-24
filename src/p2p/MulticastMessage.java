package p2p;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MulticastMessage extends Thread {
    private byte[] message;
    Set<Integer> nodes;
    MessageType messageType;
    boolean hasCompleted;
    //for all the connections
    public MulticastMessage(byte[] message) {
        this.message = message;
        this.messageType = Message.getMessageType(message);
        nodes = new HashSet<>(peerProcess.getCurrentPeer().getConnections().keySet());
    }

    //only to provided nodes
    public MulticastMessage(byte[] message, Set<Integer> nodes) {
        this.message = message;
        this.nodes = new HashSet<>(nodes);
        this.messageType = Message.getMessageType(message);
    }

    //only to provided nodes
    public MulticastMessage(MessageType messageType, Set<Integer> nodes) {
        this.messageType = messageType;
        this.nodes = new HashSet<>(nodes);
    }

    public void run() {
        Map<Integer, PeerHandler> connections = peerProcess.getCurrentPeer().getConnections();
        int numberOfCores = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numberOfCores);
        for (Integer peerid : nodes) {
            switch (messageType) {
                case INTERESTED:
                    executor.submit(() -> connections.get(peerid).sendInterestedMessage());
                    break;
                case NOTINTRESTED:
                    executor.submit(() -> connections.get(peerid).sendNotInterestedMessage());
                    break;
                default:
                    executor.submit(() -> connections.get(peerid).sendMessage(message));
                    break;
            }

        }

        executor.shutdown();

        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        hasCompleted = true;
    }
}
