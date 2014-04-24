import java.io.*;
import java.net.*;

public class RandomWikiUrlFetcher {

  public RandomWikiUrlFetcher() {

  }

  public String[] fetchRandomUrls(int numUrls) {

    String[] urls = new String[numUrls];

    // URL to a random Wikipedia article
    String addr = "http://en.wikipedia.org/wiki/Special:Random";

    try{
      URL url = new URL(addr);

      for(int i = 0; i < numUrls; i++) {
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        String line = "";
        while((line = in.readLine()) != null) {
          int startIndex = line.indexOf("http://en.wikipedia.org/wiki/");
          int endIndex = 0;
          if(startIndex != -1) {
            endIndex = line.indexOf("\"", startIndex);
            urls[i] = line.substring(startIndex, endIndex);
            break; // found the URL to this page
          }
        }
        in.close();
      }
    } catch(Exception e) {
      System.out.println("Error when reading the random Wikipedia article URL: " + e);
    }

    return urls;
  }
}