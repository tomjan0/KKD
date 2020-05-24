import java.io.IOException;
import java.net.Proxy;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Quantizer {
    private final TGA image;
    private final Pixel[] codebook;
    private final Pixel[][] processedPixels;

    public Quantizer(byte[] raw, int colorsNo) {
        image = new TGA(raw);
        codebook = generateCodeBook(colorsNo);
        processedPixels = new Pixel[image.height][image.width];
    }

    public byte[] quantify() {
        for (int i = 0; i < image.height; i++) {
            for (int j = 0; j < image.width; j++) {
                Double[] diffs = new Double[codebook.length];
                for (int k = 0; k < codebook.length; k++) {
                    diffs[k] = euclidSquared(pixelAsVector(image.getPixels()[i][j]), pixelAsVector(codebook[k]));
                }
                processedPixels[i][j] = codebook[Arrays.asList(diffs).indexOf(Collections.min(Arrays.asList(diffs)))];
            }
        }
        return TGA.mergeTGA(image.getHeader(), TGA.pixelsToBytes(processedPixels), image.getFooter());
    }

    public double mse() {
        Pixel[][] original = image.getPixels();
        double acc = 0;
        for (int i = 0; i < original.length; i++) {
            for (int j = 0; j < original[0].length; j++) {
                double temp = euclidSquared(pixelAsVector(original[i][j]), pixelAsVector(this.processedPixels[i][j]));
                acc += temp * temp;
            }
        }
        return acc / (original.length * original[0].length);
    }

    public double snr(double mse) {
        Pixel[][] original = image.getPixels();
        double acc = 0;
        for (Pixel[] row : original) {
            for (Pixel pixel : row) {
                acc += pixel.red * pixel.red;
                acc += pixel.green * pixel.green;
                acc += pixel.blue * pixel.blue;
            }
        }
        return (acc / (original.length * original[0].length)) / mse;
    }

    private Pixel[] generateCodeBook(int codebookSize) {
        double eps = 0.00001;
        ArrayList<Double[]> codebook = new ArrayList<>();
        ArrayList<Double[]> data = pixelsToVectors(image.getPixels());
        Double[] c0 = averageVectorOfVectors(data);
        codebook.add(c0);

        double averageDistortion = averageDistortion(c0, data, data.size());

        while (codebook.size() < codebookSize) {
            Pair<ArrayList<Double[]>, Double> result = splitCodebook(data, codebook, eps, averageDistortion);
            codebook = result.first;
            averageDistortion = result.second;
        }
        return codebookToPixelArray(codebook);
    }

    private ArrayList<Double[]> pixelsToVectors(Pixel[][] pixels) {
        ArrayList<Double[]> vectors = new ArrayList<>();
        for (int i = 0; i < image.height; i++) {
            for (int j = 0; j < image.width; j++) {
                Pixel pixel = pixels[i][j];
                vectors.add(new Double[]{((double) pixel.red), ((double) pixel.green), ((double) pixel.blue)});
            }
        }
        return vectors;
    }

    private Pixel[] codebookToPixelArray(ArrayList<Double[]> vectors) {
        Pixel[] codebook = new Pixel[vectors.size()];
        for (int i = 0; i < vectors.size(); i++) {
            codebook[i] = new Pixel(vectors.get(i)[0].intValue(), vectors.get(i)[1].intValue(), vectors.get(i)[2].intValue());
        }
        return codebook;
    }

    private Double[] averageVectorOfVectors(ArrayList<Double[]> vectors) {
        int size = vectors.size();
        Double[] averageVector = {0.0, 0.0, 0.0};
        for (Double[] vector : vectors) {
            for (int i = 0; i < 3; i++) {
                averageVector[i] += vector[i] / size;
            }
        }
        return averageVector;
    }

    private double averageDistortion(Double[] vector0, ArrayList<Double[]> vectors, int size) {
        ArrayList<Double> vectorsEuclid = new ArrayList<>();
        vectorsEuclid.add(0.0);
        for (Double[] vector :
                vectors) {
            vectorsEuclid.add(euclidSquared(vector0, vector));
        }
        return vectorsEuclid.stream().reduce((s, d) -> s + d / size).get();
    }

    private double averageDistortion(ArrayList<Double[]> vectorsA, ArrayList<Double[]> vectorsB, int size) {
        ArrayList<Double> vectorsEuclid = new ArrayList<>();
        vectorsEuclid.add(0.0);
        for (int i = 0; i < vectorsA.size(); i++) {
            vectorsEuclid.add(euclidSquared(vectorsA.get(i), vectorsB.get(i)));
        }
        return vectorsEuclid.stream().reduce((s, d) -> s + d / size).get();
    }


    private double euclidSquared(Double[] a, Double[] b) {
        double acc = 0;
        for (int i = 0; i < a.length; i++) {
            acc += Math.pow((a[i] - b[i]), 2);
        }
        return acc;
    }


    private Pair<ArrayList<Double[]>, Double> splitCodebook(ArrayList<Double[]> data, ArrayList<Double[]> codebook, double eps, double initialAvgDist) {
        int dataSize = data.size();
        ArrayList<Double[]> newCodebook = new ArrayList<>();
        for (Double[] c : codebook) {
            newCodebook.add(newVector(c, eps));
            newCodebook.add(newVector(c, -eps));
        }
        codebook = newCodebook;

        double averageDistortion = 0.0;
        double err = eps + 1;
        while (err > eps) {
            ArrayList<Double[]> closest = new ArrayList<>(dataSize);
            for (int i = 0; i < dataSize; i++) {
                closest.add(null);
            }
            HashMap<Integer, ArrayList<Double[]>> nearestVectors = new HashMap<>();
            HashMap<Integer, ArrayList<Integer>> nearestVectorsIndexes = new HashMap<>();
            for (int i = 0; i < data.size(); i++) {
                double minDist = -1;
                int closestIndex = -1;
                for (int j = 0; j < codebook.size(); j++) {
                    double d = euclidSquared(data.get(i), codebook.get(j));
                    if (j == 0 || d < minDist) {
                        minDist = d;
                        closest.set(i, codebook.get(j));
                        closestIndex = j;
                    }
                }
                nearestVectors.putIfAbsent(closestIndex, new ArrayList<>());
                nearestVectorsIndexes.putIfAbsent(closestIndex, new ArrayList<>());

                nearestVectors.get(closestIndex).add(data.get(i));
                nearestVectorsIndexes.get(closestIndex).add(i);
            }

            for (int i = 0; i < codebook.size(); i++) {
                ArrayList<Double[]> nearestVectorsOfI = nearestVectors.get(i);
                if (nearestVectorsOfI.size() > 0) {
                    Double[] averageVector = averageVectorOfVectors(nearestVectorsOfI);
                    codebook.set(i, averageVector);
                    nearestVectorsIndexes.get(i).forEach(idx -> closest.set(idx, averageVector));
                }
            }

            double prevAvgDist = averageDistortion > 0.0 ? averageDistortion : initialAvgDist;
            averageDistortion = averageDistortion(closest, data, dataSize);

            err = (prevAvgDist - averageDistortion) / prevAvgDist;
        }
        return new Pair<>(codebook, averageDistortion);
    }

    private Double[] newVector(Double[] vector, double eps) {
        Double[] newVector = new Double[3];
        for (int i = 0; i < vector.length; i++) {
            newVector[i] = vector[i] * (1.0 + eps);
        }
        return newVector;
    }

    private Double[] pixelAsVector(Pixel a) {
        Double[] x = new Double[3];
        x[0] = (double) a.red;
        x[1] = ((double) a.green);
        x[2] = ((double) a.blue);
        return x;
    }

    static class Pair<U, V> {
        U first;
        V second;

        public Pair(U first, V second) {
            this.first = first;
            this.second = second;
        }
    }


    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Użycie: plik_wejściowy plik_wyjściowy potęga_kolorów");
        } else {
            try {
                int colorsNo = Integer.parseInt(args[2]);
                if (colorsNo < 0 || colorsNo > 24) {
                    System.out.println("Potęga kolorów powinna być z zakresu od 0 do 24");
                } else {
                    byte[] raw = Files.readAllBytes(Paths.get(args[0]));
                    Quantizer qt = new Quantizer(raw, (int) Math.pow(2, colorsNo));
                    byte[] result = qt.quantify();
                    double mse = qt.mse();
                    double snr = qt.snr(mse);

                    Files.write(Paths.get(args[1]), result);

                    System.out.println("Dokonano kwantyzacji " + args[0] + " ---> " + args[1]);
                    System.out.println("\tBłąd średniokwadratowy: " + mse);
                    System.out.println("\tStosunek sygnału do szumu: " + snr);
                }
            } catch (NumberFormatException | IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
