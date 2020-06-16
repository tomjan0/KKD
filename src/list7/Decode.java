package list7;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public class Decode {
    static String[] codes = {
            "00000000",
            "11010010",
            "01010101",
            "10000111",
            "10011001",
            "01001011",
            "11001100",
            "00011110",
            "11100001",
            "00110011",
            "10110100",
            "01100110",
            "01111000",
            "10101010",
            "00101101",
            "11111111",
    };
    static int errors = 0;

    private static String fromHamming(String bits) {
        for (String code : codes) {
            int diffs = 0;
            char[] bitsArr = bits.toCharArray();
            char[] codeArr = code.toCharArray();

            for (int i = 0; i < bitsArr.length; i++) {
                if (bitsArr[i] != codeArr[i]) {
                    diffs++;
                }
            }

            switch (diffs) {
                case 0 -> {
                    return "" + bitsArr[2] + bitsArr[4] + bitsArr[5] + bitsArr[6];
                }
                case 1 -> {
                    return "" + codeArr[2] + codeArr[4] + codeArr[5] + codeArr[6];
                }
                case 2 -> {
                    errors++;
                    return null;
                }
            }
        }
        return null;
    }

    private static String decode(String bits) {
        StringBuilder decoded = new StringBuilder();

        for (int i = 0; i < bits.length(); i += 8) {
            String block = bits.substring(i, i+8);
            decoded.append(Objects.requireNonNullElse(fromHamming(block), "0000"));
        }

        System.out.println("\tLiczba bloków z 2 błędami: " + errors + ".");
        return decoded.toString();
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("użycie: plik_wejściowy plik_wyjściowy");
        } else {
            try {
                byte[] bytes = Files.readAllBytes(Paths.get(args[0]));
                Bits bits = new Bits(bytes);

                System.out.println("Dekodowanie '" + args[0] + "':");
                String decoded = decode(bits.toString());
                Files.write(Paths.get(args[1]), new Bits(decoded).toBytes());
                System.out.println("\tZdekodowano do '" + args[1] + "'.");
            } catch (IOException e) {
                System.out.println("Nie znaleziono plików");
            }
        }
    }
}
