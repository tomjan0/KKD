import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Filters {
    TGA image;
    int k;

    public Filters(TGA image, int k) {
        this.image = image;
        this.k = k;
    }

    public Pixel filters(int x, int y, boolean high) {
        int[][] weights_low = {{1,1,1}, {1,1,1}, {1,1,1}};
        int[][] weights_high = {{0,-1,0}, {-1,5,-1}, {0,-1,0}};

        int[][] weights = high ? weights_high : weights_low;

        Pixel pix = new Pixel();
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                pix = pix.add(this.image.getPixels()[x+i][y+j].mul(weights[i+1][j+1]));
            }
        }

        int weights_sum = 0;
        for (int[] row: weights) {
            for (int weight: row) {
                weights_sum += weight;
            }
        }
        if(weights_sum < 0) {
            weights_sum = 1;
        }

        pix = pix.div(weights_sum);

        pix.red = Math.max(pix.red, 0);
        pix.green = Math.max(pix.green, 0);
        pix.blue = Math.max(pix.blue, 0);

        pix.red = Math.min(pix.red, 255);
        pix.green = Math.min(pix.green, 255);
        pix.blue = Math.min(pix.blue, 255);

        return pix;
    }

    public String eliasEncode(int n) {
        StringBuilder res = new StringBuilder();
        String code = Integer.toBinaryString(n);
        res.append("0".repeat(code.length() -1)).append(code);
        return res.toString();
    }

    public Tuple encode() {
        Pixel[][] filtered_low = new Pixel[this.image.height][this.image.width];
        for (int i = 0; i < filtered_low.length; i++) {
            for (int j = 0; j < filtered_low[0].length; j++) {
                filtered_low[i][j] = filters(i,j, false);
            }
        }

        Pixel[][] filtered_high = new Pixel[this.image.height][this.image.width];
        for (int i = 0; i < filtered_low.length; i++) {
            for (int j = 0; j < filtered_low[0].length; j++) {
                filtered_low[i][j] = filters(i,j, true);
            }
        }

        byte[] byte_array = TGA.pixelsToBytes(filtered_low);
        byte_array = TGA.differentialCoding(byte_array);

        for (int i = 0; i < byte_array.length; i++) {
            byte x = byte_array[i];
            byte_array[i] = (byte) (x > 0 ? 2*x : Math.abs(x) * 2 + 1);
        }



        StringBuilder bitStringBuilder = new StringBuilder();
        for (byte x: byte_array) {
            bitStringBuilder.append(eliasEncode(x));
        }

        String bitString = bitStringBuilder.toString();

        if(bitString.length() % 8 != 0) {
            bitString += "0".repeat(8 - (bitString.length() % 8));
        }

        byte[] b = bitStringToBytes(bitString);

        Pixel[][] quantified = quantify(filtered_high);
        byte[] quantified_bytes = TGA.pixelsToBytes(quantified);

        return new Tuple(filtered_low, filtered_high, b, quantified_bytes);

    }

    class Tuple {
        Pixel[][] filtered_low;
        Pixel[][] filtered_high;
        byte[] b;
        byte[] quantified_bytes;


        public Tuple(Pixel[][] filtered_low, Pixel[][] filtered_high, byte[] b, byte[] quantified_bytes) {
            this.filtered_low = filtered_low;
            this.filtered_high = filtered_high;
            this.b = b;
            this.quantified_bytes = quantified_bytes;
        }
    }

    Pixel[][] quantify(Pixel[][] pixels) {
        Pixel[][] quantifiedPixels = new Pixel[pixels.length][pixels[0].length];
        int step = (int) (256 / Math.pow(2,this.k));
        for (int i = 0; i < pixels.length; i++) {
            for (int j = 0; j < pixels[0].length; j++) {
                quantifiedPixels[i][j] = pixels[i][j].quantization(step);
            }
        }
        return quantifiedPixels;
    }

    byte[] bitStringToBytes(String bitString) {
        byte[] bytes = new byte[bitString.length() / 8];
        for (int i = 0; i < bitString.length(); i += 8) {
            int temp = Integer.parseInt(bitString.substring(i, i + 8), 2);
            bytes[i / 8] = (byte) temp;
        }
        return bytes;
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Użycie: --encode/--decode nazwa_pliku k");
        } else {
            try {
                int k = Integer.parseInt(args[2]);
                if (k < 1 || k > 7) {
                    System.out.println("k powinno być być z zakresu od 1 do 7");
                } else {
                    byte[] raw = Files.readAllBytes(Paths.get(args[0]));
                    TGA image = new TGA(raw);
                    Filters ft = new Filters(image,2);
                    Tuple res = ft.encode();
                    byte[] low_bytes = TGA.pixelsToBytes(res.filtered_low);
                    byte[] high_bytes = TGA.pixelsToBytes(res.filtered_high);

                    Files.write(Paths.get("filtered_low.tga"), image.replaceBody(low_bytes));
                    Files.write(Paths.get("filtered_high.tga"), image.replaceBody(high_bytes));
                    Files.write(Paths.get("encoded_low.tga"), image.replaceBody(res.b));
                    Files.write(Paths.get("encoded_high.tga"), image.replaceBody(res.quantified_bytes));


                }
            } catch (NumberFormatException | IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
