import java.io.Serializable;
import java.util.HashMap;

public class SiteRecord implements Serializable {
    private String url;
    private HashMap<String, Integer> wordFrequency;

    public SiteRecord(String url, HashMap<String, Integer> wordFrequency) {
        this.url = url;
        this.wordFrequency = wordFrequency;
    }

    public String getUrl() {
        return url;
    }

    public HashMap<String, Integer> getWordFrequency() {
        return wordFrequency;
    }
}
