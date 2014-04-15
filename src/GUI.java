import java.io.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Basic GUI for the ImageSearch application.
 */
public class GUI extends JFrame implements ActionListener {

  // GUI configurations
  private final String TITLE = "ImageSearch";
  private final int WIDTH = 400;
  private final int HEIGHT = 400;

  // URL parser
  UrlParser urlParser;

  // Index
  Index index;

  // GUI components
  JPanel jMainPanel;
  JMenuBar jMenuBar;
  JMenu jFileMenu;
  JMenuItem jIndexUrlMenuItem;
  JTextField jSearchField;
  JButton jSearchButton;

  /**
   * Constructor for the GUI.
   */
  public GUI() {
    urlParser = new UrlParser();
    index = new Index();
  }

  /**
   * Initialize the GUI, add all components and action listeners.
   */
  public void init() {

    jMainPanel = new JPanel();
    jMenuBar = new JMenuBar();
    jFileMenu = new JMenu("File");
    jIndexUrlMenuItem = new JMenuItem("Index URL ...");
    jSearchField = new JTextField(20);
    jSearchButton = new JButton("Search");

    // Set all action listeners
    setActionListeners();

    // Add GUI components to the main panale
    jFileMenu.add(jIndexUrlMenuItem);
    jMenuBar.add(jFileMenu);
    jMainPanel.add(jSearchField);
    jMainPanel.add(jSearchButton);
    add(jMainPanel);

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
    jIndexUrlMenuItem.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new File("."));
        int returnVal = fc.showOpenDialog(GUI.this);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
          File file = fc.getSelectedFile();
          urlParser.parseUrlsFromFile(file);
        }
      }
    });

    // Search button action listener
    jSearchButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        index.search(jSearchField.getText());
      }
    });
  }

  @Override
  public void actionPerformed(ActionEvent e) {

  }
}
