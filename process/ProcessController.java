package process;

import java.util.ArrayList;

public class ProcessController {
    // this is just a list to reference all processes by their id, by no means is this the list of loaded processes
    public static ArrayList<PCB> processTable = new ArrayList<>();

    public static void AddNewProcess(String[] instructions) {
        PCB newProcess = new PCB(PCB.ProcessState.Ready, 0, -1, -1);
        processTable.add(newProcess);
    }
    
    public static PCB getProcess(int processID) {
        // This is a placeholder. You would need to implement a way to retrieve the PCB based on the processID.
        for (PCB pcb : processTable) {
            if (pcb.processID == processID) {
                return pcb;
            }
        }
        return null;
    }
}
