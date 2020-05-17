public class Pixel {
    int red;
    int green;
    int blue;

    public Pixel(int red, int green, int blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public Pixel() {
        this.red = 0;
        this.green = 0;
        this.blue = 0;
    }

    public Pixel add(Pixel pixel) {
        return new Pixel(
                this.red + pixel.red,
                this.green + pixel.green,
                this.blue + pixel.blue);
    }

    public Pixel sub(Pixel pixel) {
        return new Pixel(
                this.red - pixel.red,
                this.green - pixel.green,
                this.blue - pixel.blue);
    }

    public Pixel div(int n) {
        return new Pixel(
                this.red / n,
                this.green / n,
                this.blue / n);
    }

    public Pixel mod(int n) {
        return new Pixel(
                (this.red + n) % n,
                (this.green + n) % n,
                (this.blue + n) % n);
    }

    String str() {
        return "(" + this.red + "," + this.green + "," + this.blue + ")";
    }

}
