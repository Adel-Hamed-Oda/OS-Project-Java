
import java.util.ArrayList;
import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Dashboard extends Application {

    private final Label timeLabel = new Label("Time: 0");
    private final Label currentProcessLabel = new Label("Current Process: None");
    private final Label statusLabel = new Label("Status: Idle");

    private final TableView<MemoryRow> memoryTable = new TableView<>();
    private final ListView<String> processStatesList = new ListView<>();
    private final ListView<String> readyQueueList = new ListView<>();
    private final ListView<String> blockedQueueList = new ListView<>();

    private final ComboBox<String> algorithmSelector = new ComboBox<>();
    private final Label quantumLabel = new Label("Quantum Time:");
    private final TextField rrQuantumField = new TextField("");
    private final Button startButton = new Button("Start Simulation");
    private final Button stepButton = new Button("Step");
    private final Button runPauseButton = new Button("Run");
    private final Button endButton = new Button("End");
    private final List<CheckBox> programSelectionChecks = new ArrayList<>();
    private final List<TextField> programArrivalFields = new ArrayList<>();
    private final List<String> availablePrograms = new ArrayList<>();

    private final Label inputRequestLabel = new Label("No process is waiting for input.");
    private final TextField inputField = new TextField();
    private final Button submitInputButton = new Button("Submit Input");
    private VBox inputPanel;

    private VBox outPutPanel;
    private final TextArea outputArea1 = new TextArea();
    private final TextArea outputArea2 = new TextArea();
    private final TextArea outputArea3 = new TextArea();

    private List<String> lastMemorySnapshot = new ArrayList<>();

    private Timeline uiRefreshTimeline;
    private volatile boolean simulationCompleted = false;
    private volatile boolean simulationEndedByUser = false;

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");

        // Top Section: Program Configuration and Scheduling
        SplitPane topPane = new SplitPane(buildProgramConfigPanel(), buildSchedulingPanel());
        topPane.getStyleClass().add("top-split-pane");
        topPane.setDividerPositions(0.58);

        VBox topContainer = new VBox(15, topPane, buildExecutionPanel());
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
        TableColumn<MemoryRow, String> addressColumn = new TableColumn<>("Address");
        addressColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().address()));
        addressColumn.setSortable(false);
        addressColumn.setReorderable(false);
        addressColumn.setMinWidth(68);
        addressColumn.setPrefWidth(72);
        addressColumn.setMaxWidth(82);

        TableColumn<MemoryRow, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().variable()));
        nameColumn.setSortable(false);
        nameColumn.setReorderable(false);
        nameColumn.setMinWidth(120);
        nameColumn.setPrefWidth(130);

        TableColumn<MemoryRow, String> valueColumn = new TableColumn<>("Value");
        valueColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().value()));
        valueColumn.setSortable(false);
        valueColumn.setReorderable(false);
        valueColumn.setMinWidth(220);
        valueColumn.setPrefWidth(230);

        TableColumn<MemoryRow, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().type()));
        typeColumn.setSortable(false);
        typeColumn.setReorderable(false);
        typeColumn.setMinWidth(100);
        typeColumn.setPrefWidth(100);
        typeColumn.setMaxWidth(100);

        memoryTable.getColumns().setAll(java.util.Arrays.asList(addressColumn, nameColumn, valueColumn, typeColumn));
        memoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        memoryTable.getStyleClass().add("memory-table");

        addressColumn.setCellFactory(column -> createDefaultMemoryCell());
        nameColumn.setCellFactory(column -> createDefaultMemoryCell());
        valueColumn.setCellFactory(column -> createDefaultMemoryCell());

        typeColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                    setTooltip(null);
                    return;
                }

                boolean missing = item == null || item.trim().isEmpty();
                if (missing) {
                    setText("EMPTY");
                    setStyle("-fx-text-fill: #9192947b; -fx-font-style: italic;");
                    setTooltip(new Tooltip("No value"));
                    return;
                }

                setText(item);
                setTooltip(null);
                if ("Free".equals(item)) {
                    setStyle("-fx-text-fill: #4dabf7;");
                } else if ("Variable".equals(item)) {
                    setStyle("-fx-text-fill: #98c379;");
                } else if ("Instruction".equals(item)) {
                    setStyle("-fx-text-fill: #ea80ee;");
                } else {
                    setStyle("-fx-text-fill: #d19a66;");
                }
            }
        });

        VBox memoryBox = createCard("System Memory Map", memoryTable);
        memoryBox.setPadding(new Insets(0, 20, 20, 10));

        SplitPane body = new SplitPane(leftPane, memoryBox);
        body.setDividerPositions(0.3);
        root.setCenter(body);

        // Bottom Section: Input and Output Panels
        inputPanel = buildInputPanel();
        outPutPanel = buildOutputPanel();
        SplitPane IOPane = new SplitPane(inputPanel, outPutPanel);
        IOPane.getStyleClass().add("io-split-pane");
        IOPane.setDividerPositions(0.295);
        IOPane.setMaxHeight(30);
        IOPane.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
        root.setBottom(IOPane);

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

    private TableCell<MemoryRow, String> createDefaultMemoryCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setText(null);
                    setStyle("");
                    setTooltip(null);
                    return;
                }

                boolean missing = item == null || item.trim().isEmpty();
                if (missing) {
                    setText("EMPTY");
                    setStyle("-fx-text-fill: #656666; -fx-font-style: italic;");
                    setTooltip(new Tooltip("No value"));
                    return;
                }

                setText(item);
                setStyle("");
                setTooltip(null);
            }
        };
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

    private VBox buildOutputPanel() {
        Label mainHeader = new Label("OUTPUT PANEL");
        mainHeader.getStyleClass().add("card-header");

        VBox p1Box = createOutputSection("Program 1", outputArea1);
        VBox p2Box = createOutputSection("Program 2", outputArea2);
        VBox p3Box = createOutputSection("Program 3", outputArea3);

        HBox outputSections = new HBox(12, p1Box, p2Box, p3Box);
        HBox.setHgrow(p1Box, Priority.ALWAYS);
        HBox.setHgrow(p2Box, Priority.ALWAYS);
        HBox.setHgrow(p3Box, Priority.ALWAYS);
        VBox.setVgrow(outputSections, Priority.ALWAYS);

        VBox container = new VBox(10, mainHeader, outputSections);
        container.getStyleClass().add("input-card");
        container.setPadding(new Insets(20));

        return container;
    }

    private VBox createOutputSection(String title, TextArea textArea) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("output-section-title");

        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.getStyleClass().add("output-text-area");
        VBox.setVgrow(textArea, Priority.ALWAYS);

        VBox section = new VBox(5, titleLabel, textArea);
        section.setPadding(new Insets(8));
        return section;
    }

    private VBox buildSchedulingPanel() {
        algorithmSelector.getItems().addAll("RR", "HRRN", "MLFQ");
        algorithmSelector.setTooltip(new Tooltip("Select the scheduling algorithm (RR, HRRN, or MLFQ)"));
        algorithmSelector.valueProperty().addListener((obs, oldVal, newVal) -> updateQuantumUsability());
        algorithmSelector.valueProperty().addListener((obs, oldVal, newVal) -> updateStartButtonState());

        rrQuantumField.textProperty().addListener((obs, oldVal, newVal) -> updateStartButtonState());
        rrQuantumField.setTooltip(new Tooltip(
                "Enter the time quantum for Round-Robin scheduling technique (must be a positive integer)"));

        Label header = new Label("SCHEDULING CONFIGURATION");
        header.getStyleClass().add("card-header");

        HBox schedulerConfigGroup = new HBox(10,
                new Label("Scheduling Algorithm:"), algorithmSelector,
                quantumLabel, rrQuantumField);
        schedulerConfigGroup.setAlignment(Pos.CENTER_LEFT);

        quantumLabel.setDisable(true);
        rrQuantumField.setDisable(true);

        updateQuantumUsability();
        updateStartButtonState();

        VBox panel = new VBox(8, header, schedulerConfigGroup);
        panel.getStyleClass().add("scheduling-panel");
        panel.setPadding(new Insets(12));
        return panel;
    }

    private HBox buildExecutionPanel() {
        startButton.getStyleClass().add("button-start");
        stepButton.getStyleClass().add("button-secondary");
        runPauseButton.getStyleClass().add("button-secondary");
        endButton.getStyleClass().add("button-end");

        startButton.setDisable(true);
        startButton.setOnAction(event -> startSimulation());
        stepButton.setDisable(true);
        stepButton.setOnAction(event -> Scheduler.requestStep());
        runPauseButton.setDisable(true);
        runPauseButton.setOnAction(event -> toggleRunPause());
        endButton.setDisable(true);
        endButton.setOnAction(event -> endSimulation());

        timeLabel.getStyleClass().add("status-value");
        currentProcessLabel.getStyleClass().add("status-value");

        HBox runtimeInfoGroup = new HBox(10,
                new Label("TIME:"), timeLabel,
                new Label("CURRENT PROCESS:"), currentProcessLabel);
        runtimeInfoGroup.setAlignment(Pos.CENTER_LEFT);

        HBox rightControlsGroup = new HBox(10,
            startButton, endButton, stepButton, runPauseButton,
            statusLabel);
        rightControlsGroup.setAlignment(Pos.CENTER_RIGHT);

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox executionGroup = new HBox(10,
                runtimeInfoGroup,
            spacer,
            rightControlsGroup);
        executionGroup.setAlignment(Pos.CENTER_LEFT);

        Memory.initMemory();
        List<String> initialMemorySnapshot = Memory.getMemorySnapshot();
        refreshMemoryTable(initialMemorySnapshot);

        executionGroup.getStyleClass().add("controls-bar");
        return executionGroup;
    }

    private VBox buildProgramConfigPanel() {
        availablePrograms.clear();
        availablePrograms.addAll(PublicDomain.FILE_NAMES);

        programSelectionChecks.clear();
        programArrivalFields.clear();

        Label header = new Label("PROGRAM CONFIGURATION");
        header.getStyleClass().add("card-header");

        HBox selectionRow = new HBox(12);
        selectionRow.getStyleClass().add("program-config-row");

        HBox arrivalRow = new HBox(12);
        arrivalRow.getStyleClass().add("program-config-row");

        for (int i = 0; i < availablePrograms.size(); i++) {
            String fileName = availablePrograms.get(i);

            CheckBox includeProgram = new CheckBox(fileName);
            includeProgram.setSelected(false);
            includeProgram.getStyleClass().add("program-config-check");

            TextField arrivalField = new TextField("");
            arrivalField.getStyleClass().add("program-arrival-field");
            arrivalField.setPromptText("Non-negative Integer");
            arrivalField.setTooltip(new Tooltip("Enter a valid arrival time (non-negative integer, e.g. 0, 1, 2)."));
            arrivalField.textProperty().addListener((obs, oldVal, newVal) -> updateStartButtonState());
            arrivalField.setDisable(true);

            includeProgram.selectedProperty().addListener((obs, oldVal, newVal) -> {
                arrivalField.setDisable(!newVal);
                updateStartButtonState();
            });

            VBox selectionCell = new VBox(includeProgram);
            selectionCell.getStyleClass().add("program-config-cell");
            selectionCell.setAlignment(Pos.CENTER_LEFT);
            selectionCell.setPadding(new Insets(8));
            selectionCell.setPrefWidth(170);

            VBox arrivalCell = new VBox(arrivalField);
            arrivalCell.getStyleClass().add("program-config-cell");
            arrivalCell.setAlignment(Pos.CENTER_LEFT);
            arrivalCell.setPadding(new Insets(8));
            arrivalCell.setPrefWidth(170);

            programSelectionChecks.add(includeProgram);
            programArrivalFields.add(arrivalField);

            selectionRow.getChildren().add(selectionCell);
            arrivalRow.getChildren().add(arrivalCell);
        }

        Label selectionLabel = new Label("Program Selection");
        selectionLabel.getStyleClass().addAll("program-config-header", "program-config-row-title");

        Label arrivalLabel = new Label("Arrival Time");
        arrivalLabel.getStyleClass().addAll("program-config-header", "program-config-row-title");

        HBox selectionLine = new HBox(12, selectionLabel, selectionRow);
        selectionLine.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(selectionRow, Priority.ALWAYS);

        HBox arrivalLine = new HBox(12, arrivalLabel, arrivalRow);
        arrivalLine.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(arrivalRow, Priority.ALWAYS);

        VBox rows = new VBox(8, selectionLine, arrivalLine);

        VBox panel = new VBox(8, header, rows);
        panel.getStyleClass().add("program-config-panel");
        panel.setPadding(new Insets(12));
        return panel;
    }

    private void updateQuantumUsability() {
        boolean isRR = "RR".equals(algorithmSelector.getValue());
        quantumLabel.setDisable(!isRR);
        rrQuantumField.setDisable(!isRR);
    }

    private void updateStartButtonState() {
        String algorithmSelected = algorithmSelector.getValue();
        startButton.setDisable(algorithmSelected == null || algorithmSelected.isEmpty()
                || algorithmSelected.equals("RR") && parseRRQuantum() <= 0
                || !hasValidProgramConfiguration());
    }

    private boolean hasValidProgramConfiguration() {
        boolean atLeastOneSelected = false;

        for (int i = 0; i < programSelectionChecks.size(); i++) {
            if (!programSelectionChecks.get(i).isSelected()) {
                continue;
            }

            atLeastOneSelected = true;
            try {
                int arrival = Integer.parseInt(programArrivalFields.get(i).getText().trim());
                if (arrival < 0) {
                    return false;
                }
            } catch (Exception ex) {
                return false;
            }
        }

        return atLeastOneSelected;
    }

    private boolean applyProgramConfiguration() {
        ArrayList<String> selectedFiles = new ArrayList<>();
        ArrayList<Integer> selectedArrivals = new ArrayList<>();

        for (int i = 0; i < programSelectionChecks.size(); i++) {
            if (!programSelectionChecks.get(i).isSelected()) {
                continue;
            }

            String arrivalText = programArrivalFields.get(i).getText().trim();
            int arrival;
            try {
                arrival = Integer.parseInt(arrivalText);
            } catch (Exception ex) {
                statusLabel.setText("Status: Invalid arrival time for " + availablePrograms.get(i));
                return false;
            }

            if (arrival < 0) {
                statusLabel.setText("Status: Arrival time must be a non-negative integer");
                return false;
            }

            selectedFiles.add(availablePrograms.get(i));
            selectedArrivals.add(arrival);
        }

        if (selectedFiles.isEmpty()) {
            statusLabel.setText("Status: Select at least one program");
            return false;
        }

        PublicDomain.configurePrograms(selectedFiles, selectedArrivals);
        return true;
    }

    private void setProgramConfigurationDisabled(boolean disabled) {
        for (int i = 0; i < programSelectionChecks.size(); i++) {
            CheckBox checkBox = programSelectionChecks.get(i);
            TextField textField = programArrivalFields.get(i);

            checkBox.setDisable(disabled);
            textField.setDisable(disabled || !checkBox.isSelected());
        }
    }

    private void resetConfigurationSelections() {
        algorithmSelector.getSelectionModel().clearSelection();
        rrQuantumField.clear();

        for (int i = 0; i < programSelectionChecks.size(); i++) {
            CheckBox checkBox = programSelectionChecks.get(i);
            TextField arrivalField = programArrivalFields.get(i);

            checkBox.setSelected(false);
            arrivalField.clear();
            arrivalField.setDisable(true);
        }

        updateQuantumUsability();
        updateStartButtonState();
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
        if (!applyProgramConfiguration()) {
            return;
        }

        setProgramConfigurationDisabled(true);
        startButton.setDisable(true);
        stepButton.setDisable(false);
        runPauseButton.setDisable(false);
        endButton.setDisable(false);
        runPauseButton.setText("Run");
        simulationCompleted = false;
        simulationEndedByUser = false;
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

    private void endSimulation() {
        if (!startButton.isDisable()) {
            return;
        }

        simulationEndedByUser = true;
        Scheduler.requestSimulationStop();

        if (SystemCalls.isAwaitingInput()) {
            SystemCalls.provideInput("");
        }

        statusLabel.setText("Status: Terminated by User");
        stepButton.setDisable(true);
        runPauseButton.setDisable(true);
        endButton.setDisable(true);
        runPauseButton.setText("Run");
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
        refreshMemoryTable(memorySnapshot);

        refreshInputWindowState();

        if (simulationCompleted) {
            statusLabel.setText(simulationEndedByUser ? "Status: Terminated by User" : "Status: Completed");
            startButton.setDisable(false);
            stepButton.setDisable(true);
            runPauseButton.setDisable(true);
            endButton.setDisable(true);
            setProgramConfigurationDisabled(false);
            resetConfigurationSelections();
            runPauseButton.setText("Run");
            simulationCompleted = false;
            simulationEndedByUser = false;
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

    private void refreshMemoryTable(List<String> memorySnapshot) {
        if (memorySnapshot.equals(lastMemorySnapshot)) {
            return;
        }

        List<MemoryRow> rows = new ArrayList<>(memorySnapshot.size());
        for (String line : memorySnapshot) {
            rows.add(parseMemoryRow(line));
        }
        memoryTable.getItems().setAll(rows);
        lastMemorySnapshot = new ArrayList<>(memorySnapshot);
    }

    private MemoryRow parseMemoryRow(String line) {
        int addressMarker = line.indexOf(':');
        if (addressMarker < 0) {
            return new MemoryRow("", "", line, "");
        }

        String addressPart = line.substring(0, addressMarker).replace("Address", "").trim();
        String payload = line.substring(addressMarker + 1).trim();

        int firstComma = payload.indexOf(',');
        int lastComma = payload.lastIndexOf(',');

        if (firstComma < 0 || lastComma < 0 || firstComma == lastComma) {
            return new MemoryRow(addressPart, "", payload, "");
        }

        String variablePart = payload.substring(0, firstComma).trim();
        String valuePart = payload.substring(firstComma + 1, lastComma).trim();
        String typePart = payload.substring(lastComma + 1).trim();

        return new MemoryRow(addressPart, variablePart, valuePart, typePart);
    }

    private record MemoryRow(String address, String variable, String value, String type) {
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
