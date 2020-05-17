import java.io.*;
import java.util.*;


public class DynamicHuffmanCoder {
    Node nyt;
    Node root;
    ArrayList<Node> nodes;
    Node[] seen;
    int counter;
    double summedCodesLengths;

    double getAvgCodeLength() {
        this.counter = 0;
        this.summedCodesLengths = 0;
        traverse(this.root);
        return this.summedCodesLengths / this.counter;

    }

    void traverse(Node node) {
        if (node.symbol > 0) {
            this.summedCodesLengths += Integer.toBinaryString(node.symbol).length();
            this.counter++;
        }
        if (node.left != null) {
            traverse(node.left);
        }
        if (node.right != null) {
            traverse(node.right);
        }
    }

    public DynamicHuffmanCoder() {
        this.nyt = new Node(Node.NYT);
        this.root = this.nyt;
        this.nodes = new ArrayList<>();
        this.seen = new Node[256];
    }

    String getCode(byte symbol, Node node, String code) {
        if (node.left == null && node.right == null) {
            return node.symbol == symbol ? code : "";
        } else {
            String temp = "";

            if (node.left != null) {
                temp = this.getCode(symbol, node.left, code + "0");
            }

            if (temp.isEmpty() && node.right != null) {
                temp = this.getCode(symbol, node.right, code + "1");
            }
            return temp;
        }
    }

    void insert(byte symbol) {
        Node node = this.seen[symbol];

        if (node == null) {
            Node spawn = new Node(symbol, 1);
            Node internal = new Node((byte) 0, 1, this.nyt.parent, this.nyt, spawn);

            spawn.parent = internal;
            this.nyt.parent = internal;

            if (internal.parent != null) {
                internal.parent.left = internal;
            } else {
                this.root = internal;
            }

            this.nodes.add(0, internal);
            this.nodes.add(0, spawn);

            this.seen[symbol] = spawn;
            node = internal.parent;
        }

        while (node != null) {
            Node largest = this.findLargestHuffmanNode(node.weight);

            if (node != largest && node != largest.parent && largest != node.parent) {
                this.swapHuffmanNodes(node, largest);
            }

            node.weight = node.weight + 1;
            node = node.parent;
        }
    }

    Node findLargestHuffmanNode(int weight) {
        Collections.reverse(this.nodes);
        for (Node node : this.nodes) {
            if (node.weight == weight) {
                Collections.reverse(this.nodes);
                return node;
            }
        }
        return null;
    }

    void swapHuffmanNodes(Node a, Node b) {
        Collections.swap(nodes, nodes.indexOf(a), nodes.indexOf(b));

        Node temp = a.parent;
        a.parent = b.parent;
        b.parent = temp;

        if (a.parent.left == b) {
            a.parent.left = a;
        } else {
            a.parent.right = a;
        }

        if (b.parent.left == a) {
            b.parent.left = b;
        } else {
            b.parent.right = b;
        }
    }

    String encode(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            if (this.seen[b] != null) {
                result.append(this.getCode(b, this.root, ""));
            } else {
                result.append(this.getCode(Node.NYT, this.root, ""));
                result.append(String.format("%8s", Integer.toBinaryString((char) b)).replaceAll(" ", "0"));
            }
            this.insert(b);
        }
        return result.toString();
    }

    String decode(String text) {
        StringBuilder result = new StringBuilder();
        byte symbol = Byte.parseByte(text.substring(0, 8), 2);
        result.append((char) symbol);
        this.insert(symbol);

        Node node = this.root;
        int i = 8;
        while (i < text.length()) {
            node = text.charAt(i) == '0' ? node.left : node.right;
            symbol = node.symbol;

            if (symbol != (byte) 0) {
                if (symbol == Node.NYT) {
                    if (i + 8 < text.length()) {
                        symbol = Byte.parseByte(text.substring(i + 1, i + 9), 2);
                    } else {
                        symbol = Byte.parseByte(text.substring(i + 1), 2);
                    }
                    i += 8;
                }
                result.append((char) symbol);
                this.insert(symbol);
                node = this.root;
            }
            i++;
        }
        return result.toString();
    }

    static byte[] getBytesFromEncodedBitString(String bitString) {
        ArrayList<Byte> bytes = new ArrayList<>();
        for (int i = 0; i < bitString.length(); i += 8) {
            int temp;
            if (i + 7 < bitString.length()) {
                temp = Integer.parseInt(bitString.substring(i, i + 8), 2);
                bytes.add((byte) (temp - 128));
            } else {
                //Appending ones to last part of bits to fill a byte, then adding an extra byte with that many ones at the start to make eight of them at all. Filling free space of byte with 0 if necessary
                StringBuilder beforeLastByte = new StringBuilder(bitString.substring(i));
                int size = beforeLastByte.length();
                StringBuilder lastByte = new StringBuilder();
                lastByte.append("1".repeat(Math.max(0, size)));
                for (int j = 0; j < 8 - size; j++) {
                    beforeLastByte.append("1");
                    lastByte.append("0");
                }

                bytes.add((byte) (Integer.parseInt(beforeLastByte.toString(), 2) - 128));
                bytes.add((byte) (Integer.parseInt(lastByte.toString(), 2) - 128));
            }

        }
        byte[] res = new byte[bytes.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = bytes.get(i);
        }
        return res;
    }

    static String getBitStringFromEncodedBytes(byte[] bytes) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            text.append(String.format("%8s", Integer.toBinaryString(bytes[i] + 128)).replaceAll(" ", "0"));
        }
        while (text.charAt(text.length() - 1) == '0') {
            text.deleteCharAt(text.length() - 1);
        }
        text.delete(text.length() - 8, text.length());
        return text.toString();
    }


    public static void main(String[] args) {
        try {
            if (args.length < 3) {
                System.out.println("Użycie: --decode/--encode in out");
            } else {
                if (args[0].equals("--encode")) {
                    InputStream inputStream = new FileInputStream(new File(args[1]));
                    byte[] bytes = inputStream.readAllBytes();
                    inputStream.close();

                    EntropyCalculator calc = new EntropyCalculator(args[1]);
                    DynamicHuffmanCoder coder = new DynamicHuffmanCoder();

                    String encoded = coder.encode(bytes);
                    byte[] encodedBytes = DynamicHuffmanCoder.getBytesFromEncodedBitString(encoded);

                    double entropy = calc.getEntropy();
                    double avg = (1.0 * encoded.length()) / bytes.length;
                    double ratio = (bytes.length * 8.0 / encoded.length());

                    System.out.println("Statystyki po zakodowaniu '" + args[1] + "' do '" + args[2] + "':");
                    System.out.println("\tEntropia: " + entropy + ",");
                    System.out.println("\tŚrednia długość kodu: " + avg + ",");
                    System.out.println("\tStopień kompresji: " + ratio + " (" + Math.round(10000/ratio) / 100.0 + "% początkowego rozmiaru).");


                    OutputStream out = new FileOutputStream(new File(args[2]));
                    out.write(encodedBytes);
                    out.close();
                } else if (args[0].equals("--decode")) {
                    InputStream inputStream = new FileInputStream(new File(args[1]));
                    byte[] decodeBytes = inputStream.readAllBytes();

                    DynamicHuffmanCoder coder = new DynamicHuffmanCoder();
                    String text = DynamicHuffmanCoder.getBitStringFromEncodedBytes(decodeBytes);
                    String decoded = coder.decode(text);

                    System.out.println("Odkodowano '" + args[1] + "' do '" + args[2] +"'.");

                    PrintWriter out = new PrintWriter(args[2]);
                    out.println(decoded);
                    out.close();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

