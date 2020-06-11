package list7;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Checker {

    private static String[] splitToBlocks(String bitString) {
        String[] res = new String[bitString.length() / 4];
        for (int i = 0, j = 0; i < res.length; i++, j += 4) {
            res[i] = bitString.substring(j, j + 4);
        }
        return res;
    }

    private static int countDifferentBlocks(String[] blocks1, String[] blocks2) {
        int minLength = Math.min(blocks1.length, blocks2.length);
        int counter = 0;
        for (int i = 0; i < minLength; i++) {
            if (!blocks1[i].equals(blocks2[i])) {
                counter++;
            }
        }
        return counter;
    }

    private static void compare(String file1, String file2) throws IOException {
        System.out.println("Porównywanie '" + file1 + "' oraz '" + file2 + "':");
        String[] filenames = {file1, file2};

        byte[] bytes1 = Files.readAllBytes(Paths.get(file1));
        byte[] bytes2 = Files.readAllBytes(Paths.get(file2));
        String[] blocks1 = splitToBlocks(BytesUtils.bytesToBitString(bytes1));
        String[] blocks2 = splitToBlocks(BytesUtils.bytesToBitString(bytes2));

        int longerFile = blocks1.length > blocks2.length ? 0 : 1;
        if (blocks1.length != blocks2.length) {
            System.out.println("\tPliki różnej długości, '" + filenames[longerFile] + "' posiada o " + Math.abs(blocks1.length - blocks2.length) + " więcej bloków niż plik '" + filenames[(longerFile + 1) % 2] + "'.");
        }
        int differentBlocks = countDifferentBlocks(blocks1, blocks2);
        System.out.println("\tLiczba róznych bloków: " + differentBlocks + ".");
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("użycie: plik1 plik2");
        } else {
            try {
                compare(args[0], args[1]);
            } catch (IOException e) {
                System.out.println("Nie znaleziono plików");
            }
        }
    }
}
