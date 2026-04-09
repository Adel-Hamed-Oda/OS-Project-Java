package memory;

public class PCB {
    private int processID;
    private String processState; // "Ready", "Running", "Blocked", "Finished"
    private int programCounter;
    private int lowerBoundary;
    private int upperBoundary;

    public PCB(int processID, String processState, int programCounter, int lowerBoundary, int upperBoundary) {
        this.processID = processID;
        this.processState = processState;
        this.programCounter = programCounter;
        this.lowerBoundary = lowerBoundary;
        this.upperBoundary = upperBoundary;
    }

    // Getters and Setters
    public int getProcessID() { return processID; }

    public String getProcessState() { return processState; }

    public void setProcessState(String state) { this.processState = state; }

    public int getProgramCounter() { return programCounter; }

    public void setProgramCounter(int pc) { this.programCounter = pc; }

    public int getLowerBoundary() { return lowerBoundary; }

    public void setLowerBoundary(int lower) { this.lowerBoundary = lower; }

    public int getUpperBoundary() { return upperBoundary; }

    public void setUpperBoundary(int upper) { this.upperBoundary = upper; }
    
}
