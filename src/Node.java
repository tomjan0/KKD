public class Node {
    static byte NYT = -1;
    Node parent = null;
    Node left = null;
    Node right = null;
    int weight = 0;
    byte symbol;

    public Node(byte symbol) {
        this.symbol = symbol;
    }

    public Node(byte symbol, int weight) {
        this.symbol = symbol;
        this.weight = weight;
    }

    public Node(byte symbol, int weight, Node parent, Node left, Node right) {
        this.parent = parent;
        this.left = left;
        this.right = right;
        this.weight = weight;
        this.symbol = symbol;
    }
}

