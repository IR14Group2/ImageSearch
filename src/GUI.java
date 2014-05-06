import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

/**
 * Basic GUI for the ImageSearch application.
 */
public class GUI extends JFrame implements ActionListener {

  // GUI configurations
  private final String TITLE = "ImageSearch";
  private final int WIDTH = 800;
  private final int HEIGHT = 710;
  final int IMAGE_SIZE = 100;
  private final int PICTURES_WIDTH = WIDTH / IMAGE_SIZE;
  private boolean doClustering = true;

  // Index
  Index index;

  // GUI components
  JPanel jMainPanel;
  JMenuBar jMenuBar;
  JMenu jFileMenu;
  JMenuItem jIndexUrlMenuItem;
  JTextField jSearchField;
  JButton jSearchButton;
  JPanel jresultPanel;
  JButton jrelevanceFeedbackButton;
  JPanel jResCluster;
  JPanel clusterPictures;

  TreeSet<SolrDocument> relevantDoc;

  UrlParser urlParser;

  /**
   * Constructor for the GUI.
   */
  public GUI() {
    index = new Index();
    urlParser = new UrlParser();
    relevantDoc = new TreeSet<SolrDocument>(new SolrCompare());

  }

  private class SolrCompare implements Comparator<SolrDocument> {

    @Override
    public int compare(SolrDocument o1, SolrDocument o2) {
      String id1 = (String) o1.getFieldValue("id");
      String id2 = (String) o2.getFieldValue("id");
      return id1.compareTo(id2);
    }

  }

  /**
   * Initialize the GUI, add all components and action listeners.
   */
  public void init() {
    jMainPanel = new JPanel(new BorderLayout());
    jMenuBar = new JMenuBar();
    jFileMenu = new JMenu("File");
    jIndexUrlMenuItem = new JMenuItem("Index URL ...");
    JPanel searchPanel = new JPanel();
    jSearchField = new JTextField(40);
    jSearchButton = new JButton("Search");
    jrelevanceFeedbackButton = new JButton("Give relevance feedback");
    jresultPanel = new JPanel(new GridLayout(0, PICTURES_WIDTH - 1));
    jResCluster = new JPanel(new BorderLayout());
    // in width

    // Set all action listeners
    setActionListeners();

    // Add GUI components to the main panel
    jFileMenu.add(jIndexUrlMenuItem);
    searchPanel.add(jSearchField);
    searchPanel.add(jSearchButton);

    jResCluster.add(jresultPanel, BorderLayout.SOUTH);

    jMenuBar.add(jFileMenu);
    jMainPanel.add(searchPanel, BorderLayout.PAGE_START);
    jMainPanel.add(jResCluster, BorderLayout.CENTER);
    jMainPanel.add(jrelevanceFeedbackButton, BorderLayout.PAGE_END);

    // Makes the main panel scrollable
    JScrollPane scrollpane = new JScrollPane(jMainPanel);
    add(scrollpane);

    // Configure basic GUI behaviour and display the GUI
    setTitle(TITLE);
    setSize(WIDTH, HEIGHT);
    setLocationRelativeTo(null);
    setJMenuBar(jMenuBar);
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setVisible(true);
  }

