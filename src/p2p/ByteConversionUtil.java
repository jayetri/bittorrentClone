package p2p;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ByteConversionUtil {

    //convert byte array of len 4 and return the int
    public static int bytesToInt (byte[] intBytes) {
        return ByteBuffer.wrap(intBytes).getInt();
    }

    //convert int to byte array of len 4
    public static byte[] intToBytes (int number) {
        return ByteBuffer.allocate(4).putInt(number).array();
    }

    //convert string byte array
    public static byte[] stringToBytes (String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    //convert byte array to String
    public static String bytesToString (byte[] stringBytes) {
        return new String(stringBytes, StandardCharsets.UTF_8);
    }

    public static String byteListToString(List<Byte> l) {
        if (l == null) {
            return "";
        }
        byte[] array = new byte[l.size()];
        int i = 0;
        for (Byte current : l) {
            array[i] = current;
            i++;
        }
        return bytesToString(array);
    }
}
