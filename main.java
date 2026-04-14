import java.util.*;
import memory.Memory;
import mutex.MutexManager;
import os_process.ProcessController;
import scheduler.*;

public class Main {

    // example: file1 loadtime1 file2 loadtime2 file3 loadtime3
    public static void main(String[] args) {
        Init(args);

        ArrayList<OS_Process> processes = Scheduler.convertjobPoolToProcesses();

        Scheduler.simulate_HRRN(processes);
    }

    public static void Init(String[] args) {
        Memory.Init_Memory();
        MutexManager.InitMutexes();

        String[] fileNames = new String[args.length / 2];
        for (int i = 0; i < args.length; i += 2) {
            fileNames[i / 2] = args[i];
        }

        for (int i = 1; i < args.length; i += 2) {
            Scheduler.arrival_times.add(Integer.parseInt(args[i]));
        }

        for (String fileName : fileNames) {
            ProcessController.AddNewProcess(fileName);
            Scheduler.burst_times.add(ProcessController.getInstructionCount(ProcessController.processTable.size() - 1));
            Scheduler.jobPool.offer(ProcessController.processTable.size() - 1);
        }
    }
}
