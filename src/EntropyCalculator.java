import java.io.*;
import java.util.HashMap;

public class EntropyCalculator {
    private byte[] bytes;
    private double entropy;
    private double condEntropy;
    private final HashMap<Byte, Integer> countMap = new HashMap<>();
    private final HashMap<String, Integer> countMapCond = new HashMap<>();

    public EntropyCalculator(String filename) {
        try {
            InputStream input = new FileInputStream(new File(filename));
            this.bytes = input.readAllBytes();
            this.countOccurrences();
            this.calculateEntropies();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void countOccurrences() {
        byte last = 0;
        for (byte b : bytes) {
            this.countMap.put(b, this.countMap.getOrDefault(b, 0) + 1);
            String pair = b + " " + last;
            this.countMapCond.put(pair, this.countMapCond.getOrDefault(pair, 0) + 1);
            last = b;
        }
    }

    private void calculateEntropies() {
        double[] entropies = {0, 0};
        double sizeLog = log2(this.bytes.length);
        int[] counter = {0};
        this.countMap.forEach((b, count) -> {
            double countLog = log2(count);
            entropies[0] += count * (sizeLog - countLog);
            this.countMapCond.forEach((pair, pairCount) -> {
                if (pair.split(" ")[0].equals(b.toString())) {
                    counter[0]++;
                    entropies[1] += pairCount * (countLog - log2(pairCount));
                }
            });
        });
        this.entropy = entropies[0] / this.bytes.length;
        this.condEntropy = entropies[1] / this.bytes.length;
    }

    private double log2(double x) {
        return Math.log(x) / Math.log(2);
    }

    public double getEntropy() {
        return this.entropy;
    }

    public double getCondEntropy() {
        return this.condEntropy;
    }

    public static void main(String[] args) {
        String[] filenames = {"pan-tadeusz-czyli-ostatni-zajazd-na-litwie.txt", "test1.bin", "test2.bin", "test3.bin"};
        for (String filename : filenames) {
            EntropyCalculator calc = new EntropyCalculator(filename);
            System.out.println("Wynik dla " + filename + ":");
            System.out.println("\tEntropia: " + calc.getEntropy());
            System.out.println("\tEntropia warunkowa: " + calc.getCondEntropy());
            System.out.println("\tRóżnica: " + (calc.getEntropy() - calc.getCondEntropy()));
            System.out.println();
        }

    }
}
