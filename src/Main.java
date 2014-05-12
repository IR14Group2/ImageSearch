/**
 * Main class that starts the GUI.
 */
public class Main {

    //Toggle between evaluation and normal mode:
    private static boolean evaluationMode = false;

    public static void main(String[] args) {

        if(!evaluationMode){
            // Start the main GUI
            GUI gui = new GUI();
            gui.init();
        }
        else{
            // Start the evaluation GUI
            EvaluationGUI egui = new EvaluationGUI();
        }
    }

}
