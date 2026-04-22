

import java.util.ArrayList;
import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Dashboard extends Application {

    private final Label timeLabel = new Label("Time: 0");
    private final Label currentProcessLabel = new Label("Current Process: None");
    private final Label statusLabel = new Label("Status: Idle");

    private final TextArea memoryArea = new TextArea();
    private final ListView<String> processStatesList = new ListView<>();

    private final ComboBox<String> algorithmSelector = new ComboBox<>();
    private final TextField rrQuantumField = new TextField("2");
    private final Button startButton = new Button("Start Simulation");

    private Timeline uiRefreshTimeline;
    private volatile boolean simulationCompleted = false;

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        HBox controls = buildControls();
        HBox topStatus = buildTopStatus();

        VBox leftPane = new VBox(8, new Label("Process States"), processStatesList);
        leftPane.setPadding(new Insets(10, 10, 10, 0));
        VBox.setVgrow(processStatesList, Priority.ALWAYS);

        memoryArea.setEditable(false);
        memoryArea.setWrapText(false);

        VBox centerPane = new VBox(8, new Label("Memory Content"), memoryArea);
        centerPane.setPadding(new Insets(10, 0, 10, 10));
        VBox.setVgrow(memoryArea, Priority.ALWAYS);

        HBox body = new HBox(12, leftPane, centerPane);
        HBox.setHgrow(leftPane, Priority.SOMETIMES);
        HBox.setHgrow(centerPane, Priority.ALWAYS);

        root.setTop(new VBox(10, controls, topStatus));
        root.setCenter(body);

        Scene scene = new Scene(root, 1100, 650);
        primaryStage.setTitle("OS Scheduler Dashboard");
        primaryStage.setScene(scene);
        primaryStage.show();

        initializeRefreshLoop();
        refreshUI();
    }

    private HBox buildControls() {
        algorithmSelector.getItems().addAll("RR", "HRRN", "MLFQ");
        algorithmSelector.setValue("RR");

        rrQuantumField.setPrefWidth(70);

        startButton.setOnAction(event -> startSimulation());

        HBox controls = new HBox(10,
                new Label("Algorithm:"), algorithmSelector,
                new Label("RR Quantum:"), rrQuantumField,
                startButton,
                statusLabel);

        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(0, 0, 5, 0));
        return controls;
    }

    private HBox buildTopStatus() {
        HBox topStatus = new HBox(25, timeLabel, currentProcessLabel);
        topStatus.setAlignment(Pos.CENTER_LEFT);
        return topStatus;
    }

    private void initializeRefreshLoop() {
        uiRefreshTimeline = new Timeline(new KeyFrame(Duration.millis(250), event -> refreshUI()));
        uiRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        uiRefreshTimeline.play();
    }

    private void startSimulation() {
        startButton.setDisable(true);
        simulationCompleted = false;
        statusLabel.setText("Status: Running");

        Scheduler.initializeSimulation();
        refreshUI();

        String selectedAlgorithm = algorithmSelector.getValue();

        Thread simulationThread = new Thread(() -> {
            try {
                ArrayList<OS_Process> processExecutionList = new ArrayList<>(Scheduler.allProcesses);

                switch (selectedAlgorithm) {
                    case "HRRN" -> Scheduler.simulate_HRRN(processExecutionList);
                    case "MLFQ" -> Scheduler.simulate_MLFQ(processExecutionList);
                    default -> {
                        int timeQuantum = parseRRQuantum();
                        Scheduler.simulate_RR(processExecutionList, timeQuantum);
                    }
                }

                simulationCompleted = true;
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("Status: Error - " + ex.getMessage()));
                simulationCompleted = true;
            }
        });

        simulationThread.setDaemon(true);
        simulationThread.start();
    }

    private int parseRRQuantum() {
        try {
            int value = Integer.parseInt(rrQuantumField.getText().trim());
            return Math.max(1, value);
        } catch (NumberFormatException ex) {
            rrQuantumField.setText("2");
            return 2;
        }
    }

    private void refreshUI() {
        timeLabel.setText("Time: " + Scheduler.getCurrentTimeSnapshot());

        Integer runningProcessID = Scheduler.getCurrentRunningProcessID();
        String runningValue = runningProcessID == null ? "None" : "P" + runningProcessID;
        currentProcessLabel.setText("Current Process: " + runningValue);

        List<String> processStates = Scheduler.getProcessStateSnapshot();
        processStatesList.getItems().setAll(processStates);

        List<String> memorySnapshot = Memory.getMemorySnapshot();
        memoryArea.setText(String.join("\n", memorySnapshot));

        if (simulationCompleted) {
            statusLabel.setText("Status: Completed");
            startButton.setDisable(false);
            simulationCompleted = false;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
