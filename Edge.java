import java.io.Serializable;

public class Edge implements Serializable{
    private final String source;
    private final String destination;
    private final double similarity;
    private final double cost;

    public Edge(String source, String destination, double similarity) {
        this.source = source;
        this.destination = destination;
        this.similarity = similarity;
        this.cost = (1/similarity);
    }

    public String getSource() {
        return source;
    }

    public String getDestination() {
        return destination;
    }
    public double getSimilarity() {
        return similarity;
    }

    public double getCost() {
        return cost;
    }
}
