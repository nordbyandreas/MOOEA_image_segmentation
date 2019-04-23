import java.awt.*;
import java.util.ArrayList;

/**
 * Created by Andreas on 22.02.2019.
 */
public class Edge implements Comparable<Edge>{
    public int from, to;
    public double weight;
    public ArrayList<Color> pixels;

    public Edge() {
        this.from = -1;
        this.to = -1;
        this.weight = Double.POSITIVE_INFINITY;

    }

    public Edge(int from, int to, ArrayList<Color> pixels) {
        this.from = from;
        this.to = to;
        this.pixels = pixels;
        this.weight = Genotype.RGBDistance(this.pixels.get(from), this.pixels.get(to));
    }

    @Override
    public int compareTo(Edge o) {
        if (this.weight > o.weight) return 1;
        if (this.weight < o.weight) return -1;
        return 0;
    }

    @Override
    public String toString() {
        return "(" + from + " " + to + ") " + weight;
    }
}
