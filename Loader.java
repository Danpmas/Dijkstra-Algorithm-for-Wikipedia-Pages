import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import java.io.*;
import java.util.*;
import java.util.stream.Stream;

public class Loader implements Serializable{

    //size of block to be written to disk
    private final int blockSize;

    //to store SiteRecord objects before writing them to disk
    private final List<SiteRecord> blockBuffer;

    //to keep track of the blocks written to disk
    private int blockIndex;

    static Map<String, Map<String, Integer>> wordFrequencyTable = new HashMap<>();

    private static final Set<String> stopWords = createStopWordsSet();

    public Loader(int blockSize) {
        this.blockSize = blockSize;
        this.blockBuffer = new ArrayList<>();
        this.blockIndex = 0;
    }

    private void writeEdgesToFile(SiteRecord node, List<SiteRecordSimilarity> similarNodes, String filePath, PersistentHashTable hashTable, Set<String> existingEdges) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) { //true to append data instead of overwrite
            for (SiteRecordSimilarity similarNode : similarNodes) {
                Edge edge = new Edge(node.getUrl(), similarNode.getRecord().getUrl(), similarNode.getSimilarity());
                String edgeInfo = edge.getSource() + " -> " + edge.getDestination() + " [Cost: " + edge.getCost() + "]";
                //check if edge already in file
                if (!existingEdges.contains(edgeInfo)) {
                    writer.write(edgeInfo + "\n");
                    hashTable.addEdge(edge);
                    existingEdges.add(edgeInfo); // Update the set of existing edges
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private Set<String> readExistingEdges(String filePath) throws IOException {
        Set<String> existingEdges = new HashSet<>();
        File file = new File(filePath);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    existingEdges.add(line.trim()); //trime white space
                }
            }
        }
        return existingEdges;
    }

    public void addSiteRecord(SiteRecord record, PersistentHashTable hashTable) {
        blockBuffer.add(record);
        //When buffer is full, writes the block to file
        if (blockBuffer.size() >= blockSize) {
            writeBlockToFile();
        }
        hashTable.put(record.getUrl(), blockIndex, blockBuffer.size() - 1);
    }

    public void constructGraph(List<SiteRecord> records, String edgesFilePath, PersistentHashTable hashTable) throws IOException {
        Set<String> existingEdges = readExistingEdges(edgesFilePath);
        for (int i = 0; i < records.size(); i++) {
            SiteRecord currentRecord = records.get(i);
            //Priority queue to store SiteRecordSimilarity, ordered based on their similarity score, in descending order (reversed)
            PriorityQueue<SiteRecordSimilarity> similarityQueue = new PriorityQueue<>(Comparator.comparingDouble(SiteRecordSimilarity::getSimilarity).reversed());

            // Calculate similarity with all other records
            for (int j = 0; j < records.size(); j++) {
                //make sure record not compared to itself
                if (i != j) {
                    SiteRecord otherRecord = records.get(j);
                    double similarity = calculateSimilarity(currentRecord.getWordFrequency(), otherRecord.getWordFrequency());
                    similarityQueue.add(new SiteRecordSimilarity(otherRecord, similarity));
                }
            }

            // Get top 4 similar records along with their similarity scores
            List<SiteRecordSimilarity> topSimilarRecords = new ArrayList<>();
            for (int k = 0; k < 4 && !similarityQueue.isEmpty(); k++) {
                topSimilarRecords.add(similarityQueue.poll());
            }

            // Write edges with similarity scores to file and add them to the hash table
            writeEdgesToFile(currentRecord, topSimilarRecords, edgesFilePath, hashTable, existingEdges);
        }
    }

    private static class SiteRecordSimilarity {
        private SiteRecord record;
        private double similarity;
        public SiteRecordSimilarity(SiteRecord record, double similarity) {
            this.record = record;
            this.similarity = similarity;
        }
        public SiteRecord getRecord() {
            return record;
        }
        public double getSimilarity() {
            return similarity;
        }
    }


    public static void main(String[] args) throws Exception {

        //Save data
        Loader loader = new Loader( 1005);
        PersistentHashTable hashTable = new PersistentHashTable();
        if (hashTable == null) {
            hashTable = new PersistentHashTable();
        }
        List<SiteRecord> allSiteRecords = new ArrayList<>();

        List<String> urls = loadUrlsFromFile("/Users/danmas/CSC365P3/WikiLinks.txt");

        for (String url : urls) {
            HashMap<String, Integer> wordFrequency = analyzeWebsite(url);
            SiteRecord siteRecord = new SiteRecord(url, wordFrequency);
            allSiteRecords.add(siteRecord);
            loader.addSiteRecord(siteRecord, hashTable);
        }

        // Flush any remaining records to disk
        loader.flush();

        // Save PersistentHashTable to disk
        hashTable.saveToFile("persistent_hashtable.dat");

        // Construct the graph
        loader.constructGraph(allSiteRecords, "edges.txt", hashTable);
    }

    //Read URLs from text file and returns them in list
    public static List<String> loadUrlsFromFile(String filePath) throws IOException {
        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            return stream.collect(Collectors.toList());
        }}

    public static HashMap<String, Integer> analyzeWebsite(String url) throws Exception {
        Document document = Jsoup.connect(url).get();

        Elements pTags = document.select("p");

        // Update the frequency table for the selected website
        Map<String, Integer> wordFrequency = new HashMap<>();

        for (Element pTag : pTags) {
            String text = pTag.text().toLowerCase();
            // Add spaces between words
            String[] words = text.split("\\s+");

            for (String word : words) {
                if (!stopWords.contains(word) && word.matches("^[a-zA-Z]*$")) {
                    wordFrequency.put(word, wordFrequency.getOrDefault(word, 0) + 1);
                }
            }
        }

        // Store word frequency in a persistent data structure
        wordFrequencyTable.put(url, wordFrequency);
        return (HashMap<String, Integer>) wordFrequency;
    }
    private static Set<String> createStopWordsSet() {
        Set<String> stopWords = new HashSet<>();
        // Adding prepositions and article words to this set
        stopWords.add("a");
        stopWords.add("the");
        stopWords.add("in");
        stopWords.add("at");
        stopWords.add("on");
        stopWords.add("of");
        stopWords.add("and");
        stopWords.add("to");
        stopWords.add("by");
        stopWords.add("have");
        stopWords.add("it");
        stopWords.add("for");
        stopWords.add("as");
        stopWords.add("or");
        stopWords.add("there");
        stopWords.add("what");
        stopWords.add("can");
        stopWords.add("use");
        stopWords.add("because");
        stopWords.add("most");
        stopWords.add("more");
        stopWords.add("be");
        stopWords.add("with");
        stopWords.add("may");
        stopWords.add("these");
        stopWords.add("is");
        return stopWords;
    }

    private double calculateSimilarity(Map<String, Integer> frequency1, Map<String, Integer> frequency2) {
        Set<String> word_Set1 = frequency1.keySet();
        Set<String> word_Set2 = frequency2.keySet();
        Set<Object> commonWords = new HashSet<>(word_Set1);
        //retain all words also found in set 2
        commonWords.retainAll(word_Set2);

        int totalWords1 = word_Set1.stream().mapToInt(frequency1::get).sum();
        int totalWords2 = word_Set2.stream().mapToInt(frequency2::get).sum();
        //For each word in commonWords set, calculates min frequency of that word between frequency1 and frequency2
        //Then calculates sum of all these minimum frequencies
        int totalCommonWords = commonWords.stream().mapToInt(word -> Math.min(frequency1.get((String) word), frequency2.get((String) word))).sum();
        return (double) totalCommonWords / Math.min(totalWords1, totalWords2);
    }

    private void writeBlockToFile() {
        try (ObjectOutputStream out = new ObjectOutputStream(
                new FileOutputStream("block_" + blockIndex + ".dat"))) {
            out.writeObject(blockBuffer);
            blockIndex++;
            blockBuffer.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Flush any remaining SiteRecords to disk
    public void flush() {
        if (!blockBuffer.isEmpty()) {
            writeBlockToFile();
            blockBuffer.clear();  // Clear the blockBuffer after writing it to file
        }
    }
}
