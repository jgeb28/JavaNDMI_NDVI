package pw.edu.javandmi_ndvi.view;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ProgressUI {
    public ProgressBar progressBar;
    public Label progressLabel;
    public Stage stage;
    public VBox container;
    private Button stopButton;

    public ProgressUI(ProgressBar progressBar, Label progressLabel, Stage stage,Button stopButton, VBox container) {
        this.progressBar = progressBar;
        this.progressLabel = progressLabel;
        this.stage = stage;
        this.stopButton = stopButton;
        this.container = container;
    }

    public void removeStopButton(){
        this.container.getChildren().remove(this.stopButton);
    }
}
