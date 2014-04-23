import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.apache.solr.common.SolrDocumentList;

/**
 * Basic GUI for the ImageSearch application.
 */
public class GUI extends JFrame implements ActionListener {

  // GUI configurations
  private final String TITLE = "ImageSearch";
  private final int WIDTH = 600;
  private final int HEIGHT = 600;
  private final int IMAGE_SIZE = 100;
  private final int PICTURES_WIDTH = WIDTH/IMAGE_SIZE;
  private final int MAX_PIC_WIDTH = 5;
  


  // Index
  Index index;

  // GUI components
  JPanel jMainPanel;
  JMenuBar jMenuBar;
  JMenu jFileMenu;
  JMenuItem jIndexUrlMenuItem;
  JTextField jSearchField;
  JButton jSearchButton;
  JPanel resultPanel;
//  JTextArea jResult;

  /**
   * Constructor for the GUI.
   */
  public GUI() {
    index = new Index();
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
    System.err.println(PICTURES_WIDTH);
    resultPanel = new JPanel(new GridLayout(0, Math.min(MAX_PIC_WIDTH, PICTURES_WIDTH)));// Don't want more than MAX_PIC_WIDTH pictures in width
//    jResult = new JTextArea();
    


    // Set all action listeners
    setActionListeners();

    // Add GUI components to the main panel
    jFileMenu.add(jIndexUrlMenuItem);
    searchPanel.add(jSearchField);
    searchPanel.add(jSearchButton);
    
    jMenuBar.add(jFileMenu);
    jMainPanel.add(searchPanel, BorderLayout.PAGE_START);
    jMainPanel.add(resultPanel, BorderLayout.LINE_START);
    
    //Makes the main panel scrollable
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


  private void setActionListeners() {
    // URL parsing action listener
//    jIndexUrlMenuItem.addActionListener(new ActionListener() {
//      @Override
//      public void actionPerformed(ActionEvent e) {
//        JFileChooser fc = new JFileChooser();
//        fc.setCurrentDirectory(new File("."));
//        int returnVal = fc.showOpenDialog(GUI.this);
//        if(returnVal == JFileChooser.APPROVE_OPTION) {
//          File file = fc.getSelectedFile();
//          urlParser.parseUrlsFromFile(file);
//        }
//      }
//    });

    // Search button action listener
    jSearchButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
//        jMainPanel.add(new TextArea("Searching"));
        SolrDocumentList documents = index.search(jSearchField.getText());
        LinkedList<ImageIcon> img = index.getImages(documents);//This might be better to do asynchronous
        LinkedList<String> info = index.getInformation(documents);
                
        //Clear resultPanel from previous results
        resultPanel.removeAll();
        
        JLabel jLabel;
        if(img == null){
          System.err.println("Cannot find images");
        }
        //Add all pictures to the resultPanel
        for (int i=0;i<img.size(); i++) {
      jLabel = new JLabel(info.get(i), index.resizeImage(img.get(i),IMAGE_SIZE,IMAGE_SIZE), JLabel.CENTER);
      jLabel.setHorizontalTextPosition(JLabel.CENTER);
      jLabel.setVerticalTextPosition(JLabel.BOTTOM);
          resultPanel.add( jLabel);
    }
        validate();
        repaint();

      }
    });
  }

  @Override
  public void actionPerformed(ActionEvent e) {

  }
}
