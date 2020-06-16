package list7;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Encode {

    private static String parity(String bits, int[] indices) {
        StringBuilder res = new StringBuilder();

        for (int i : indices) {
            res.append(bits.charAt(i));
        }
        int ones = 0;
        for (int i = 0; i < res.length(); i++) {
            if (res.charAt(i) == '1') {
                ones++;
            }
        }
        return String.valueOf(ones % 2);
    }


    private static String hamming(String bits) {
        String p1 = parity(bits, new int[]{0, 1, 3});
        String p2 = parity(bits, new int[]{0, 2, 3});
        String p3 = parity(bits, new int[]{1, 2, 3});
        String p = parity(p1 + p2 + bits.charAt(0) + p3 + bits.substring(1), new int[]{0, 1, 2, 3, 4, 5, 6});

        return p1 + p2 + bits.charAt(0) + p3 + bits.substring(1) + p;
    }

    private static String encode(String bits) {
        StringBuilder encoded = new StringBuilder();

        for (int i = 0; i < bits.length(); i += 4) {
            String block = bits.substring(i, i+4);
            encoded.append(hamming(block));
        }
        return encoded.toString();
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("użycie: plik_wejściowy plik_wyjściowy");
        } else {
            try {
                byte[] bytes = Files.readAllBytes(Paths.get(args[0]));
                Bits bits = new Bits(bytes);
                String encoded = encode(bits.toString());
                Files.write(Paths.get(args[1]), new Bits(encoded).toBytes());

                System.out.println("Zakodowano '" + args[0] + "' do '" + args[1] + "'.");
            } catch (IOException e) {
                System.out.println("Nie znaleziono plików");
            }
        }
    }
}
