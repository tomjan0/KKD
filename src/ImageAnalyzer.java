import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;

public class ImageAnalyzer {

    private static double log2(double x) {
        return Math.log(x) / Math.log(2);
    }

    private static Pixel newPredictor(Pixel north, Pixel west, Pixel northWest) {
        int red = newPredictorColorCalculation(north.red, west.red, north.red);
        int green = newPredictorColorCalculation(north.green, west.green, northWest.green);
        int blue = newPredictorColorCalculation(north.blue, west.blue, northWest.blue);
        return new Pixel(red, green, blue);
    }

    private static int newPredictorColorCalculation(int cNorth, int cWest, int cNorthWest) {
        if (cNorthWest >= Math.max(cWest, cNorth)) {
            return Math.min(cWest, cNorth);
        } else if (cNorthWest <= Math.min(cWest, cNorth)) {
            return Math.max(cWest, cNorth);
        } else {
            return cWest + cNorth - cNorthWest;
        }
    }

    public static Pixel[][] bitmapToPixels(byte[] bitmap, int width, int height) {
        Pixel[][] pixels = new Pixel[height][width];
        int count = 0;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                pixels[i][j] = new Pixel(bitmap[count * 3 + 2], bitmap[count * 3 + 1], bitmap[count * 3]);
                count++;
            }
        }
        return pixels;
    }

    public static double entropy(Pixel[][] pixels, String mode) {
        HashMap<Integer, Integer> occurrences = new HashMap<>();
        for (Pixel[] row : pixels) {
            for (Pixel pixel : row) {
                switch (mode) {
                    case "red" -> occurrences.put(pixel.red, occurrences.getOrDefault(pixel.red, 0) + 1);
                    case "green" -> occurrences.put(pixel.green, occurrences.getOrDefault(pixel.green, 0) + 1);
                    case "blue" -> occurrences.put(pixel.blue, occurrences.getOrDefault(pixel.blue, 0) + 1);
                    case "all" -> {
                        occurrences.put(pixel.red, occurrences.getOrDefault(pixel.red, 0) + 1);
                        occurrences.put(pixel.green, occurrences.getOrDefault(pixel.green, 0) + 1);
                        occurrences.put(pixel.blue, occurrences.getOrDefault(pixel.blue, 0) + 1);
                    }
                }
            }
        }
        int size = pixels.length * pixels[0].length;
        if (mode.equals("all")) {
            size *= 3;
        }
        double[] result = {0};
        double sizeLog = log2(size);
        occurrences.forEach((key, value) -> result[0] += value * (sizeLog - log2(value)));
        return (result[0] / size);
    }

    public static Pixel[][] encode(Pixel[][] pixels, int predictor) {
        Pixel[][] encodedPixels = new Pixel[pixels.length][pixels[0].length];
        for (int i = 0; i < pixels.length; i++) {
            for (int j = 0; j < pixels[0].length; j++) {
                Pixel pixel = pixels[i][j];
                Pixel north, west, northWest;

                north = i == 0 ? new Pixel() : pixels[i - 1][j];
                west = j == 0 ? new Pixel() : pixels[i][j - 1];
                northWest = i == 0 || j == 0 ? new Pixel() : pixels[i - 1][j - 1];

                Pixel encodedPixel;
                switch (predictor) {
                    case 1 -> encodedPixel = pixel.sub(west).mod(256);
                    case 2 -> encodedPixel = pixel.sub(north).mod(256);
                    case 3 -> encodedPixel = pixel.sub(northWest).mod(256);
                    case 4 -> encodedPixel = pixel.sub(north.add(west).sub(northWest)).mod(256);
                    case 5 -> encodedPixel = pixel.sub(west.sub(northWest).div(2).add(north)).mod(256);
                    case 6 -> encodedPixel = pixel.sub(north.sub(northWest).div(2).add(west)).mod(256);
                    case 7 -> encodedPixel = pixel.sub(north.add(west).div(2)).mod(256);
                    case 8 -> encodedPixel = pixel.sub(newPredictor(north, west, northWest)).mod(256);
                    default -> throw new IllegalStateException("Unexpected value: " + predictor);
                }
                encodedPixels[i][j] = encodedPixel;
            }
        }
        return encodedPixels;
    }

    public static void analyze(String filename) {
        try {
            byte[] raw = Files.readAllBytes(Paths.get(filename));

            int width = raw[13] * 256 + ((raw[12] + 256) % 256);
            int height = raw[15] * 256 + ((raw[14] + 256) % 256);

            Pixel[][] pixels = bitmapToPixels(Arrays.copyOfRange(raw, 18, raw.length - 26), width, height);

            System.out.println("Entropie obrazu wejściowego: ");
            System.out.println("\tOgólna: " + entropy(pixels, "all"));
            System.out.println("\tKolor czerwony: " + entropy(pixels, "red"));
            System.out.println("\tKolor zielony: " + entropy(pixels, "green"));
            System.out.println("\tKolor niebieski: " + entropy(pixels, "blue"));
            System.out.println();

            double bestGeneral = 999;
            double bestRed = 999;
            double bestGreen = 999;
            double bestBlue = 999;

            int bestGeneralId = 0;
            int bestRedId = 0;
            int bestGreenId = 0;
            int bestBlueId = 0;

            for (int i = 1; i <= 8; i++) {
                Pixel[][] encoded = encode(pixels, i);

                double general = entropy(encoded, "all");
                double red = entropy(encoded, "red");
                double green = entropy(encoded, "green");
                double blue = entropy(encoded, "blue");

                System.out.println("Entropie dla predyktora " + i + ":");
                System.out.println("\tOgólna: " + general);
                System.out.println("\tKolor czerwony: " + red);
                System.out.println("\tKolor zielony: " + green);
                System.out.println("\tKolor niebieski: " + blue);
                System.out.println();

                if (general < bestGeneral) {
                    bestGeneral = general;
                    bestGeneralId = i;
                }
                if (red < bestRed) {
                    bestRed = red;
                    bestRedId = i;
                }
                if (green < bestGreen) {
                    bestGreen = green;
                    bestGreenId = i;
                }
                if (blue < bestBlue) {
                    bestBlue = blue;
                    bestBlueId = i;
                }
            }

            System.out.println("Najlepsze uzyskane entropie: ");
            System.out.println("\tOgólna: " + bestGeneral + " dla predyktora  " + bestGeneralId);
            System.out.println("\tKolor czerwony: " + bestGeneral + " dla predyktora " + bestRedId);
            System.out.println("\tKolor zielony: " + bestGreen + " dla predyktora  " + bestGreenId);
            System.out.println("\tKolor niebieski: " + bestGeneral + " dla predyktora  " + bestBlueId);
            System.out.println();

            System.out.println("Predyktory: ");
            System.out.println("\t1) Xb = W\n\t2) Xb = N\n\t3) Xb = NW\n\t4) Xb = N + W − NW\n\t5) Xb = N + (W − NW)/2\n\t6) Xb = W + (N − NW)/2\n\t7) Xb = (N + W)/2\n\t8) Nowy");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Należy podać ścieżkę pliku jako argument");
        } else {
            ImageAnalyzer.analyze(args[0]);
        }
    }
}
