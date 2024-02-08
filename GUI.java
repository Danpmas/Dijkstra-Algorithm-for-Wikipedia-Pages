import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class GUI extends JFrame {
    private JTextField startUrlField;
    private JTextField endUrlField;
    private JTextArea resultArea;

    public GUI() {
        createView();
        setTitle("Shortest Path Finder");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1400, 600);
        setLocationRelativeTo(null); // center the window
    }

    // Dijkstra as map of maps
    public static List<String> dijkstra(Map<String, Map<String, Double>> graph, String start, String end) {
        Map<String, Double> distances = new HashMap<>(); //map shortest distance from starting vertex to every other vertex
        Map<String, String> predecessors = new HashMap<>(); //Track pred. of each vertex in path
        //to hold vertices
        PriorityQueue<String> queue = new PriorityQueue<>(Comparator.comparing(distances::get));
        Set<String> visited = new HashSet<>();

        for (String vertex : graph.keySet()) {
            distances.put(vertex, Double.POSITIVE_INFINITY); //set initial dist.'s as inf
        }
        distances.put(start, 0.0); //set dist. of start to itself is 0
        queue.add(start); //add starting vertex to queue

        while (!queue.isEmpty()) {
            String current = queue.poll(); //*retrieve and remove vertex w/ smallest distance*
            if (current.equals(end)) {
                break;
            }
            visited.add(current); //mark current vertex as visited

            //loop through neighbors of current vertex
            for (Map.Entry<String, Double> neighbor : graph.getOrDefault(current, Collections.emptyMap()).entrySet()) {
                if (!visited.contains(neighbor.getKey())) { //if not visited
                    double newDist = distances.get(current) + neighbor.getValue(); //calc dist. from this neigh. to curr. vertex
                    if (newDist < distances.get(neighbor.getKey())) {
                        distances.put(neighbor.getKey(), newDist); //update shortest dist.
                        predecessors.put(neighbor.getKey(), current); //set curr. vertex as pred.
                        queue.add(neighbor.getKey());
                    }
                }
            }
        }

        return buildPath(predecessors, start, end);
    }

    private static List<String> buildPath(Map<String, String> predecessors, String start, String end) {
        LinkedList<String> path = new LinkedList<>();
        for (String at = end; at != null; at = predecessors.get(at)) { //start loop at end vertex & move backwards
            path.addFirst(at); //add curr vertex to front of list
        }

        if (!path.isEmpty() && path.getFirst().equals(start)) { //check if path not empty and first elem is start vertex
            return path;
        } else {
            System.err.println("No path found between " + start + " and " + end);
            return Collections.emptyList(); // no path found
        }
    }

    private void createView() {
        JPanel panel = new JPanel();
        getContentPane().add(panel);

        panel.add(new JLabel("Start URL:"));
        startUrlField = new JTextField(25);
        panel.add(startUrlField);

        panel.add(new JLabel("End URL:"));
        endUrlField = new JTextField(25);
        panel.add(endUrlField);

        JButton findPathButton = new JButton("Find Shortest Path");
        findPathButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    findShortestPath();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        panel.add(findPathButton);

        resultArea = new JTextArea(10, 70);
        resultArea.setEditable(false);
        panel.add(new JScrollPane(resultArea));
        // Display Disjoint Sets
        displayDisjointSets();
    }
    private void displayDisjointSets() {
        try {
            Map<String, Map<String, Double>> graph = buildGraph("/Users/danmas/CSC365P3/edges.txt");
            Set<Set<String>> disjointSets = findDisjointSets(graph);
            resultArea.append("Disjoint Sets (" + disjointSets.size() + "):\n"); // Display the count of disjoint sets

            for (Set<String> set : disjointSets) {
                resultArea.append("Set Size: " + set.size() + " - "); // Display the size of each set
                resultArea.append(set + "\n");
            }
        } catch (IOException ex) {
            resultArea.append("Error loading graph: " + ex.getMessage());
        }
    }



    private void depthFirstSearchCollect(Map<String, Map<String, Double>> graph, String startVertex, Set<String> visited, Set<String> set) {
        visited.add(startVertex); //mark startV to stop func from revisiting same vertex (infinite loops)
        set.add(startVertex); //add all vertices connected to startVertex
        for (String neighbor : graph.getOrDefault(startVertex, Collections.emptyMap()).keySet()) { //loop over all neigh. of startV
            if (!visited.contains(neighbor)) { //check if neigh.  not visited
                depthFirstSearchCollect(graph, neighbor, visited, set); //recurse
            }
        }
    }

    private Set<Set<String>> findDisjointSets(Map<String, Map<String, Double>> graph) {
        Set<String> visited = new HashSet<>();
        Set<Set<String>> disjointSets = new HashSet<>();

        for (String vertex : graph.keySet()) {
            if (!visited.contains(vertex)) {
                Set<String> newSet = new HashSet<>();
                depthFirstSearchCollect(graph, vertex, visited, newSet);
                disjointSets.add(newSet);
            }
        }

        return disjointSets;
    }
    public static Map<String, Map<String, Double>> buildGraph(String filePath) throws IOException {
        Map<String, Map<String, Double>> graph = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" -> | \\[Cost: ");
                String source = parts[0];
                String destination = parts[1];
                double cost = Double.parseDouble(parts[2].replaceAll("]", ""));

                // Add edge from source to destination
                graph.computeIfAbsent(source, k -> new HashMap<>()).put(destination, cost);
                // ****Also add edge from destination to source****
                //graph.computeIfAbsent(destination, k -> new HashMap<>()).put(source, cost);
            }
        }

        return graph;
    }

    private double calculatePathCost(List<String> path, Map<String, Map<String, Double>> graph) {
        double totalCost = 0.0;
        for (int i = 0; i < path.size() - 1; i++) {
            String from = path.get(i);
            String to = path.get(i + 1);
            totalCost += graph.get(from).get(to);
        }
        return totalCost;
    }

    private void findShortestPath() throws IOException {
        String startUrl = startUrlField.getText();
        String endUrl = endUrlField.getText();
        Map<String, Map<String, Double>> graph = buildGraph("/Users/danmas/CSC365P3/edges.txt");

        List<String> shortestPath = dijkstra(graph, startUrl, endUrl);
        double shortestPathCost = calculatePathCost(shortestPath, graph);

        List<String> alternativePath = findAlternativePath(graph, shortestPath, startUrl, endUrl);
        double alternativePathCost = calculatePathCost(alternativePath, graph);

        String resultText = "Shortest path: " + shortestPath + " (Cost: " + shortestPathCost + ")";
        if (!alternativePath.isEmpty()) {
            resultText += "\nAlternative path: " + alternativePath + " (Cost: " + alternativePathCost + ")";
        } else {
            resultText += "\nNo alternative path found.";
        }
        resultArea.setText(resultText);
    }


    private List<String> findAlternativePath(Map<String, Map<String, Double>> graph, List<String> shortestPath, String start, String end) {
        if (shortestPath.size() <= 2) {
            return Collections.emptyList(); // No alternative if direct connection or no path
        }

        // Temporarily remove an edge from the shortest path and find another shortest path
        for (int i = 0; i < shortestPath.size() - 1; i++) {
            String from = shortestPath.get(i);
            String to = shortestPath.get(i + 1);

            // Remove edge
            Double cost = graph.get(from).remove(to);

            // Find new shortest path with this edge removed
            List<String> newShortestPath = dijkstra(graph, start, end);

            // Restore the edge
            graph.get(from).put(to, cost);

            if (!newShortestPath.equals(shortestPath) && !newShortestPath.isEmpty()) {
                return newShortestPath; // Found an alternative path
            }
        }

        return Collections.emptyList(); // No alternative path found
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new GUI().setVisible(true);
            }
        });
    }
}