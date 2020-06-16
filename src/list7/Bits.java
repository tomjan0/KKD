package list7;

import java.util.Arrays;

public class Bits {
    private final boolean[] bits;

    private static String formatBits(String bits) {
        return "0".repeat(8 - bits.length()) + bits;
    }

    private static String byteToBits(byte b) {
        String block = Integer.toBinaryString((b & 0xff));
        return formatBits(block);
    }

    public Bits(boolean[] bits) {
        this.bits = Arrays.copyOf(bits, bits.length);
    }

    public Bits(byte[] bytes) {
        this.bits = new boolean[bytes.length * 8];
        for (int i = 0, j = 0; i < bytes.length; i++, j += 8) {
            String bitString = byteToBits(bytes[i]);
            for (int k = 0; k < 8; k++) {
                this.bits[j + k] = bitString.charAt(k) == '1';
            }
        }
    }

    public Bits(String bitString) {
        this.bits = new boolean[bitString.length()];
        for (int i = 0; i < bitString.length(); i++) {
            this.bits[i] = bitString.charAt(i) == '1';
        }
    }

    public int length() {
        return this.bits.length;
    }

    public boolean getBit(int id) {
        return this.bits[id];
    }

    public void setBit(int id, boolean value) {
        this.bits[id] = value;
    }

    public void negateBit(int id) {
        this.bits[id] = !this.bits[id];
    }

    public Bits copy() {
        return new Bits(this.bits);
    }

    private byte getByte(int byteId) {
        StringBuilder bitString = new StringBuilder();
        for (int bitId = byteId * 8; bitId < (byteId + 1) * 8; bitId++) {
            bitString.append(this.bits[bitId] ? "1" : "0");
        }
        return (byte) (Integer.parseInt(bitString.toString(), 2));
    }

    public byte[] toBytes() {
        byte[] bytes = new byte[this.bits.length / 8];
        for (int i = 0, j = 0; i < bytes.length; i++, j += 8) {
            bytes[i] = getByte(i);
        }
        return bytes;
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        for (boolean bit : this.bits) {
            res.append(bit ? "1" : "0");
        }
        return res.toString();
    }

}
