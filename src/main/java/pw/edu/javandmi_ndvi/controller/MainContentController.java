package pw.edu.javandmi_ndvi.controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.geotools.coverage.grid.GridCoverage2D;
import pw.edu.javandmi_ndvi.core.FileUtils;
import pw.edu.javandmi_ndvi.core.GeoUtils;
import pw.edu.javandmi_ndvi.core.TileProcessor;
import pw.edu.javandmi_ndvi.view.MainContentView;
import pw.edu.javandmi_ndvi.view.ProgressUI;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;


import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainContentController {

    private final MainContentView view;
    private final Object pauseLock = new Object();
    private AtomicBoolean paused = new AtomicBoolean(false);

    public MainContentController(MainContentView view) {
        this.view = view;
        setupActions();
    }

    private void setupActions() {
        view.loadButton.setOnAction(e -> handleLoad());
        view.calculateButton.setOnAction(e -> handleCalculate());
    }

    private void handleLoad() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Wczytaj plik");

        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Pliki skompreskowany .zip", "*.zip");
        fileChooser.getExtensionFilters().add(extFilter);

        Stage stage = (Stage) view.loadButton.getScene().getWindow();

        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            final String baseText = "Ładowanie";
            final String[] dots = {".", "..", "..."};
            final int[] index = {0};

            Timeline loadingAnimation = new Timeline(
                new KeyFrame(Duration.millis(500), e -> {
                    view.statusLabel.setText(baseText + dots[index[0]]);
                    index[0] = (index[0] + 1) % dots.length;
                })
            );
            loadingAnimation.setCycleCount(Timeline.INDEFINITE);

            Platform.runLater(() -> {
                view.statusLabel.setStyle("-fx-text-fill: blue;");
                view.statusLabel.setVisible(true);
                loadingAnimation.play();
            });

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    File setupScript = extractScriptFromResources("setup_venv.py");
                    ProcessBuilder setupPb = new ProcessBuilder("python3", setupScript.getAbsolutePath());
                    setupPb.inheritIO();
                    Process setupProc = setupPb.start();
                    if (setupProc.waitFor() != 0) {
                        throw new IOException("setup_venv.py failed");
                    }

                    File processScript = extractScriptFromResources("convert_to_tiff.py");

                    String pythonExePath;
                    if (System.getProperty("os.name").toLowerCase().contains("win")) {
                        pythonExePath = "venv\\Scripts\\python.exe";
                    } else {
                        pythonExePath = "./venv/bin/python";
                    }

                    ProcessBuilder processPb = new ProcessBuilder(
                            pythonExePath,
                            processScript.getAbsolutePath(),
                            selectedFile.getAbsolutePath()
                    );
                    processPb.inheritIO();
                    Process processProc = processPb.start();
                    if (processProc.waitFor() != 0) {
                        throw new IOException("convert_to_tiff.py failed");
                    }

                    File venvDir = new File("venv");
                    if (venvDir.exists()) {
                        System.out.println("Cleaning up python files");
                        FileUtils.deleteDirectoryRecursively(venvDir);
                    }

                    File outputDir = new File("converted_tifs");
                    File b04 = new File(outputDir, "B04.tif");
                    File b08 = new File(outputDir, "B08.tif");
                    File b11 = new File(outputDir, "B11.tif");

                    if (!b04.exists() || !b08.exists() || !b11.exists()) {
                        throw new IOException("Brak jednego z pasm PNG");
                    }

                    return null;
                }

                @Override
                protected void succeeded() {
                    loadingAnimation.stop();
                    Platform.runLater(() -> {
                        view.statusLabel.setText("Pomyślnie wczytano i przekonwertowano: " + selectedFile.getName());
                        view.statusLabel.setStyle("-fx-text-fill: green;");
                    });
                }

                @Override
                protected void failed() {
                    loadingAnimation.stop();
                    Throwable ex = getException();
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setContentText("Nie udało się wczytać pliku: " + selectedFile.getName() + "\n" + ex.getMessage());
                        alert.show();
                        view.statusLabel.setVisible(false);
                    });
                }
            };

            Thread thread = new Thread(task);
            thread.setDaemon(true);
            thread.start();

        } else {
            view.statusLabel.setVisible(false);
        }
    }



    private void showAlert(String text){
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setContentText(text);
        a.show();
    }

    private void handleCalculate() {
         File outputDir = new File("converted_tifs");
                File b04 = new File(outputDir, "B04.tif");
                File b08 = new File(outputDir, "B08.tif");
                File b11 = new File(outputDir, "B11.tif");

        if (!b04.exists() || !b08.exists() || !b11.exists()) {
            showAlert("Należy wczytać plik wejściowy");
            return;
        }

        Boolean ndmiCalculation = view.ndmiButton.isSelected();
        Boolean ndviCalculation = view.ndviButton.isSelected();

        if (!ndmiCalculation && !ndviCalculation) {
            showAlert("Należy wybrać minimum jeden rodzaj obliczanego indexu");
            return;
        }

        ProgressUI progressUI = showProgressBar();

        Task<Void> processingTask = new Task<>() {
            @Override
            protected Void call() {
                GridCoverage2D red = GeoUtils.readCoverage("converted_tifs/B04.tif");
                GridCoverage2D nir = GeoUtils.readCoverage("converted_tifs/B08.tif");
                GridCoverage2D swir = GeoUtils.readCoverage("converted_tifs/B11.tif");

                if (red == null || nir == null || swir == null) {
                    Platform.runLater(() -> showAlert("Błąd odczytu danych"));
                    return null;
                }

                GridCoverage2D swirResampled = GeoUtils.resampleCoverage(swir, red);

                int width = red.getRenderedImage().getWidth();
                int height = red.getRenderedImage().getHeight();

                BufferedImage ndviImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                BufferedImage ndmiImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

                int tileSize = 1024;

                ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
                ArrayList<Future<Void>> futures = new ArrayList<>();

                for (int y = 0; y < height; y += tileSize) {
                    for (int x = 0; x < width; x += tileSize) {
                        int tileWidth = Math.min(tileSize, width - x);
                        int tileHeight = Math.min(tileSize, height - y);
                        int tileX = x;
                        int tileY = y;

                        TileProcessor task = new TileProcessor(red, nir, swirResampled, ndviImage, ndmiImage, tileX, tileY, tileWidth, tileHeight, ndviCalculation, ndmiCalculation, pauseLock, paused);

                        futures.add(executor.submit(task));
                    }
                }

                int totalTiles = ((width + tileSize - 1) / tileSize) * ((height + tileSize - 1) / tileSize);
                int completed = 0;
                for (Future<Void> future : futures) {
                    try {
                        future.get();
                        completed++;

                        int percent = (int) ((completed / (double) totalTiles) * 100);

                        int finalCompleted = completed;
                        Platform.runLater(() -> {
                            updateProgress(finalCompleted, totalTiles);
                            updateMessage(percent + "%");
                        });

                    } catch (Exception e) {
                        executor.shutdownNow();
                        Platform.runLater(() -> showAlert("Błąd podczas przetwarzania: " + e.getMessage()));
                        return null;
                    }
                }

                executor.shutdown();

                updateMessage("Tworzenie obrazów...");
                Platform.runLater(() -> progressUI.removeStopButton());

                if (ndviCalculation) {
                    String ndviFilename = System.getProperty("java.io.tmpdir") + "/ndvi.png";
                    FileUtils.savetoPng(ndviImage, ndviFilename);
                    Platform.runLater(() -> showOutputWindow(ndviFilename, "Indeks wegetacji (NDVI)"));
                }

                if (ndmiCalculation) {
                    String ndmiFilename = System.getProperty("java.io.tmpdir") + "/ndmi.png";
                    FileUtils.savetoPng(ndmiImage, ndmiFilename);
                    Platform.runLater(() -> showOutputWindow(ndmiFilename, "Indeks wilgotności (NDMI)"));
                }

                Platform.runLater(() -> progressUI.stage.close());

                return null;
            }
        };

        progressUI.progressBar.progressProperty().bind(processingTask.progressProperty());
        progressUI.progressLabel.textProperty().bind(processingTask.messageProperty());

        Thread thread = new Thread(processingTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void showOutputWindow(String pathfile, String title) {
        File file = new File(pathfile);

        if(!file.exists()) {
            showAlert("Blad poczas wizualizacji wyniku");
            return;
        }
        Image image = new Image(file.toURI().toString());

        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(600);

        Button saveButton = new Button("Zapisz obraz");
        saveButton.setOnAction(e -> handleSaveImage(file, saveButton));

        Label indexLabel = new Label(title);
        indexLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        VBox vbox = new VBox(10, indexLabel, imageView, saveButton);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(10));

        Scene scene = new Scene(vbox);
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(scene);

        stage.setOnCloseRequest(e -> {
            try {
                if(file.exists()) {
                    Files.delete(file.toPath());
                }
            } catch (IOException ex) {
                System.out.println("Couldn't remove a temporary png file");
            }
        });

        stage.show();
    }

    private ProgressUI showProgressBar() {
        ProgressBar progressBar = new ProgressBar(0);
        Label progressLabel = new Label("0%");
        progressBar.setPrefWidth(250);
        progressBar.setPrefHeight(40);

        Button stopButton = new Button("Zatrzymaj");
        stopButton.setPrefWidth(200);
        stopButton.setPrefHeight(40);
        stopButton.getStyleClass().add("my-button");
        stopButton.setOnAction(e -> {
            paused.set(!paused.get());

            synchronized (pauseLock) {
                if (!paused.get()) {
                    pauseLock.notifyAll();
                }
            }

            stopButton.setText(paused.get() ? "Wznów" : "Pauza");
        });

        Stage progressStage = new Stage();
        VBox vbox = new VBox(30, progressBar, progressLabel, stopButton);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(20));
        vbox.setPrefSize(300, 180);

        progressStage.setScene(new Scene(vbox));
        progressStage.setTitle("Przetwarzanie...");
        progressStage.setResizable(false);
        progressStage.show();

        return new ProgressUI(progressBar, progressLabel, progressStage, stopButton, vbox);
    }

    private void handleSaveImage(File file, Button saveButton) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Zapisz obraz");

        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));

        fileChooser.setInitialFileName(file.getName());

        Stage stage = (Stage) saveButton.getScene().getWindow();

        File destinationFile = fileChooser.showSaveDialog(stage);

        if(destinationFile == null) {
            return;
        }
        try{
            Files.copy(file.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }catch(IOException e){
            showAlert("Problem z zapisem obrazu");
        }
    }

    private File extractScriptFromResources(String scriptName) throws IOException {
        InputStream in = getClass().getResourceAsStream("/pw/edu/javandmi_ndvi/"+scriptName);
        if (in == null) {
            throw new FileNotFoundException("Script not found in resources: " + scriptName);
        }

        File scriptFile = File.createTempFile(scriptName.replace(".py", ""), ".py");
        scriptFile.deleteOnExit();

        try (OutputStream out = new FileOutputStream(scriptFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        return scriptFile;
    }


}