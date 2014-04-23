/**
 * Created by mac on 2014-04-15.
 */
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.LinkedList;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

public class Index {
  String solrURL = "http://localhost:8983/solr";
  SolrServer server;

  public Index() {
    server = new HttpSolrServer(solrURL);
  }
  
  /*
   * Connects to SolrServer and retrieves results as SolrDocumentList. 
   * Functions below can then be used to retrieve a list of images or a list of text associated with the image, using the
   * SolrDocumentList
   * To make this work, the solrServer has to be started
   */

  public SolrDocumentList search(String query) {
    SolrQuery queryObject = new SolrQuery();
    queryObject.setQuery(query);

    try {
      QueryResponse rsp = server.query(queryObject);
      SolrDocumentList docs = rsp.getResults();
      return docs;

    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
  
  public LinkedList<String> getInformation(SolrDocumentList docs){
    LinkedList<String> info = null;
    try {
      info = new LinkedList<String>();
      for (SolrDocument doc : docs) {
        String name = (String) doc.getFieldValue("name");
        info.add(name);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return info;
  }
  
  /*
   * Retrieves all the images from the documents in docs. For this to work, all resulting documents need a field called
   * "url" that is the url to the image. 
   * 
   * Change this function if the images should be retrieved from a saved place locally.
   */

  public LinkedList<ImageIcon> getImages(SolrDocumentList docs) {
    LinkedList<ImageIcon> pictures = null;
    try {
      pictures = new LinkedList<ImageIcon>();
      for (SolrDocument doc : docs) {
        String pictureURL = (String) doc.getFieldValue("url");
        URL urlobj = new URL(pictureURL);
        BufferedImage buffImage = ImageIO.read(urlobj);
        ImageIcon img = new ImageIcon(buffImage);
        pictures.add(img);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return pictures;

  }
  
  public ImageIcon resizeImage(ImageIcon icon, int width, int height){
    Image img = icon.getImage();  
    Image newimg = img.getScaledInstance(width, height,  java.awt.Image.SCALE_SMOOTH);  
    ImageIcon newIcon = new ImageIcon(newimg);
    return newIcon;
  }

//  public String connect(String query) throws Exception {
//    URL url = new URL(urlString + query);
//    URLConnection connection = url.openConnection();
//    BufferedReader in = new BufferedReader(new InputStreamReader(
//        connection.getInputStream()));
//    String inputLine;
//    StringBuffer returnString = new StringBuffer();
//    while ((inputLine = in.readLine()) != null) {
//      returnString.append(inputLine).append("\n");
//    }
//
//    in.close();
//    return returnString.toString();
//  }

  /*
   * public String getURL(String xmlString){ try { DocumentBuilderFactory
   * dbFactory = DocumentBuilderFactory.newInstance(); DocumentBuilder
   * dBuilder = dbFactory.newDocumentBuilder(); Document doc =
   * dBuilder.parse(xmlString); doc.getDocumentElement().normalize();
   * 
   * // System.out.println("root of xml file" +
   * doc.getDocumentElement().getNodeName()); NodeList nodes =
   * doc.getElementsByTagName("result"); //
   * System.out.println("==========================");
   * 
   * for (int i = 0; i < nodes.getLength(); i++) { Node node = nodes.item(i);
   * Element element = (Element) node;
   * 
   * } } catch (Exception ex) { ex.printStackTrace(); } }
   * 
   * private static String getValue(String tag, Element element) { NodeList
   * nodes = element.getElementsByTagName(tag).item(0).getChildNodes(); Node
   * node = (Node) nodes.item(0); return node.getNodeValue(); }
   */

}
