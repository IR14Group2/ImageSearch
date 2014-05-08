/**
 * Created by mac on 2014-04-15.
 */
import java.awt.Image;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TreeSet;

import javax.swing.ImageIcon;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Index {
  String solrURL = "http://localhost:8983/solr";
  SolrServer server;

  // //////////////////////////////////CHANGE THIS WHEN USING IT ON ANOTHER
  // COMPUTER/////////////////////////////////////
  String imageMap = "C:/Users/Caroline/Documents/GitHub/ImageSearch/images";
  LowResImgProducer imgProducer;
  final int IMAGE_SIZE = 100;

  final static String PICTURE_URL = "ir_picture_url";
  final static String SITE_URL = "ir_site_url";
  final static String FILE_NAME = "id";
  final static String defaultImage = "default";
  final static String defaultImageURL = "http://www.drumwright.co.uk/media/product/pop/default.jpg";
  final static String defaultURL = "http://google.com";
  final static String request_handler = "/dismax";
  final static int rowsRetrieved = 50;
  final static int CLUSTER_ROWS = 100;
  final static String[] ACCEPTED_REL_FEEDBACK_FIELDS = { "ir_img_title", "ir_header", "ir_alt",
    "ir_title", "ir_text1" };

  public Index() {
    server = new HttpSolrServer(solrURL);
    imgProducer = new LowResImgProducer(imageMap, IMAGE_SIZE, IMAGE_SIZE);
    // Add default image:
    if (!imgProducer.hasImg(defaultImage)) {
      imgProducer.saveImg(defaultImageURL, defaultImage, false);
    }
  }

  /*
   * Connects to SolrServer and retrieves results as SolrDocumentList.
   * Functions below can then be used to retrieve a list of images or a list
   * of text associated with the image, using the SolrDocumentList To make
   * this work, the solrServer has to be started
   */

  public SolrDocumentList search(String query) {
    SolrQuery queryObject = new SolrQuery();
    System.err.println("Seached for: " + query);
    queryObject.setRequestHandler(request_handler);
    queryObject.setQuery(query);
    queryObject.setRows(rowsRetrieved);
    // queryObject.setFacet(true);
    // queryObject.addFacetField("ir_header");

    try {
      QueryResponse rsp = server.query(queryObject);
      SolrDocumentList docs = rsp.getResults();

      // Test of Facetsearch
      // facetfield is the field to count different words in. fq means
      // filterquery, could be used when a special word is needed, found
      // from a facet

      // List<FacetField> facetFields = rsp.getFacetFields();
      // List<Count> counts = rsp.getFacetField("ir_header").getValues();
      // for (Count c : counts) {
      // System.out.println(c.getName() + " " + c.getCount());
      // //
      // }
      return docs;

    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public SolrDocumentList search(String query, String boostingWords) {
    String[] words = boostingWords.split(" ");
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < words.length; i++) {
      sb.append(words[i] + "^30 ");
    }
    return search(query + " " + sb.toString());
  }

  public LinkedList<String> getInformation(SolrDocumentList docs) {
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
   * Retrieves all the images from the documents in docs. First see if picture
   * have been stored, and then retrieve it, or save from URL and then
   * retrieve it. For this to work, all resulting documents need a field
   * PICTURE_URL that is the url to the image.
   */
  public LinkedList<ImageIcon> getSavedImages(SolrDocumentList docs) {

    LinkedList<ImageIcon> pictures = null;
    // long time=0;
    pictures = new LinkedList<ImageIcon>();
    for (SolrDocument doc : docs) {
      String pictureURL = (String) doc.getFieldValue(PICTURE_URL);
      // long tic = System.currentTimeMillis();
      String fileName = (String) doc.getFieldValue(FILE_NAME);
      ImageIcon pic = null;
      if (imgProducer.hasImg(fileName)) {
        pic = imgProducer.getImg(fileName);
      } else {
        pic = imgProducer.saveImg(pictureURL, fileName, true);
      }
      // time += (System.currentTimeMillis()-tic);
      if (pic == null) {
        pictures.add(imgProducer.getImg(defaultImage)); // Add default
                                // image if link
                                // doesn't work
        System.err.println("Warning, this picture is null!");
      } else {
        pictures.add(pic);
      }

    }

    // System.err.println("total time to get pic " + time);
    return pictures;
  }

  public LinkedList<String> getURLs(SolrDocumentList docs) {
    LinkedList<String> urls = null;

    urls = new LinkedList<String>();
    for (SolrDocument doc : docs) {
      try {
        String url = (String) doc.getFieldValue(SITE_URL);
        urls.add(url);
      } catch (Exception e) {
        System.err.println("Could not find URL for document");
        urls.add(defaultURL);
      }
    }
    return urls;

  }

  public ImageIcon resizeImage(ImageIcon icon, int width, int height) {
    Image img = icon.getImage();
    Image newimg = img.getScaledInstance(width, height,
        java.awt.Image.SCALE_SMOOTH);
    ImageIcon newIcon = new ImageIcon(newimg);
    return newIcon;
  }

  public ImageIcon getImage(String fileName) {
    if (imgProducer.hasImg(fileName)) {
      return imgProducer.getImg(fileName);
    }
    return null;

  }

  public String relevanceFeedback(TreeSet<SolrDocument> input) {
    // TODO make a complete list of accepted fields that should be used in
    // relevance feedback. ID, for example, is not wanted

    StringBuilder query = new StringBuilder();
    for (SolrDocument doc : input) {
      // Collection<String> fields = doc.getFieldNames();
      for (int i = 0; i < ACCEPTED_REL_FEEDBACK_FIELDS.length; i++) {
        Object fieldVal = doc.getFieldValue(ACCEPTED_REL_FEEDBACK_FIELDS[i]);
        if (fieldVal == null) {
          continue;
        }
        String value = null;
        try { // Try if value is a String, if not, try if it is
            // arraylist
          value = (String) fieldVal;
        } catch (Exception e) {

        }
        if (value == null) {
          // Try if the field is an arraylist of strings instead of a
          // String
          try {
            ArrayList<String> list = (ArrayList<String>) doc
                .getFieldValue(ACCEPTED_REL_FEEDBACK_FIELDS[i]);
            for (String s : list) {

              query.append(s.replaceAll(":", " "));
            }
          } catch (Exception e) {
            System.err
                .println("The field "
                    + ACCEPTED_REL_FEEDBACK_FIELDS[i]
                    + " is neither String or Arraylist<String>, field not "
                    + "used in relevance feedback");
          }
        }
        if (value != null) {
          // Need to replace ":" by " " because ":" means searching
          // for a field.
          query.append(value);
        }
      }
    }
    return query.toString();
  }

  public LinkedList<IndexCluster> getClusters(String query) {
    LinkedList<IndexCluster> clusterList = new LinkedList<IndexCluster>();
    ;
    try {
      // URL obj = new URL(solrURL + "/collection1/dismax?q=" + query
      // + "&wt=xml&rows=100");
      String queryEnc = URLEncoder.encode(query, "UTF-8");
      URI uri = new URI("http", null, "localhost", 8983,
          "/solr/collection1" + request_handler, "q=" + queryEnc
              + "&wt=xml&" + "rows=" + CLUSTER_ROWS, null);
      HttpURLConnection con = (HttpURLConnection) (uri.toURL())
          .openConnection();
      con.setRequestMethod("GET");

      // Parse the XML given as result
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory
          .newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document doc = dBuilder.parse(con.getInputStream());
      doc.getDocumentElement().normalize();
      Node response = doc.getElementsByTagName("response").item(0);
      NodeList children = response.getChildNodes();

      Node clusters = children.item(3);// "clusters" node
      NodeList clusterNodes = clusters.getChildNodes();// all its elements

      for (int i = 0; i < clusterNodes.getLength(); i++) {
        NodeList info = clusterNodes.item(i).getChildNodes(); // All the
                                    // cluster's
                                    // info
        Node labels = info.item(0);
        if (labels.getTextContent().equals("Other Topics")) {
          break;
        }
        NodeList docs = info.item(2).getChildNodes();
        IndexCluster cl = new IndexCluster();
        cl.label = labels.getTextContent();
        for (int j = 0; j < docs.getLength(); j++) {
          cl.solrID.add(docs.item(j).getChildNodes().item(0)
              .getTextContent());
        }
        clusterList.add(cl);
      }
      con.getInputStream().close();
    } catch (Exception e) {

      System.err
          .println("Could not cluster. Error in getClusters(query) in Index:");
      System.err.println(e.getMessage());
    }
    return clusterList;
  }

}

class IndexCluster {
  String label;
  LinkedList<String> solrID = new LinkedList<String>();
}
