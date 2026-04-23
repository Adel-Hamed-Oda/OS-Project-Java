

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
import javafx.scene.control.SplitPane;
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
    private final ListView<String> readyQueueList = new ListView<>();
    private final ListView<String> blockedQueueList = new ListView<>();

    private final ComboBox<String> algorithmSelector = new ComboBox<>();
    private final TextField rrQuantumField = new TextField("2");
    private final Button startButton = new Button("Start Simulation");
    private final Button stepButton = new Button("Step");
    private final Button runPauseButton = new Button("Run");

    private final Label inputRequestLabel = new Label("No process is waiting for input.");
    private final TextField inputField = new TextField();
    private final Button submitInputButton = new Button("Submit Input");
    private VBox inputPanel;

    private Timeline uiRefreshTimeline;
    private volatile boolean simulationCompleted = false;

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        HBox controls = buildControls();
        HBox topStatus = buildTopStatus();

        VBox queuePane = new VBox(6,
            new Label("Ready Queue"), readyQueueList,
            new Label("Blocked Queue"), blockedQueueList);
        readyQueueList.setPrefHeight(100);
        blockedQueueList.setPrefHeight(100);

        VBox leftPane = new VBox(8, new Label("Process States"), processStatesList, queuePane);
        leftPane.setPadding(new Insets(10, 10, 10, 0));
        VBox.setVgrow(processStatesList, Priority.ALWAYS);

        memoryArea.setEditable(false);
        memoryArea.setWrapText(false);
        memoryArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 12px;");

        VBox centerPane = new VBox(8, new Label("Memory Content"), memoryArea);
        centerPane.setPadding(new Insets(10, 0, 10, 10));
        VBox.setVgrow(memoryArea, Priority.ALWAYS);

        SplitPane body = new SplitPane(leftPane, centerPane);
        body.setDividerPositions(0.25);

        inputPanel = buildInputPanel();

        root.setTop(new VBox(10, controls, topStatus));
        root.setCenter(body);
        root.setBottom(inputPanel);

        Scene scene = new Scene(root, 1400, 800);
        primaryStage.setTitle("OS Scheduler Dashboard");
        primaryStage.setScene(scene);
        primaryStage.show();

        initializeRefreshLoop();
        refreshUI();
    }

    private VBox buildInputPanel() {
        inputField.setPromptText("Type input here...");
        inputField.setDisable(true);

        submitInputButton.setDisable(true);
        submitInputButton.setOnAction(event -> submitInputFromGUI());
        inputField.setOnAction(event -> submitInputFromGUI());

        VBox container = new VBox(8,
                new Label("User Input"),
                inputRequestLabel,
                new HBox(8, inputField, submitInputButton));
        container.setPadding(new Insets(10, 0, 0, 0));
        HBox.setHgrow(inputField, Priority.ALWAYS);
        container.setVisible(false);
        container.setManaged(false);
        container.setDisable(true);

        return container;
    }

    private void submitInputFromGUI() {
        if (!SystemCalls.isAwaitingInput()) {
            return;
        }

        if (SystemCalls.provideInput(inputField.getText())) {
            inputField.clear();
            inputField.setDisable(true);
            submitInputButton.setDisable(true);
        }
    }

    private HBox buildControls() {
        algorithmSelector.getItems().addAll("RR", "HRRN", "MLFQ");
        algorithmSelector.setValue("RR");

        rrQuantumField.setPrefWidth(70);

        startButton.setOnAction(event -> startSimulation());
        stepButton.setDisable(true);
        stepButton.setOnAction(event -> Scheduler.requestStep());
        runPauseButton.setDisable(true);
        runPauseButton.setOnAction(event -> toggleRunPause());

        HBox controls = new HBox(10,
                new Label("Algorithm:"), algorithmSelector,
                new Label("RR Quantum:"), rrQuantumField,
                startButton,
            stepButton,
            runPauseButton,
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
        stepButton.setDisable(false);
        runPauseButton.setDisable(false);
        runPauseButton.setText("Run");
        simulationCompleted = false;
        statusLabel.setText("Status: Ready to Step");

        Scheduler.initializeSimulation();
        Scheduler.enableStepMode();
        Scheduler.setAutoRun(false);
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
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("Status: Error - " + ex.getMessage()));
            } finally {
                Scheduler.disableStepMode();
                simulationCompleted = true;
            }
        });

        simulationThread.setDaemon(true);
        simulationThread.start();
    }

    private void toggleRunPause() {
        boolean shouldAutoRun = !Scheduler.isAutoRunEnabled();
        Scheduler.setAutoRun(shouldAutoRun);

        runPauseButton.setText(shouldAutoRun ? "Pause" : "Run");
        stepButton.setDisable(shouldAutoRun);

        if (shouldAutoRun) {
            statusLabel.setText("Status: Running");
        } else if (!SystemCalls.isAwaitingInput()) {
            statusLabel.setText("Status: Ready to Step");
        }
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

        readyQueueList.getItems().setAll(Scheduler.getReadyQueueSnapshot());
        blockedQueueList.getItems().setAll(Scheduler.getBlockedQueueSnapshot());

        List<String> memorySnapshot = Memory.getMemorySnapshot();
        memoryArea.setText(String.join("\n", memorySnapshot));

        refreshInputWindowState();

        if (simulationCompleted) {
            statusLabel.setText("Status: Completed");
            startButton.setDisable(false);
            stepButton.setDisable(true);
            runPauseButton.setDisable(true);
            runPauseButton.setText("Run");
            simulationCompleted = false;
        }
    }

    private void refreshInputWindowState() {
        boolean awaitingInput = SystemCalls.isAwaitingInput();

        if (awaitingInput) {
            Integer processID = SystemCalls.getAwaitingInputProcessID();
            String processText = processID == null ? "Unknown" : "P" + processID;
            inputRequestLabel.setText("Waiting for input from " + processText + ".");
            inputField.setDisable(false);
            submitInputButton.setDisable(false);
            inputPanel.setManaged(true);
            inputPanel.setVisible(true);
            inputPanel.setDisable(false);

            statusLabel.setText("Status: Waiting for Input");
            Platform.runLater(inputField::requestFocus);
            return;
        }

        inputRequestLabel.setText("No process is waiting for input.");
        inputField.clear();
        inputField.setDisable(true);
        submitInputButton.setDisable(true);
        inputPanel.setDisable(true);
        inputPanel.setVisible(false);
        inputPanel.setManaged(false);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
