/**
 * Created by andershuss on 2014-05-09.
 */
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import javax.imageio.ImageIO;
import javax.swing.*;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.Hash;


//TODO:
/*
- load all files from drive
- fix application that transforms .evl data to diagrams prec recall
 */

/**
 * Basic GUI for the ImageSearch application.
 */
public class EvaluationGUI extends JFrame implements ActionListener {

    // GUI configurations
    private final String TITLE = "ImageSearch";
    private final int WIDTH = 800;
    private final int HEIGHT = 710;
    final int IMAGE_SIZE = 100;
    //5 per row for easier keeping track of ranking:
    private final int PICTURES_PER_ROW = 5;
    private boolean allowClustering = false; //way to turn of all clustering.
    private boolean doClustering = false;

    private SolrDocumentList resultDocuments;


    ///////////////  EVALUATION  //////////////
    private String userName; //"id" or name of the user.
    private ArrayList<JCheckBox>[][] relevanceCheckBoxes;
    private HashSet[] markedAsRelevantSet;
    private LinkedList<ImageIcon>[][] searchResults;
    private int curQuery;
    private int curReqHandler;
    private static boolean saveSearchResults = true;

    // only for testing
    //private final String[] evaluationRequestHandlers = {"alt_only", "text1_only"};
    //private final String[] evaluationQueries = {"zlatan","\"prince charles\"", "\"princess madeleine\""};


    private final String[] evaluationRequestHandlers = {"alt_only", "text1_only",
            "allFields_equal", "allFields_weighting"};
    private final String[] evaluationQueries = {"zlatan",
            "\"prince charles\"",
            "\"princess madeleine\"",
            "\"abraham lincoln\"",
            "Obama",

            "airplane aircraft",
            "USA america US \"United States\"",
            "children child",
            "banana apple fruit",
            "\"New York\"",

            "\"stock market\"",
            "violence",
            "disease",
            "red",
            "blue"};

    // Index
    Index index;

    // GUI components
    JPanel jMainPanel;
    JPanel jTopPanel;
    JMenuBar jMenuBar;
    JMenu jFileMenu;
    JMenuItem jIndexUrlMenuItem;
    JTextField jSearchField;
    JTextField jEvalInfoField;
    JButton jSearchButton;
    JPanel jresultPanel;

    JPanel jBrowseButtonsPanel;
    JButton jBrowseHandlerFwd;
    JButton jBrowseHandlerBwd;
    JButton jBrowseQueryFwd;
    JButton jBrowseQueryBwd;


    JPanel jEndButtonsPanel;
    JButton jrelevanceFeedbackButton;
    JButton jStoreEvaluationButton;

    JPanel jResCluster;
    JPanel clusterPictures;

    TreeSet<SolrDocument> relevantDoc;

    UrlParser urlParser;

