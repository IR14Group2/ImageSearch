import java.io.*;

/**
 * Parses files containing URLs.
 */
public class UrlParser {

  public UrlParser() {

  }

  public void parseUrlsFromFile(File file) {
    // Open file containing all URLs to parse
    try {
      BufferedReader in = new BufferedReader(new FileReader(file));
      String url = "";
      while((url = in.readLine()) != null) {
        System.out.println(url);
      }
    } catch (FileNotFoundException fnfe) {
      fnfe.printStackTrace();
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }
}
