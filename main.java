import java.util.*;

import src.*;

public class Main {
    public static void main(String[] args) {
        Init(args);

        ArrayList<OS_Process> processes = Scheduler.convertjobPoolToProcesses();

        Scheduler.simulate_HRRN(processes);
    }

    public static void Init(String[] args) {
        Memory_Refactored.initMemory();
        MutexManager.InitMutexes();
        Parser.initParser();

        /* String[] fileNames = new String[args.length / 2];
        for (int i = 0; i < args.length; i += 2) {
            fileNames[i / 2] = args[i];
        } */
        String[] fileNames = new String[] {"Program_1.txt", "Program_2.txt", "Program_3.txt"};

        /* for (int i = 1; i < args.length; i += 2) {
            Scheduler.arrival_times.add(Integer.parseInt(args[i]));
        } */
        Scheduler.arrival_times.addAll(Arrays.asList(0, 1, 4));

        for (String fileName : fileNames) {
            ProcessController.AddNewProcess(fileName);
            Scheduler.burst_times.add(ProcessController.getInstructionCount(ProcessController.instructionTable.size() - 1));
            Scheduler.jobPool.offer(ProcessController.instructionTable.size() - 1);
        }
    }
}
