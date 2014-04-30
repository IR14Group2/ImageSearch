/**
 * Created by mac on 2014-04-15.
 */
import java.awt.Image;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TreeSet;

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
  
  ////////////////////////////////////CHANGE THIS WHEN USING IT ON ANOTHER COMPUTER/////////////////////////////////////
  String imageMap = "C:/Users/Caroline/Documents/GitHub/ImageSearch/images";
  LowResImgProducer imgProducer;
  final int IMAGE_SIZE = 100;

  final static String PICTURE_URL = "ir_picture_url";
  final static String SITE_URL = "ir_site_url";
  final static String FILE_NAME = "id";
  final static String defaultImage = "default";
  final static String defaultImageURL = "http://www.drumwright.co.uk/media/product/pop/default.jpg";
  final static String request_handler = "/dismax";

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
    System.err.println("The query " + query);
    queryObject.setRequestHandler(request_handler);
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
  
  public SolrDocumentList search(String query, String boostingWords){
    String[] words = boostingWords.split(" ");
    StringBuilder sb = new StringBuilder();
    for (int i=0;i<words.length; i++) {
      sb.append(words[i]+ "^30 ");
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
    try {
      urls = new LinkedList<String>();
      for (SolrDocument doc : docs) {

        String url = (String) doc.getFieldValue(SITE_URL);
        urls.add(url);
      }
    } catch (Exception e) {
      e.printStackTrace();
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

  public String relevanceFeedback(TreeSet<SolrDocument> input) {
    // TODO make a complete list of accepted fields that should be used in relevance feedback. ID, for example, is not wanted
    String[] acceptedFields = { "features", "name", "caption" };
    StringBuilder query = new StringBuilder();
    for (SolrDocument doc : input) {
      // Collection<String> fields = doc.getFieldNames();
      for (int i = 0; i < acceptedFields.length; i++) {
        Object fieldVal = doc.getFieldValue(acceptedFields[i]);
        if(fieldVal == null){
          continue;
        }
        String value = null;
        try { //Try if value is a String, if not, try if it is arraylist
          value = (String) fieldVal;
        } catch (Exception e) {   
          
        }
        if(value == null){
          //Try if the field is an arraylist of strings instead of a String
          try{
          ArrayList<String> list = (ArrayList<String>) doc.getFieldValue(acceptedFields[i]);
          for (String s : list) {
            
            query.append(s.replaceAll(":", " "));
          }
          }catch(Exception e){
            System.err.println("The field " + acceptedFields[i] + " is neither String or Arraylist<String>, field not " +
                "used in relevance feedback");
          }
        }
        if (value != null) {
          //Need to replace ":" by " " because ":" means searching for a field.
          query.append(value);
        }
      }
    }
    return query.toString();
  }

}
