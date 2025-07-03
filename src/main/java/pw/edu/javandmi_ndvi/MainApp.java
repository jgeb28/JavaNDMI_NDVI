package pw.edu.javandmi_ndvi;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.scene.Scene;
import pw.edu.javandmi_ndvi.controller.MainContentController;
import pw.edu.javandmi_ndvi.core.FileUtils;
import pw.edu.javandmi_ndvi.view.BorderedTitledPane;
import pw.edu.javandmi_ndvi.view.MainContentView;

import java.io.File;
import java.io.IOException;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        MainContentView mainContentView = new MainContentView();
        new MainContentController(mainContentView);
        BorderedTitledPane borderedPane = new BorderedTitledPane(
                "Wyliczanie indeksów wilgotności (NDMI) i wegetacji (NDVI) roślin na zobrazowaniach satelitarnych",
                mainContentView.getView()
        );

        borderedPane.setPrefSize(500, 400);
        borderedPane.setMinSize(500, 400);
        borderedPane.setMaxSize(500, 400);

        StackPane root = new StackPane(borderedPane);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #D9D9D9;");

        StackPane.setAlignment(borderedPane, Pos.CENTER);

        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/pw/edu/javandmi_ndvi/style.css").toExternalForm());

        primaryStage.setScene(scene);

        primaryStage.setOnCloseRequest((e) -> {
            try{
                FileUtils.deleteDirectoryRecursively(new File("converted_tifs"));
            }catch(IOException ex){
                System.out.print("Couldn't delete temporary tif files\n");
            }
        });

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}