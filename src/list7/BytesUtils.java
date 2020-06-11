package list7;

public class BytesUtils {
    private static String formatBits(String bits) {
        return bits + "0".repeat(8 - bits.length());
    }


    public static String bytesToBitString(byte[] bytes) {
        StringBuilder bits = new StringBuilder();
        for (byte b : bytes) {
            String block = Integer.toBinaryString(b);
            String formatted = formatBits(block);
            bits.append(formatted);
        }
        return bits.toString();
    }
}
