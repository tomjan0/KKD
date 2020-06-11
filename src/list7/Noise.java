package list7;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Noise {

    private static Bits noise(Bits bits, double p) {
        Bits noised = bits.copy();
        for (int i = 0; i < bits.length(); i++) {
            if (Math.random() < p) {
                noised.negateBit(i);
            }
        }
        return noised;
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("użycie: prawdopodobieństwo plik_wejścowy plik_wyjściowy");
        } else {
            try {
                byte[] bytes = Files.readAllBytes(Paths.get(args[1]));
                Bits bits = new Bits(bytes);
                double p = Double.parseDouble(args[0]);

                System.out.println("Nakładanie szumu na '" + args[1] + "' z prawdopodobieństwem " + p + ".");
                System.out.println("\tZaszumiony plik zapisano do '" + args[2] + "'.");
                Bits noised = noise(bits, p);

                System.out.println(bits);
                System.out.println(noised);
                Files.write(Paths.get(args[2]), noised.toBytes());

            } catch (IOException e) {
                System.out.println("Nie znaleziono plików");
            } catch (NumberFormatException e) {
                System.out.println("Błędne prawdopobobieństwo");
            }
        }
    }
}
