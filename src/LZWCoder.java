import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class LZWCoder {

    ArrayList<String> dict = new ArrayList<>();

    public LZWCoder(ArrayList<String> initialDict) {
        this.dict.addAll(initialDict);
    }

    public ArrayList<Integer> encode(String text) {
        ArrayList<Integer> res = new ArrayList<>();
        int indexSize = 9;
        while (!text.isEmpty()) {
            int maxLen = 0;
            int maxId = -1;
            for (int i = 0; i < this.dict.size(); i++) {
                String currentCode = this.dict.get(i);
                if (text.startsWith(currentCode)) {
                    if (currentCode.length() > maxLen) {
                        maxLen = currentCode.length();
                        maxId = i;
                    }
                }
            }
            res.add(maxId);

            if (text.length() > maxLen) {
                dict.add(text.substring(0, maxLen + 1));
                if (dict.size() == Math.pow(2, indexSize) - 1) {
                    indexSize++;
                    res.add(-1);
                }

            }
            text = text.substring(maxLen);
        }
        return res;
    }

    public String decode(String encodedBitString) {
        StringBuilder res = new StringBuilder();
        int indexSize = 9;
        String last = "";
        for (int i = 0; i < encodedBitString.length(); i += indexSize) {
            int current = Integer.parseInt(encodedBitString.substring(i, i + indexSize), 2);
            if (current < this.dict.size()) {
                String currentWord = this.dict.get(current);
                res.append(currentWord);
                if (i > 0) {
                    this.dict.add(last + currentWord.charAt(0));
                }
                last = currentWord;
            } else {
                String currentWord = last + last.substring(0, 1);
                res.append(currentWord);
                this.dict.add(currentWord);
                last = currentWord;
            }
            if (this.dict.size() == Math.pow(2, indexSize) - 1) {
                indexSize++;
            }
        }
        return res.toString();
    }

    static String encodedNumbersToBitString(ArrayList<Integer> encoded) {
        StringBuilder res = new StringBuilder();
        int indexSize = 9;
        for (int code : encoded) {
            if (code == -1) {
                indexSize++;
            } else {
                String codeBin = Integer.toBinaryString(code);
                codeBin = String.format("%" + indexSize + "s", codeBin).replaceAll(" ", "0");
                res.append(codeBin);
            }
        }
        res.append("11111111");
        res.append("0".repeat(8 - (res.length() % 8)));
        return res.toString();
    }

    static byte[] encodedBitStringToBytes(String encodedBits) {
        byte[] bytes = new byte[encodedBits.length() / 8];
        for (int i = 0; i < encodedBits.length(); i += 8) {
            int temp = Integer.parseInt(encodedBits.substring(i, i + 8), 2);
            bytes[i / 8] = (byte) (temp - 128);
        }
        return bytes;
    }

    static String encodedBytesToBitString(byte[] bytes) {
        StringBuilder text = new StringBuilder();
        for (byte aByte : bytes) {
            text.append(String.format("%8s", Integer.toBinaryString(aByte + 128)).replaceAll(" ", "0"));
        }
        while (text.charAt(text.length() - 1) == '0') {
            text.deleteCharAt(text.length() - 1);
        }
        text.delete(text.length() - 8, text.length());
        return text.toString();
    }

    static double getEncodedEntropy(ArrayList<Integer> encoded) {
        HashMap<Integer, Integer> occurrences = new HashMap<>();
        for (int code : encoded) {
            if (occurrences.containsKey(code)) {
                occurrences.put(code, occurrences.get(code) + 1);
            } else {
                occurrences.put(code, 1);
            }
        }

        double sizeLog = LZWCoder.log2(encoded.size());
        double entropy = 0.0;
        for (int count : occurrences.values()) {
            double countLog = LZWCoder.log2(count);
            entropy += count * (sizeLog - countLog);
        }
        entropy = entropy / encoded.size();
        return entropy;
    }

    static double getInputEntropy(String text) {
        HashMap<Character, Integer> occurrences = new HashMap<>();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (occurrences.containsKey(c)) {
                occurrences.put(c, occurrences.get(c) + 1);
            } else {
                occurrences.put(c, 1);
            }
        }

        double sizeLog = LZWCoder.log2(text.length());
        double entropy = 0.0;
        for (int count : occurrences.values()) {
            double countLog = LZWCoder.log2(count);
            entropy += count * (sizeLog - countLog);
        }
        entropy = entropy / text.length();
        return entropy;
    }

    static private double log2(double x) {
        return Math.log(x) / Math.log(2);
    }

    public String decode2(ArrayList<Integer> encoded) {
        StringBuilder res = new StringBuilder();
        String last = this.dict.get(encoded.get(0));
        res.append(last);
        for (int i = 1; i < encoded.size(); i++) {
            int code = encoded.get(i);
            if (code < this.dict.size()) {
                String current = this.dict.get(code);
                res.append(current);
                this.dict.add(last + current.substring(0, 1));
                last = current;
            } else {
                String current = last + last.substring(0, 1);
                res.append(current);
                this.dict.add(current);
                last = current;
            }
        }
        return res.toString();
    }

    public static void main(String[] args) {
        try {
            if (args.length < 4) {
                System.out.println("Użycie: --decode/--encode in_file out_file dictionary_file");
            } else {
                //build initial dictionary
                String dictionaryBase = Files.readString(Paths.get(args[3]));
                ArrayList<String> dictionary = new ArrayList<>();
                for (int i = 0; i < dictionaryBase.length(); i++) {
                    String current = String.valueOf(dictionaryBase.charAt(i));
                    if (!dictionary.contains(current)) {
                        dictionary.add(current);
                    }
                }

                if (args[0].equals("--encode")) {
                    //read in
                    String rawText = Files.readString(Paths.get(args[1]));

                    //encode
                    LZWCoder coder = new LZWCoder(dictionary);
                    ArrayList<Integer> encoded = coder.encode(rawText);
                    byte[] encodedBytes = LZWCoder.encodedBitStringToBytes(LZWCoder.encodedNumbersToBitString(encoded));

                    //write result
                    Files.write(Paths.get(args[2]), encodedBytes);

                    //print stats
                    System.out.println("Statystyki po zakodowaniu " + args[1] + ":");
                    System.out.println("\tDługość kodowanego pliku: " + rawText.length());
                    System.out.println("\tDługość uzyskanego kodu: " + encoded.size());
                    System.out.println("\tStopień kompresji: " + rawText.length() * 1.0 / encoded.size() + " (" + Math.round(10000 / (rawText.length() * 1.0 / encoded.size())) / 100.0 + "% początkowego rozmiaru).");
                    System.out.println("\tEntropia kodowanego tekstu: " + LZWCoder.getInputEntropy(rawText));
                    System.out.println("\tEntropia uzyskanego kodu: " + LZWCoder.getEncodedEntropy(encoded));
                } else if (args[0].equals("--decode")) {
                    //read in and convert to bitString
                    byte[] encodedBytes = Files.readAllBytes(Paths.get(args[1]));
                    String encodedBitString = LZWCoder.encodedBytesToBitString(encodedBytes);

                    //decode
                    LZWCoder coder = new LZWCoder(dictionary);
                    String decodedText = coder.decode(encodedBitString);

                    //write result
                    Files.writeString(Paths.get(args[2]), decodedText);

                    System.out.println("Zdekodowano " + args[1] + " do " + args[2]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
