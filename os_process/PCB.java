package os_process;

public class PCB {
    private static int idCounter = 0;

    public int processID;
    public ProcessState processState;
    public int programCounter;
    public int lowerBoundary;
    public int upperBoundary;

    public PCB(ProcessState processState, int programCounter, int lowerBoundary, int upperBoundary) {
        this.processID = idCounter++;
        this.processState = processState;
        this.programCounter = programCounter;
        this.lowerBoundary = lowerBoundary;
        this.upperBoundary = upperBoundary;
    }
}