    /**
     * Constructor for the GUI.
     */
    public EvaluationGUI() {
        index = new Index();
        urlParser = new UrlParser();
        relevantDoc = new TreeSet<SolrDocument>(new SolrCompare());

        curQuery = 0;
        curReqHandler = 0;
        promptUserName();

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                storeCurrentMarking();
                storeMarkedAsRelevantSet();
                saveResultToFile(relevanceCheckBoxes,
                        evaluationRequestHandlers,
                        evaluationQueries,
                        index.projectPath + "/evaluation/" + userName,
                        true);
                e.getWindow().dispose();
            }
        });
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
        jTopPanel =  new JPanel(new BorderLayout());
        JPanel jEvaluationPanel = new JPanel(new BorderLayout());
        jMenuBar = new JMenuBar();
        jFileMenu = new JMenu("File");
        jIndexUrlMenuItem = new JMenuItem("Index URL ...");
        JPanel searchPanel = new JPanel();
        jSearchField = new JTextField(40);
        jEvalInfoField = new JTextField(40);

        jSearchButton = new JButton("Search");

        jEndButtonsPanel = new JPanel(new BorderLayout());
        jrelevanceFeedbackButton = new JButton("Give relevance feedback");
        jStoreEvaluationButton = new JButton("Save evaluation");

        jBrowseButtonsPanel = new JPanel();
        jBrowseHandlerFwd  = new JButton("Next Req. Handler");
        jBrowseHandlerBwd = new JButton("Prev. Req. Handler");
        jBrowseQueryFwd = new JButton("Next Query");
        jBrowseQueryBwd = new JButton("Prev Query");

        jresultPanel = new JPanel(new GridLayout(0, PICTURES_PER_ROW));
        jResCluster = new JPanel(new BorderLayout());
        // in width

        // Set all action listeners
        setActionListeners();

        // Add GUI components to the main panel

        jFileMenu.add(jIndexUrlMenuItem);
        searchPanel.add(jSearchField);
        searchPanel.add(jSearchButton);

        jBrowseButtonsPanel.add(jBrowseHandlerBwd);
        jBrowseButtonsPanel.add(jBrowseHandlerFwd);
        jBrowseButtonsPanel.add(jBrowseQueryBwd);
        jBrowseButtonsPanel.add(jBrowseQueryFwd);

        jEvaluationPanel.add(jEvalInfoField, BorderLayout.PAGE_START);
        jEvaluationPanel.add(jBrowseButtonsPanel, BorderLayout.CENTER);
        jEvaluationPanel.add(jStoreEvaluationButton, BorderLayout.PAGE_END);

        jTopPanel.add(searchPanel, BorderLayout.PAGE_START);
        jTopPanel.add(jEvaluationPanel, BorderLayout.CENTER);

        jResCluster.add(jresultPanel, BorderLayout.SOUTH);

        jMenuBar.add(jFileMenu);
        jMainPanel.add(jTopPanel, BorderLayout.PAGE_START);
        jMainPanel.add(jResCluster, BorderLayout.CENTER);


        jEndButtonsPanel.add(jrelevanceFeedbackButton, BorderLayout.PAGE_START);
        jMainPanel.add(jEndButtonsPanel, BorderLayout.PAGE_END);

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

        searchResults = new LinkedList[evaluationRequestHandlers.length][evaluationQueries.length];
        relevanceCheckBoxes = new ArrayList[evaluationRequestHandlers.length][evaluationQueries.length];

        markedAsRelevantSet = loadMarkedAsRelevantSet();

        // Present the first query handler setting to evaluate
        updateEvaluationBrowse();

    }

    private void promptUserName(){
        final JDialog promptFrame = new JDialog();
        JPanel panel = new JPanel(new BorderLayout());
        JLabel promptText = new JLabel("Enter your name");
        final JTextField input = new JTextField("default_user");
        JButton enter = new JButton("Start Evaluation");

        panel.add(promptText, BorderLayout.PAGE_START);
        panel.add(input, BorderLayout.CENTER);
        panel.add(enter, BorderLayout.PAGE_END);
        promptFrame.add(panel);
        promptFrame.setSize(200, 100);
        promptFrame.setLocationRelativeTo(null);
        promptFrame.setVisible(true);

        enter.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                userName = input.getText();
                promptFrame.dispose();
                init();
            }
        });
    }

    private void storeMarkedAsRelevantSet(){
        String filePathName = index.projectPath + "/evaluation/" + userName + "_marked.evl";
        FileOutputStream fos;
        ObjectOutputStream out;
        try {
            fos = new FileOutputStream(filePathName);
            out = new ObjectOutputStream(fos);
            out.writeObject(markedAsRelevantSet);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HashSet[] loadMarkedAsRelevantSet(){
        String filePathName = index.projectPath + "/evaluation/" + userName + "_marked.evl";

        HashSet[] markedAsRelevantSet = null;

        FileInputStream fis;
        ObjectInputStream in;
        try {
            fis = new FileInputStream(filePathName);
            in = new ObjectInputStream(fis);
            markedAsRelevantSet = ( HashSet[] ) in.readObject();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error when trying to read markedAsRelevantSet from file!");
        }

        if (markedAsRelevantSet == null || markedAsRelevantSet[0] == null ||
                markedAsRelevantSet.length != evaluationQueries.length){
            markedAsRelevantSet = new HashSet[evaluationQueries.length];
            for (int i = 0; i < markedAsRelevantSet.length; i++){
                markedAsRelevantSet[i] = new HashSet(index.rowsRetrieved);
            }
        }
        return markedAsRelevantSet;
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
                int returnVal = fc.showOpenDialog(EvaluationGUI.this);
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
                if(allowClustering){
                    doClustering = true;
                }
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
                for(int i=0; i< relevanceCheckBoxes[curReqHandler][curQuery].size(); i++){
                    if(relevanceCheckBoxes[curReqHandler][curQuery].get(i).isSelected()){
                        relevantDoc.add(resultDocuments.get(i));
                    }
                }
                String expanded = index.relevanceFeedback(relevantDoc);
                searchAndUpdateGUI(expanded, jSearchField.getText());
            }
        });

        // STORE EVALUATION

        jStoreEvaluationButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                saveResultToFile(relevanceCheckBoxes,
                        evaluationRequestHandlers,
                        evaluationQueries,
                        index.projectPath + "/evaluation/" + userName,
                        true);

                if (saveSearchResults){
                    System.err.print("Storing search results (images) to file... ");
                    for (int rhIdx = 0; rhIdx < evaluationRequestHandlers.length; rhIdx++){
                        for (int qIdx = 0; qIdx < evaluationQueries.length; qIdx++){
                            if (searchResults[rhIdx][qIdx] != null && searchResults[rhIdx][qIdx].size() != 0){
                                saveImagesToOne(searchResults[rhIdx][qIdx],
                                        index.projectPath + "/evaluation/imageResults/" +
                                                userName + "_" +
                                                evaluationRequestHandlers[rhIdx] + "_" +
                                                evaluationQueries[qIdx].replace("\"", "_"));
                            }
                        }
                    }
                    System.err.println("Complete!");
                }
            }
        });

        jBrowseHandlerFwd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (curReqHandler < evaluationRequestHandlers.length-1){
                    storeCurrentMarking();
                    curReqHandler++;
                    updateEvaluationBrowse();
                }
            }
        });

        jBrowseHandlerBwd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (curReqHandler > 0){
                    storeCurrentMarking();
                    curReqHandler--;
                    updateEvaluationBrowse();
                }
            }
        });

        jBrowseQueryFwd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (curQuery < evaluationQueries.length-1){
                    storeCurrentMarking();
                    curQuery++;
                    updateEvaluationBrowse();
                }
            }
        });

        jBrowseQueryBwd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (curQuery > 0){
                    storeCurrentMarking();
                    curQuery--;
                    updateEvaluationBrowse();
                }
            }
        });


    }

    private void storeCurrentMarking(){
        // must be done before changing curQuery or curReqestHandler...

        if (relevanceCheckBoxes[curReqHandler][curQuery] != null){
            for (int i = 0; i < relevanceCheckBoxes[curReqHandler][curQuery].size(); i++){
                if (relevanceCheckBoxes[curReqHandler][curQuery].get(i).isSelected()){
                    markedAsRelevantSet[curQuery].add(resultDocuments.get(i).getFieldValue("id"));
                }
                else{
                    markedAsRelevantSet[curQuery].remove(resultDocuments.get(i).getFieldValue("id"));
                }
            }
        }
    }

    private void updateEvaluationBrowse(){

        jSearchField.setText(evaluationQueries[curQuery]);
        jEvalInfoField.setText("Current Request Handler:  " + evaluationRequestHandlers[curReqHandler] +
                ",  Current query nb:  " + curQuery);
        index.setRequest_handler(evaluationRequestHandlers[curReqHandler]);
        this.jSearchField.setText(evaluationQueries[curQuery]);
        this.searchAndUpdateGUI(evaluationQueries[curQuery], null);
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
   * from the resultDocuments returned and update the GUI with these pictures
   */

    public void searchAndUpdateGUI(String query, String boostedQueries) {
        long tic = System.currentTimeMillis();
        resultDocuments = null;
        if (boostedQueries != null) {
            resultDocuments = index.search(query, boostedQueries);
        } else {
            resultDocuments = index.search(query);
        }

        LinkedList<ImageIcon> img = index.getSavedImages(resultDocuments);

        LinkedList<String> info = index.getInformation(resultDocuments);

        LinkedList<String> urls = index.getURLs(resultDocuments);

        //Store images for later saving to file.
        if(searchResults[curReqHandler][curQuery] == null){
            searchResults[curReqHandler][curQuery] = img;
        }

        if (info == null || info.size() == 0) {
            System.err.println("No results found");
            jEvalInfoField.setText("Current Request Handler:  " + evaluationRequestHandlers[curReqHandler] +
                    ",  Current query nb:  " + curQuery + "  --->  NO RESULTS FOUND");
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
                        PICTURES_PER_ROW - 1));
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
                    if (clusterLabel.length() > 20) {
                        clusterPanel.add(new JTextField(clusterLabel
                                .subSequence(0, 20).toString()), c);
                        c.gridx = 0;
                        c.gridy = 2;
                        clusterPanel.add(new JTextField(clusterLabel
                                .subSequence(20, Math.min(clusterLabel.length(),40))
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
        if(relevanceCheckBoxes[curReqHandler][curQuery] == null)
            relevanceCheckBoxes[curReqHandler][curQuery] = new ArrayList<>(img.size());
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

            try{
                relevanceCheckBoxes[curReqHandler][curQuery].get(i);
            }
            catch (Exception e){
                JCheckBox evalCheckBox = new JCheckBox();
                relevanceCheckBoxes[curReqHandler][curQuery].add(evalCheckBox);
            }
            c.gridx = 0;
            c.gridy = 1;
            imageAndFeedback.add(relevanceCheckBoxes[curReqHandler][curQuery].get(i), c);

            jresultPanel.add(imageAndFeedback);

            if(markedAsRelevantSet[curQuery].contains(resultDocuments.get(i).getFieldValue("id"))){
                relevanceCheckBoxes[curReqHandler][curQuery].get(i).setSelected(true);
            }
            else{
                relevanceCheckBoxes[curReqHandler][curQuery].get(i).setSelected(false);
            }

        }
        validate();
        repaint();
        System.err.println("Searched with " + resultDocuments.size()
                + " resultDocuments retrieved");
        System.err.println("total time " + (System.currentTimeMillis() - tic));
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        // TODO Auto-generated method stub

    }



    private static void saveResultToFile(ArrayList<JCheckBox>[][] relevanceCheckBoxes,
                                         String[] requestHandlers,
                                         String[] queries,
                                         String filePathName, boolean matlabFormat){
        StringBuilder sb = new StringBuilder();

        System.err.print("Storing evaluation result to file... ");

        //General INFO
        sb.append("# RESULT FROM EVALUATION (SETUP BELLOW)\n");
        sb.append("#\n");
        sb.append("# --- Request Handlers ---\n");
        for(String reqHand: requestHandlers){
            sb.append("# " + reqHand + "\n");
        }
        sb.append("#\n# --- Queries ---\n");
        for(String query: queries){
            sb.append("# " + query + "\n");
        }
        sb.append("#\n# --- Evaluation Data ---\n");
        // DATA:
        // nb_results 1 0 0 1 ...
        for (int rhIdx = 0; rhIdx < relevanceCheckBoxes.length; rhIdx++){
            sb.append("RH " + rhIdx + " " + requestHandlers[rhIdx] + "\n");
            for (int qIdx = 0; qIdx < relevanceCheckBoxes[rhIdx].length; qIdx++){
                if(relevanceCheckBoxes[rhIdx][qIdx] != null){
                    if(relevanceCheckBoxes[rhIdx][qIdx].size() == 0){
                        sb.append("/ No Results ");
                    }
                    for (JCheckBox checkBox : relevanceCheckBoxes[rhIdx][qIdx]){
                        if (checkBox.isSelected()){
                            sb.append("1 ");
                        }
                        else {
                            sb.append("0 ");
                        }
                    }
                    sb.append("# ("+ relevanceCheckBoxes[rhIdx][qIdx].size() + ") "); //nb of results
                }
                else{
                    sb.append("- Not Evaluated: ");
                }
                sb.append(queries[qIdx].replace(' ', '_') + "\n");
            }
        }
        PrintWriter out = null;
        try {
            out = new PrintWriter(filePathName + ".txt");
            out.print(sb.toString());
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if (out != null){
            out.close();
        }

        System.err.println("Complete!");

    }

    private static void saveImagesToOne(LinkedList<ImageIcon> images, String filePathName){

        int cols = Math.min(5, images.size());
        int rows;
        if (images.size() % cols == 0){
            rows = images.size()/cols;
        }
        else{
            rows = images.size()/cols + 1;
        }
        int iconWidth = 0;
        int iconHeight = 0;
        for (ImageIcon image : images){
            if (image.getIconWidth() > iconWidth){
                iconWidth = image.getIconWidth();
            }
            if (image.getIconHeight() > iconHeight){
                iconHeight = image.getIconHeight();
            }
        }

        //creating a bufferd image array from image files
        BufferedImage[] buffImages = new BufferedImage[images.size()];
        for (int i = 0; i < images.size(); i++) {
            BufferedImage bi = new BufferedImage(
                    images.get(i).getIconWidth(),
                    images.get(i).getIconHeight(),
                    BufferedImage.TYPE_INT_RGB);
            Graphics g = bi.createGraphics();
            images.get(i).paintIcon(null, g, 0, 0);
            g.dispose();
            buffImages[i] = bi;
        }
        int type = buffImages[0].getType();

        //Initializing the final image
        BufferedImage finalImg = new BufferedImage(iconWidth*cols, iconHeight*rows, type);

        Graphics g = finalImg.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, iconWidth*cols, iconHeight*rows);
        g.dispose();

        int imgIdx = 0;
        int row = 0;
        while (imgIdx < images.size()){
            for (int col = 0; col < cols; col++) {
                if (imgIdx == images.size()){
                    break;
                }
                finalImg.createGraphics().drawImage(buffImages[imgIdx],
                        iconWidth * col, iconHeight * row, null);
                imgIdx++;
            }
            row++;
        }

        try {
            ImageIO.write(finalImg, "jpeg", new File(filePathName + ".jpg"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}

