package p2p;

public enum MessageType {

    CHOKE(0),
    UNCHOKE(1),
    INTERESTED(2),
    NOTINTRESTED(3),
    HAVE(4),
    BITFIELD(5),
    REQUEST(6),
    PIECE(7);

    private final int value;

    MessageType(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }
}
