
import java.util.ArrayList;
import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Dashboard extends Application {

    private final Label timeLabel = new Label("Time: 0");
    private final Label currentProcessLabel = new Label("Current Process: None");
    private final Label statusLabel = new Label("Status: Idle");

    private final TextFlow memoryFlow = new TextFlow();
    private final ListView<String> processStatesList = new ListView<>();
    private final ListView<String> readyQueueList = new ListView<>();
    private final ListView<String> blockedQueueList = new ListView<>();

    private final ComboBox<String> algorithmSelector = new ComboBox<>();
    private final Label quantumLabel = new Label("Quantum Time:");
    private final TextField rrQuantumField = new TextField("");
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
        root.getStyleClass().add("root-pane");

        // Top Section: Controls and Status Header
        VBox topContainer = new VBox(15, buildControls(), buildTopStatus());
        topContainer.setPadding(new Insets(20));
        root.setTop(topContainer);

        // Left Section: Queues and States
        VBox processBox = createCard("Process States", processStatesList);
        VBox readyBox = createCard("Ready Queue", readyQueueList);
        VBox blockedBox = createCard("Blocked Queue", blockedQueueList);

        VBox leftPane = new VBox(15, processBox, readyBox, blockedBox);
        leftPane.setPadding(new Insets(0, 10, 20, 20));
        leftPane.setPrefWidth(350);
        VBox.setVgrow(processBox, Priority.ALWAYS);

        // Center Section: Memory
        ScrollPane memScrollPane = new ScrollPane(memoryFlow);
        memScrollPane.setFitToWidth(true);
        memScrollPane.setFitToHeight(true);
        memScrollPane.getStyleClass().add("memory-scroll");
        memoryFlow.getStyleClass().add("memory-container");
        memoryFlow.setLineSpacing(2);

        VBox memoryBox = createCard("System Memory Map", memScrollPane);
        memoryBox.setPadding(new Insets(0, 20, 20, 10));

        SplitPane body = new SplitPane(leftPane, memoryBox);
        body.setDividerPositions(0.3);
        root.setCenter(body);

        // Bottom Section: Input
        inputPanel = buildInputPanel();
        root.setBottom(inputPanel);

        Scene scene = new Scene(root, 1400, 800);
        // Link the CSS file
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        processStatesList.setCellFactory(lv -> new ListCell<String>() {
            private final ProgressBar pb = new ProgressBar();
            private final HBox container = new HBox(10, new Label(), pb);

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label lbl = (Label) container.getChildren().get(0);
                    lbl.setText(item);
                    double progress = calculateProgress(item);
                    pb.setProgress(progress);

                    boolean isWaiting = item.contains("Waiting");
                    if (pb.getProgress() >= 1.0)
                        pb.setStyle("-fx-accent: #2f9e44;");
                    else if (isWaiting)
                        pb.setStyle("-fx-accent: #8d8c8b;");
                    else
                        pb.setStyle("-fx-accent: #4dabf7;");

                    setGraphic(container);
                }
            }
        });

        primaryStage.setTitle("Modern OS Scheduler Dashboard");
        primaryStage.setScene(scene);
        primaryStage.show();

        initializeRefreshLoop();
        refreshUI();
    }

    private VBox createCard(String title, Control content) {
        Label header = new Label(title.toUpperCase());
        header.getStyleClass().add("card-header");
        VBox card = new VBox(8, header, content);
        card.getStyleClass().add("card");
        VBox.setVgrow(content, Priority.ALWAYS);
        return card;
    }

    private VBox buildInputPanel() {
        inputField.setPromptText("Enter system input...");
        inputField.getStyleClass().add("modern-input");
        inputField.setTooltip(new Tooltip("Enter the required process input and press Enter or click Submit"));
        submitInputButton.getStyleClass().add("button-primary");

        HBox inputRow = new HBox(12, inputField, submitInputButton);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(inputField, Priority.ALWAYS);

        submitInputButton.setDisable(true);
        submitInputButton.setOnAction(event -> submitInputFromGUI());
        inputField.setOnAction(event -> submitInputFromGUI());

        VBox container = new VBox(10, new Label("USER INPUT REQUESTED"), inputRequestLabel, inputRow);
        container.getStyleClass().add("input-card");
        container.setPadding(new Insets(20));
        container.setVisible(true);
        container.setManaged(true);
        return container;
    }

    private HBox buildControls() {
        algorithmSelector.getItems().addAll("RR", "HRRN", "MLFQ");
        algorithmSelector.setTooltip(new Tooltip("Select the scheduling algorithm (RR, HRRN, or MLFQ)"));
        algorithmSelector.valueProperty().addListener((obs, oldVal, newVal) -> updateQuantumUsability());
        algorithmSelector.valueProperty().addListener((obs, oldVal, newVal) -> updateStartButtonState());

        rrQuantumField.textProperty().addListener((obs, oldVal, newVal) -> updateStartButtonState());
        rrQuantumField.setTooltip(new Tooltip(
                "Enter the time quantum for Round-Robin scheduling technique (must be a positive integer)"));

        startButton.getStyleClass().add("button-start");
        stepButton.getStyleClass().add("button-secondary");
        runPauseButton.getStyleClass().add("button-secondary");

        startButton.setDisable(true);
        startButton.setOnAction(event -> startSimulation());
        stepButton.setDisable(true);
        stepButton.setOnAction(event -> Scheduler.requestStep());
        runPauseButton.setDisable(true);
        runPauseButton.setOnAction(event -> toggleRunPause());

        HBox schedulerConfigGroup = new HBox(10,
                new Label("Scheduling Algorithm:"), algorithmSelector,
                quantumLabel, rrQuantumField);
        schedulerConfigGroup.setAlignment(Pos.CENTER_LEFT);
        quantumLabel.setDisable(true);
        rrQuantumField.setDisable(true);

        HBox executionGroup = new HBox(10,
                startButton, stepButton, runPauseButton,
                statusLabel);
        executionGroup.setAlignment(Pos.CENTER_LEFT);

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox controls = new HBox(20, schedulerConfigGroup, spacer, executionGroup);

        updateQuantumUsability();
        updateStartButtonState();

        controls.setAlignment(Pos.CENTER_LEFT);
        controls.getStyleClass().add("controls-bar");
        return controls;
    }

    private void updateQuantumUsability() {
        boolean isRR = "RR".equals(algorithmSelector.getValue());
        quantumLabel.setDisable(!isRR);
        rrQuantumField.setDisable(!isRR);
    }

    private void updateStartButtonState() {
        String algorithmSelected = algorithmSelector.getValue();
        startButton.setDisable(algorithmSelected == null || algorithmSelected.isEmpty()|| algorithmSelected.equals("RR") && parseRRQuantum() <= 0);
    }

    private HBox buildTopStatus() {
        timeLabel.getStyleClass().add("status-value");
        currentProcessLabel.getStyleClass().add("status-value");

        HBox timeBox = new HBox(10, new Label("TIME: "), timeLabel);
        HBox procBox = new HBox(10, new Label("CURRENT PROCESS: "), currentProcessLabel);

        HBox topStatus = new HBox(40, timeBox, procBox);
        topStatus.getStyleClass().add("status-header");
        return topStatus;
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
            return value;
        } catch (Exception ex) {
            return -1;
        }
    }

    private void refreshUI() {
        timeLabel.setText("" + Scheduler.getCurrentTimeSnapshot());

        Integer runningProcessID = Scheduler.getCurrentRunningProcessID();
        String runningValue = runningProcessID == null ? "None" : "P" + runningProcessID;
        currentProcessLabel.setText("" + runningValue);

        List<String> processStates = Scheduler.getProcessStateSnapshot();
        processStatesList.getItems().setAll(processStates);

        readyQueueList.getItems().setAll(Scheduler.getReadyQueueSnapshot());
        blockedQueueList.getItems().setAll(Scheduler.getBlockedQueueSnapshot());

        List<String> memorySnapshot = Memory.getMemorySnapshot();
        refreshMemoryHeatmap(memorySnapshot);

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

            statusLabel.setText("Status: Waiting for Input");
            Platform.runLater(inputField::requestFocus);
            return;
        }

        inputRequestLabel.setText("No process is waiting for input.");
        inputField.clear();
        inputField.setDisable(true);
        submitInputButton.setDisable(true);

        if (!simulationCompleted && startButton.isDisable()) {
            statusLabel.setText(Scheduler.isAutoRunEnabled() ? "Status: Running" : "Status: Ready to Step");
        }
    }

    private void refreshMemoryHeatmap(List<String> memorySnapshot) {
        memoryFlow.getChildren().clear();

        for (String block : memorySnapshot) {
            Text textNode = new Text(block + "\n");

            textNode.setFont(Font.font("JetBrains Mono", 13));

            if (block.contains("Free")) {
                textNode.setFill(Color.web("#5c6370"));
            } else if (block.contains("P1")) {
                textNode.setFill(Color.web("#98c379")); // Green for Process 1
            } else if (block.contains("P2")) {
                textNode.setFill(Color.web("#61afef")); // Blue for Process 2
            } else {
                textNode.setFill(Color.web("#d19a66")); // Orange for others
            }

            memoryFlow.getChildren().add(textNode);
        }
    }

    private double calculateProgress(String item) {
        try {
            if (item.contains("/")) {
                int openParen = item.lastIndexOf("(") + 1;
                int slash = item.indexOf("/");
                int closeParen = item.indexOf(")");

                double current = Double.parseDouble(item.substring(openParen, slash).trim());
                double total = Double.parseDouble(item.substring(slash + 1, closeParen).trim());

                return (total > 0) ? (current / total) : 0.0;
            }
        } catch (Exception e) {
            return 0.0;
        }
        return 0.0;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
