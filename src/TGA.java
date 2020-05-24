import java.util.ArrayList;
import java.util.Arrays;

public class TGA {
    private final byte[] header;
    private final byte[] footer;
    private final Pixel[][] pixels;
    protected int width;
    protected int height;

    public TGA(byte [] raw) {
        this.header = Arrays.copyOfRange(raw, 0, 18);
        this.footer = Arrays.copyOfRange(raw, raw.length - 26, raw.length);

        this.width = raw[13] * 256 + (raw[12] & 0xFF);
        this.height = raw[15] * 256 + (raw[14] & 0xFF);
        this.pixels = TGA.bitmapToPixels(Arrays.copyOfRange(raw, 18, raw.length - 26), width, height);

    }

    public static Pixel[][] bitmapToPixels(byte[] bitmapBytes, int width, int height) {
        Pixel[][] pixels = new Pixel[height][width];
        int count = 0;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                pixels[i][j] = new Pixel(bitmapBytes[count * 3 + 2] & 0xff, bitmapBytes[count * 3 + 1] & 0xff, bitmapBytes[count * 3] & 0xff);
                count++;
            }
        }
        return pixels;
    }

    public void print() {
        for (Pixel[] row : this.pixels) {
            for (Pixel pixel : row) {
                System.out.println(pixel + " ");
            }
            System.out.println();
        }
    }

    public byte[] getHeader() {
        return header;
    }

    public byte[] getFooter() {
        return footer;
    }

    public Pixel[][] getPixels() {
        return pixels;
    }


    public static byte[] pixelsToBytes(Pixel[][] pixels) {
        ArrayList<Byte> res = new ArrayList<>();
        for (Pixel[] row: pixels) {
            for (Pixel pixel: row) {
                res.add((byte) (pixel.blue ));
                res.add((byte) (pixel.green ));
                res.add((byte) (pixel.red ));
            }
        }
        byte[] bytes = new byte[res.size()];
        for (int i = 0; i < res.size(); i++) {
            bytes[i] = res.get(i);
        }
        return bytes;
    }

    public static byte[] mergeTGA(byte[] header, byte[] body, byte[] footer) {
        byte[] bytes = new byte[body.length + header.length + footer.length];
        System.arraycopy(header, 0, bytes, 0, header.length);
        System.arraycopy(body, 0, bytes, header.length, body.length);
        System.arraycopy(footer, 0, bytes, header.length+body.length, footer.length);
        return bytes;
    }
}
