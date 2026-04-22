import java.util.*;

public class main {
    public static void main(String[] args) {
        Init(args);

        ArrayList<OS_Process> processes = Scheduler.convertjobPoolToProcesses();

        Scheduler.allProcesses.addAll(processes);
        // Scheduler.simulate_MLFQ(processes); // time quantum of 2
        if (PublicDomain.SCHEDULING_TECHNIQUE.equals("HRRN")) {
            Scheduler.simulate_HRRN(processes);
        } else if (PublicDomain.SCHEDULING_TECHNIQUE.equals("MLFQ")) {
            Scheduler.simulate_MLFQ(processes);
        } else if (PublicDomain.SCHEDULING_TECHNIQUE.equals("RR")) {
            Scheduler.simulate_RR(processes, PublicDomain.TIME_QUANTUM);
        } else {
            System.out.println("Unknown scheduling technique: " + PublicDomain.SCHEDULING_TECHNIQUE);
        }
        // Scheduler.simulate_HRRN(processes);
        // System.out.println(Scheduler.allProcesses.toString());
        // for(int i=0 ; i<Scheduler.allProcesses.size(); i++) {
        //     OS_Process p = Scheduler.allProcesses.get(i);
        //     System.out.println("Process " + p.getP_id() + ": Arrival Time = " + p.getArrival_time() + ", Burst Time = " + p.getBurst_time() + ", Executed Time = " + p.getExecuted_time());
        // }
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