  /*
   * Add action listeners to search button, both for enter key and search
   * button. Add action listener to relevance feedback buttons. Make the image
   * clickable, redirecting to the website where the picture came from
   */
  private void setActionListeners() {
    // URL parsing action listener
    jIndexUrlMenuItem.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new File("."));
        int returnVal = fc.showOpenDialog(GUI.this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
          File file = fc.getSelectedFile();
          urlParser.parseUrlsFromFile(file);
        }
      }
    });

    // Search button action listener, added to both enter key and to the
    // search button

    Action searchAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doClustering = true;
        searchAndUpdateGUI(jSearchField.getText(), null);
      }
    };

    jSearchButton.addActionListener(searchAction);

    jSearchField.registerKeyboardAction(searchAction, "",
        KeyStroke.getKeyStroke("ENTER"), JComponent.WHEN_FOCUSED);

    jrelevanceFeedbackButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        doClustering = false;
        String expanded = index.relevanceFeedback(relevantDoc);
        searchAndUpdateGUI(expanded, jSearchField.getText());
      }
    });
  }

  private static void open(URI uri) {
    if (Desktop.isDesktopSupported()) {
      try {
        Desktop.getDesktop().browse(uri);
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else {
      System.err.println("Not possible to open browser");
    }
  }

  /*
   * Makes a search using the Index class, finds the pictures and information
   * from the documents returned and update the GUI with these pictures
   */

  public void searchAndUpdateGUI(String query, String boostedQueries) {
    long tic = System.currentTimeMillis();
    SolrDocumentList documents = null;
    if (boostedQueries != null) {
      documents = index.search(query, boostedQueries);
    } else {
      documents = index.search(query);
    }

    LinkedList<ImageIcon> img = index.getSavedImages(documents);

    LinkedList<String> info = index.getInformation(documents);

    LinkedList<String> urls = index.getURLs(documents);

    if (info == null || info.size() == 0) {
      System.err.println("No results found");
      jResCluster.add(new JTextField("No results found"),
          BorderLayout.NORTH);
      validate();
      repaint();
      return;
    }

    // Dont cluster on relevance feedback and if you clicked on a cluster
    // Clear resultPanel from previous results
    jResCluster.removeAll();
    jresultPanel.removeAll();
    if (clusterPictures != null) {
      clusterPictures.removeAll();
    }

    if (doClustering) {
      System.err.println("Do clustering");
      LinkedList<IndexCluster> clusters = index.getClusters(query);
      if (clusters.size() > 0) {
        clusterPictures = new JPanel(new GridLayout(0,
            PICTURES_WIDTH - 1));
        for (IndexCluster indexCluster : clusters) {
          // System.out.println("Category: " + indexCluster.label);
          JButton clusterButton = new JButton();
          clusterButton.setIcon(index.getImage(indexCluster.solrID
              .get(0)));
          clusterButton.setBackground(Color.WHITE);
          final LinkedList<String> docList = indexCluster.solrID;
          clusterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
              String query = "";

              for (String docID : docList) {
                query += "id:" + docID + " ";
              }
              doClustering = false; // Cannot do multiple
                          // clusterings
              searchAndUpdateGUI(query, null);
            }
          });

          JPanel clusterPanel = new JPanel(new GridBagLayout());
          GridBagConstraints c = new GridBagConstraints();
          c.gridx = 0;
          c.gridy = 0;
          clusterPanel.add(clusterButton, c);
          c.gridx = 0;
          c.gridy = 1;
          String clusterLabel = indexCluster.label;
          if (clusterLabel.length() > 24) {
            clusterPanel.add(new JTextField(clusterLabel
                .subSequence(0, 24).toString()), c);
            c.gridx = 0;
            c.gridy = 2;
            clusterPanel.add(new JTextField(clusterLabel
                .subSequence(24, clusterLabel.length())
                .toString()), c);
          } else {
            clusterPanel.add(new JTextField(clusterLabel), c);
          }

          clusterPictures.add(clusterPanel);
        }
        clusterPictures.setBorder(BorderFactory.createLineBorder(
            Color.YELLOW, 3));
        JTextField text = new JTextField("Groups");
        text.setBackground(Color.YELLOW);
        jResCluster.add(text, BorderLayout.NORTH);
        jResCluster.add(clusterPictures, BorderLayout.CENTER);
        jResCluster.add(jresultPanel, BorderLayout.SOUTH);
      }
      else{
        System.err.println("No clusters found");
        jResCluster.add(jresultPanel, BorderLayout.CENTER);
      }
    } else {
      jResCluster.add(jresultPanel, BorderLayout.CENTER);
    }

    JButton websiteButton;// This button has the picture on it
    JPanel imageAndFeedback;// this label contains both the button with the
                // image, and the feedback button

    // Add all pictures to the resultPanel
    for (int i = 0; i < img.size(); i++) {
      websiteButton = new JButton();
      websiteButton.setIcon(img.get(i));
      websiteButton.setSize(new Dimension(IMAGE_SIZE, IMAGE_SIZE));
      websiteButton
          .setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      websiteButton.setBackground(Color.WHITE);
      // Make a click to the website button opening the standard desktop
      // browser with the picture's site address
      URI uri0 = null;
      // Use defaultURL if url not set

      try {
        String url = urls.get(i);
        uri0 = (new URL(url).toURI());
      } catch (MalformedURLException e1) {
        e1.printStackTrace();
      } catch (URISyntaxException e1) {
        e1.printStackTrace();
      }
      final URI uri = uri0;
      websiteButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          open(uri);
        }
      });

      imageAndFeedback = new JPanel(new GridBagLayout());
      GridBagConstraints c = new GridBagConstraints();
      c.gridx = 0;
      c.gridy = 0;
      imageAndFeedback.add(websiteButton, c);

      JButton relevanceFeedbackButton = new JButton("Relevant");
      c.gridx = 0;
      c.gridy = 1;
      imageAndFeedback.add(relevanceFeedbackButton, c);

      // When relevance feedback button is clicked
      final SolrDocument doc = documents.get(i);
      relevanceFeedbackButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent arg0) {
          if (relevantDoc.contains(doc)) {// If the button has already
                          // been clicked, clicking
                          // again means removing it
            ((JButton) arg0.getSource())
                .setBackground(jSearchButton.getBackground());
            relevantDoc.remove(doc);
          } else {
            relevantDoc.add(doc);
            ((JButton) arg0.getSource()).setBackground(Color.RED);
          }

        }
      });

      jresultPanel.add(imageAndFeedback);

    }
    validate();
    repaint();
    System.err.println("Searched with " + documents.size()
        + " documents retrieved");
    System.err.println("total time " + (System.currentTimeMillis() - tic));
  }

  @Override
  public void actionPerformed(ActionEvent arg0) {
    // TODO Auto-generated method stub

  }
}
