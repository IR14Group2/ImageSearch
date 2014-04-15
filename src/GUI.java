import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Basic GUI for the ImageSearch application.
 */
public class GUI extends JFrame implements ActionListener {

  private final String TITLE = "ImageSearch";
  private final int WIDTH = 400;
  private final int HEIGHT = 400;

  // GUI components
  JPanel jMainPanel;
  JTextField jSearchField;
  JButton jSearchButton;

  public GUI() {

  }

  public void init() {

    jMainPanel = new JPanel();
    jSearchField = new JTextField(20);
    jSearchButton = new JButton("Search");

    jSearchButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        String query = jSearchField.getText();
        System.out.println("Searching for " + query + " ...");
      }
    });

    // Add GUI components to the main panel
    jMainPanel.add(jSearchField);
    jMainPanel.add(jSearchButton);
    add(jMainPanel);

    // Configure basic GUI behaviour and display the GUI
    setTitle(TITLE);
    setSize(WIDTH, HEIGHT);
    setLocationRelativeTo(null);
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setVisible(true);
  }

  public void run() {

  }

  @Override
  public void actionPerformed(ActionEvent e) {

  }
}
