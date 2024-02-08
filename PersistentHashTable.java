import java.util.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class PersistentHashTable implements Serializable {

    //mapping url to its block index and offset
    private Map<String, String> urlBlocks;
    private Map<String, List<Edge>> urlEdges;

    public PersistentHashTable() {
        urlBlocks = new HashMap<>();
        urlEdges = new HashMap<>();
    }

    public void put(String url, int blockIndex, int offset) {
        String blockAndOffset = blockIndex + "," + offset;
        urlBlocks.put(url, blockAndOffset);
    }

    public Set<String> getUrls() {
        return urlBlocks.keySet();
    }
    public void saveToFile(String filePath) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filePath))) {
            out.writeObject(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static PersistentHashTable loadFromFile(String filePath) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            return (PersistentHashTable) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
    public SiteRecord getRecord(String url) {
        String blockAndOffset = urlBlocks.get(url);
        if (blockAndOffset == null) {
            return null;
        }
        //Split string into array
        String[] parts = blockAndOffset.split(",");
        int blockIndex = Integer.parseInt(parts[0]);
        int offset = Integer.parseInt(parts[1]);
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream("block_" + blockIndex + ".dat"))) {
            //Read obj and cast to SiteRecord list
            List<SiteRecord> blockBuffer = (List<SiteRecord>) in.readObject();
            //specified offset
            return blockBuffer.get(offset);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void addEdge(Edge edge) {
        urlEdges.computeIfAbsent(edge.getSource(), k -> new ArrayList<>()).add(edge);
    }

    public List<Edge> getEdges(String url) {
        return urlEdges.getOrDefault(url, new ArrayList<>());
    }
}