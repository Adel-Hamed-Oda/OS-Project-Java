import java.util.*;
import src.*;

public class Main {
    public static void main(String[] args) {
        Init(args);

        ArrayList<OS_Process> processes = Scheduler.convertjobPoolToProcesses();

        Scheduler.simulate_RR(processes, 2);
    }

    public static void Init(String[] args) {
        Memory_Refactored.initMemory();
        MutexManager.InitMutexes();
        Parser.initParser();
        
        Scheduler.arrival_times.addAll(PublicDomain.ARRIVAL_TIMES);

        for (String fileName : PublicDomain.FILE_NAMES) {
            ProcessController.AddNewProcess(fileName);
            Scheduler.burst_times.add(ProcessController.getInstructionCount(ProcessController.instructionTable.size() - 1));
            Scheduler.jobPool.offer(ProcessController.instructionTable.size() - 1);
        }
    }
}
