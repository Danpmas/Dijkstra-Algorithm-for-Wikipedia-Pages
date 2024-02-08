import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Main {
    public static class WikipediaLinkExtractor {
        public static void extractLinks(List<String> initialUrls, String outputFile) {
            Set<String> allLinks = new HashSet<>();
            //Queue implemented as linked list
            Queue<String> urlsToProcess = new LinkedList<>(initialUrls);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) { //write extracted links to file
                //while still have urls & links less than 1005
                while (!urlsToProcess.isEmpty() && allLinks.size() < 1005) {
                    String currentUrl = urlsToProcess.poll(); //remove and return head of queue
                    try {
                        Document doc = Jsoup.connect(currentUrl).get();
                        Elements paragraphs = doc.select("p");

                        int linksCount = 0;
                        for (Element paragraph : paragraphs) {
                            if (allLinks.size() >= 1005) {
                                break;
                            }
                            //take hyper links
                            Elements links = paragraph.select("a[href]");
                            for (Element link : links) {
                                String linkHref = link.attr("abs:href");
                                if (isValidWikipediaLink(linkHref) && allLinks.add(linkHref)) {
                                    //write to file and add to queue
                                    writer.write(linkHref);
                                    writer.newLine();
                                    urlsToProcess.offer(linkHref);
                                    linksCount++;
                                    //first 10 links per page
                                    if (linksCount >= 10 || allLinks.size() >= 1005) {
                                        break;
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Error processing URL: " + currentUrl);
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                System.err.println("Error writing to file: " + outputFile);
                e.printStackTrace();
            }
        }

        private static boolean isValidWikipediaLink(String url) {
            return url.startsWith("https://en.wikipedia.org/wiki/") &&
                    !url.contains("#") &&
                    !url.matches(".*\\.(jpg|jpeg|png|gif|svg)$") &&
                    !url.matches(".*[0-9].*");
        }
    }

    public static void main(String[] args) {
        List<String> initialWikipediaUrls = Collections.singletonList("https://en.wikipedia.org/wiki/Computer_science");
        WikipediaLinkExtractor.extractLinks(initialWikipediaUrls, "wikiLinks.txt");
    }
}
