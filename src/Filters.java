import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Filters {
    public Pixel[][] filteredLow;
    public Pixel[][] filteredHigh;
    public byte[] bytes;
    public Pixel[][] quantified;
    TGA image;
    int k;

    public Filters(TGA image, int k) {
        this.image = image;
        this.k = k;
    }

    public Pixel filters(int x, int y, boolean high) {
        int[][] weights_low = {{1, 1, 1}, {1, 1, 1}, {1, 1, 1}};
        int[][] weights_high = {{0, -1, 0}, {-1, 5, -1}, {0, -1, 0}};

        int[][] weights = high ? weights_high : weights_low;

        Pixel pix = new Pixel();
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                int i1 = Math.max(i, 0);
                int j1 = Math.max(j, 0);

                int xi = Math.min(i1 + x, this.image.height - 1);
                int yj = Math.min(j1 + y, this.image.width - 1);
                pix = pix.add(this.image.getPixels()[xi][yj].mul(weights[i + 1][j + 1]));
            }
        }

        int weights_sum = 0;
        for (int[] row : weights) {
            for (int weight : row) {
                weights_sum += weight;
            }
        }
        if (weights_sum < 0) {
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
        res.append("0".repeat(code.length() - 1)).append(code);
        return res.toString();
    }

    public ArrayList<Integer> eliasDecode(String code) {
        ArrayList<Integer> codes = new ArrayList<>();
        int counter = 0;
        int index = 0;
        while (index < code.length()) {
            if (code.charAt(index) == '0') {
                counter++;
                index++;
            } else {
                codes.add(Integer.parseInt(code.substring(index, index + counter + 1), 2));
                index += counter + 1;
                counter = 0;
            }
        }
        return codes;
    }

    public Tuple encode() {
        Pixel[][] filtered_low = new Pixel[this.image.height][this.image.width];
        for (int i = 0; i < filtered_low.length; i++) {
            for (int j = 0; j < filtered_low[0].length; j++) {
                filtered_low[i][j] = filters(i, j, false);
            }
        }

        Pixel[][] filtered_high = new Pixel[this.image.height][this.image.width];
        for (int i = 0; i < filtered_high.length; i++) {
            for (int j = 0; j < filtered_high[0].length; j++) {
                filtered_high[i][j] = filters(i, j, true);
            }
        }

        byte[] byte_array = TGA.pixelsToBytes(filtered_low);
        byte_array = TGA.differentialCoding(byte_array);

        for (int i = 0; i < byte_array.length; i++) {
            byte x = byte_array[i];
            byte_array[i] = (byte) (x > 0 ? 2 * x : Math.abs(x) * 2 + 1);
        }


        StringBuilder bitStringBuilder = new StringBuilder();
        for (byte x : byte_array) {
            bitStringBuilder.append(eliasEncode(x));
        }

        String bitString = bitStringBuilder.toString();

        if (bitString.length() % 8 != 0) {
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
        int step = (int) (256 / Math.pow(2, this.k));
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

    byte[] decode() {
        byte[] bytes = TGA.pixelsToBytes(this.image.getPixels());
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(String.format("%8s", Integer.toBinaryString(b).replaceAll(" ", "0")));
        }

        ArrayList<Integer> codes = eliasDecode(builder.toString());
        byte[] diffs = new byte[codes.size()];
        for (int i = 0; i < diffs.length; i++) {
            int code = codes.get(i);
            diffs[i] = (byte) (code % 2 == 0 ? code / 2 : -(code / 2));
        }
        return TGA.differentialCoding(diffs);
    }

    public void stats(String statsTitle, Pixel[][] changed) {
        System.out.println(statsTitle);
        System.out.println("Blad sredniokwadaratowy ");
        double mse = mse(changed);
        System.out.println("\t(ogolny)\t" + mse);
        System.out.println("\t(czerwony)\t" + mse(changed, 0));
        System.out.println("\t(zielony)\t" + mse(changed, 1));
        System.out.println("\t(niebieski)\t" + mse(changed, 2));
        System.out.println("Stosunek sygnalu do szumu: " + snr(mse));
        System.out.println("\n");
    }

    public double mse(Pixel[][] changed) {
        double sum = 0;
        for (int i = 0; i < image.height; i++) {
            for (int j = 0; j < image.width; j++) {
                sum += euclidSquared(getPixelAsDoubleArray(image.getPixels()[i][j]), getPixelAsDoubleArray(changed[i][j]));
            }
        }
        return sum / (image.width * image.height);
    }

    public double mse(Pixel[][] changed, int color) {

        double sum = 0;
        for (int i = 0; i < image.height; i++) {
            for (int j = 0; j < image.width; j++) {
                int a,b;
                switch (color) {
                    case 0 -> {
                        a = image.getPixels()[i][j].red;
                        b = changed[i][j].red;
                    }
                    case 1 -> {
                        a = image.getPixels()[i][j].green;
                        b = changed[i][j].green;
                    }
                    case 2 -> {
                        a = image.getPixels()[i][j].blue;
                        b = changed[i][j].blue;
                    }
                    default -> {
                        a = 0;
                        b = 0;
                    }
                }
                sum += Math.pow(a - b, 2);
            }
        }
        return sum / (image.width * image.height);
    }


    public double snr(double MSE) {
        double sum = 0;
        for (Pixel[] row :
                image.getPixels()) {
            for (Pixel pixel :
                    row) {
                sum += Math.pow(pixel.red, 2) + Math.pow(pixel.green, 2) + Math.pow(pixel.blue, 2);
            }
        }
        return (sum / (image.width * image.height)) / MSE;
    }


    private double euclidSquared(Double[] a, Double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += Math.pow((a[i] - b[i]), 2);
        }
        return sum;
    }

    private Double[] getPixelAsDoubleArray(Pixel a) {
        Double[] x = new Double[3];
        x[0] = (double) a.red;
        x[1] = ((double) a.green);
        x[2] = ((double) a.blue);
        return x;
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
                    byte[] raw = Files.readAllBytes(Paths.get(args[1]));
                    if(args[0].equals("--encode")) {
                        TGA image = new TGA(raw);
                        Filters ft = new Filters(image, 2);
                        Tuple res = ft.encode();
                        byte[] low_bytes = TGA.pixelsToBytes(res.filtered_low);
                        byte[] high_bytes = TGA.pixelsToBytes(res.filtered_high);

                        ft.stats("Low", res.filtered_low);
                        ft.stats("High",res.filtered_high);

                        Files.write(Paths.get("filtered_low.tga"), image.replaceBody(low_bytes));
                        Files.write(Paths.get("filtered_high.tga"), image.replaceBody(high_bytes));
                        Files.write(Paths.get("encoded_low.tga"), image.replaceBody(res.b));
                        Files.write(Paths.get("encoded_high.tga"), image.replaceBody(res.quantified_bytes));
                    }
                    if(args[0].equals("--decode")) {
                        TGA image = new TGA(raw);
                        Filters ft = new Filters(image, 2);
                        byte[] bytes = ft.decode();
                        Files.write(Paths.get("low_decoded.tga"), image.replaceBody(bytes));
                    }

                }
            } catch (NumberFormatException | IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
