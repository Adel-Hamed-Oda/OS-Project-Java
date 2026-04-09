package memory;

public class PCB {
    public enum ProcessState {
        Ready,
        Running,
        Waiting,
        Terminated
    }

    private static int idCounter = 0; // Static counter to assign unique IDs

    public int processID;
    // since we have java it's better to make it an enum, e7na msh fe C
    public ProcessState processState;
    public int programCounter;
    public int lowerBoundary;
    public int upperBoundary;

    // processID is auto-assigned based on the static counter
    public PCB(ProcessState processState, int programCounter, int lowerBoundary, int upperBoundary) {
        this.processID = idCounter++;
        this.processState = processState;
        this.programCounter = programCounter;
        this.lowerBoundary = lowerBoundary;
        this.upperBoundary = upperBoundary;
    }
}