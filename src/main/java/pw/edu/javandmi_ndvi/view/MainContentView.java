package pw.edu.javandmi_ndvi.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.VBox;

public class MainContentView extends VBox {
    private VBox root;
    public Button loadButton;
    public Button calculateButton;
    public RadioButton ndmiButton;
    public RadioButton ndviButton;
    public final Label statusLabel;

    public MainContentView() {
        ndmiButton = new RadioButton("Indeks wilgotności (NDMI)");
        ndmiButton.getStyleClass().add("my-radio-button");
        ndviButton = new RadioButton("Indeks wegetacji (NDVI)");
        ndviButton.getStyleClass().add("my-radio-button");

        Label label = new Label("Oblicz:");
        VBox radioBox = new VBox(10, label, ndmiButton, ndviButton);
        radioBox.setMaxWidth(200);
        VBox.setMargin(radioBox, new Insets(0,0,10,0));

        loadButton = new Button("Wczytaj plik");
        loadButton.setPrefWidth(200);
        loadButton.setPrefHeight(40);
        loadButton.getStyleClass().add("my-button");

        statusLabel = new Label("");
        statusLabel.setWrapText(true);
        statusLabel.setVisible(false);

        calculateButton = new Button("Oblicz");
        calculateButton.setPrefWidth(200);
        calculateButton.setPrefHeight(40);
        calculateButton.getStyleClass().add("my-button");

        root = new VBox(20, loadButton, statusLabel, radioBox, calculateButton);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40, 20, 20, 20));
    }

    public VBox getView() {
        return root;
    }
}
