
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Scheduler {

    public static Queue<Integer> readyQueue = new LinkedList<>(); // should things from the waiting queue return to the
                                                                  // beginning or the end of the queue?
    public static Queue<Integer> waitingQueueInput = new LinkedList<>();
    public static Queue<Integer> waitingQueueOutput = new LinkedList<>();
    public static Queue<Integer> waitingQueueMemory = new LinkedList<>();
    public static Queue<Integer> jobPool = new LinkedList<>();

    final public static ArrayList<Integer> arrival_times = new ArrayList<>();
    final public static ArrayList<Integer> burst_times = new ArrayList<>();
    public static int current_time = 0;
    public static OS_Process current_process;
    private static int unblockedProcessID = -1;

    public static ArrayList<OS_Process> allProcesses = new ArrayList<>();

    public static void initializeSimulation() {
        readyQueue.clear();
        waitingQueueInput.clear();
        waitingQueueOutput.clear();
        waitingQueueMemory.clear();
        jobPool.clear();
        arrival_times.clear();
        burst_times.clear();
        allProcesses.clear();
        current_time = 0;
        current_process = null;
        unblockedProcessID = -1;

        ProcessController.instructionTable.clear();
        Memory.initMemory();
        MutexManager.InitMutexes();
        Parser.initParser();

        arrival_times.addAll(PublicDomain.ARRIVAL_TIMES);

        for (String fileName : PublicDomain.FILE_NAMES) {
            ProcessController.AddNewProcess(fileName);
            int processID = ProcessController.instructionTable.size() - 1;
            burst_times.add(ProcessController.getInstructionCount(processID));
            jobPool.offer(processID);
        }

        allProcesses.addAll(convertjobPoolToProcesses());
    }

    public static synchronized Integer getCurrentRunningProcessID() {
        return current_process == null ? null : current_process.getP_id();
    }

    public static synchronized int getCurrentTimeSnapshot() {
        return current_time;
    }

    public static synchronized List<String> getProcessStateSnapshot() {
        List<String> snapshot = new ArrayList<>();
        for (OS_Process process : allProcesses) {
            String state = inferState(process);
            String row = "P" + process.getP_id()
                    + " | " + state
                    + " | " + process.getExecuted_time() + "/" + process.getBurst_time();
            snapshot.add(row);
        }
        return snapshot;
    }

    private static String inferState(OS_Process process) {
        int processID = process.getP_id();

        if (process.getExecuted_time() >= process.getBurst_time()) {
            return ProcessState.Terminated.name();
        }

        if (current_process != null && processID == current_process.getP_id()) {
            return ProcessState.Running.name();
        }

        if (process.isBlocked() || waitingQueueInput.contains(processID) || waitingQueueOutput.contains(processID)
                || waitingQueueMemory.contains(processID)) {
            return ProcessState.Waiting.name();
        }

        if (readyQueue.contains(processID) || process.getArrival_time() <= current_time) {
            return ProcessState.Ready.name();
        }

        return "NotArrived";
    }

    public static int getCurrentProcessID() {
        if (current_process == null) {
            return -1;
        }
        return current_process.getP_id();
    }

    public static OS_Process getProcess(int processID) {
        for (OS_Process process : allProcesses) {
            if (process.getP_id() == processID) {
                return process;
            }
        }
        return null;
    }

    public static void blockProcessOnInput() {
        int currentProcessID = getCurrentProcessID();
        if (currentProcessID != -1) {
            waitingQueueInput.offer(currentProcessID);
        }
        current_process.setBlocked(true);
        ProcessController.setProcessState(currentProcessID, ProcessState.Waiting);
    }

    public static void blockProcessOnOutput() {
        int currentProcessID = getCurrentProcessID();
        if (currentProcessID != -1) {
            waitingQueueOutput.offer(currentProcessID);
        }
        current_process.setBlocked(true);
        ProcessController.setProcessState(currentProcessID, ProcessState.Waiting);
    }

    public static void blockProcessOnMemory() {
        int currentProcessID = getCurrentProcessID();
        if (currentProcessID != -1) {
            waitingQueueMemory.offer(currentProcessID);
        }
        current_process.setBlocked(true);
        ProcessController.setProcessState(currentProcessID, ProcessState.Waiting);
    }

    public static int unblockProcessOnInput() {
        if (!waitingQueueInput.isEmpty()) {
            int processID = waitingQueueInput.poll();
            unblockedProcessID = processID;

            return processID;
        }
        return -1;
    }

    public static int unblockProcessOnOutput() {
        if (!waitingQueueOutput.isEmpty()) {
            int processID = waitingQueueOutput.poll();
            unblockedProcessID = processID;
            return processID;
        }
        return -1;
    }

    public static int unblockProcessOnMemory() {
        if (!waitingQueueMemory.isEmpty()) {
            int processID = waitingQueueMemory.poll();
            unblockedProcessID = processID;
            return processID;
        }
        return -1;
    }

    public static ArrayList<OS_Process> convertjobPoolToProcesses() {
        ArrayList<OS_Process> processes = new ArrayList<>();
        ArrayList<Integer> jobPoolArr = new ArrayList<>(jobPool);
        for (int i = 0; i < jobPoolArr.size(); i++) {
            int p_id = jobPoolArr.get(i);
            int arrival_time = arrival_times.get(i);
            int burst_time = burst_times.get(i);
            processes.add(new OS_Process(p_id, arrival_time, burst_time));
        }
        return processes;
    }

    public static int get_HRRN(ArrayList<OS_Process> processes) {
        int n = processes.size();
        int max_index = -1;
        double max_response_ratio = -1;

        for (int i = 0; i < n; i++) {
            if (processes.get(i).getArrival_time() <= current_time) {
                double response_ratio = (double) (current_time - processes.get(i).getArrival_time()
                        + processes.get(i).getBurst_time()) / processes.get(i).getBurst_time();
                if (response_ratio > max_response_ratio) {
                    max_response_ratio = response_ratio;
                    max_index = i;
                }
            }
        }
        return max_index;
    }

    // For testing purposes
    public static void printReadyQueue() {
        System.out.print("Ready Queue: ");
        for (Integer processID : readyQueue) {
            System.out.print(processID + " ");
        }
        System.out.println();
        System.out.println("----End of Ready Queue----");
    }

    public static void printRRAndReadyQueues(Queue<OS_Process> RRQueue) {
        System.out.print("RR Queue: ");
        for (OS_Process process : RRQueue) {
            System.out.print(process.getP_id() + " ");
        }
        System.out.println();

        System.out.print("Ready Queue: ");
        for (Integer processID : readyQueue) {
            System.out.print(processID + " ");
        }
        System.out.println();

        System.out.print("Waiting Queue Input: ");
        for (Integer processID : waitingQueueInput) {
            System.out.print(processID + " ");
        }
        System.out.println();
        System.out.println("-------------------------");

    }

    public static void updateReadyQueue(ArrayList<OS_Process> processes) {
        for (OS_Process process : processes) {
            if (process.getArrival_time() <= current_time && !process.is_in_ready_queue()
                    && process != current_process) {
                readyQueue.offer(process.getP_id());
                process.set_in_ready_queue(true);
                ProcessController.setProcessState(process.getP_id(), ProcessState.Ready);
            }
        }
    }

    public static void simulate_HRRN(ArrayList<OS_Process> processes) {
        processes.sort((p1, p2) -> Integer.compare(p1.getArrival_time(), p2.getArrival_time()));
        current_time = 0;

        while (!processes.isEmpty()) {
            updateReadyQueue(processes);
            int index = get_HRRN(processes);
            int processId = processes.get(index).getP_id();
            if (index != -1) {
                // This line makes the process NEW after it was ready and in description , we
                // only need ready.
                if (!Memory.tryLoadProcess(processId, false)) {
                    System.out.println("Error: Not enough memory to load process " + processId);
                    return;
                }

                current_process = processes.get(index);
                readyQueue.remove(current_process.getP_id());
                processes.remove(index);
                ProcessController.setProcessState(current_process.getP_id(), ProcessState.Running);

                for (int i = 0; i < current_process.getBurst_time(); i++) {
                    updateReadyQueue(processes);
                    Memory.printProcess(processId);
                    System.out.println("Process " + current_process.getP_id() + " is running at time " + current_time);

                    int currectPC = Memory.getPC(processId);
                    String instruction = Memory.getInstruction(processId, currectPC);
                    Parser.parse(instruction);

                    Memory.setPC(processId, currectPC + 1);

                    current_time++;
                }

                current_process.set_Executed_time(current_process.getBurst_time());
                ProcessController.setProcessState(current_process.getP_id(), ProcessState.Terminated);

                System.out.println("Process " + current_process.getP_id() + " completed at time " + current_time);
                current_process = null;
            } else {
                current_time++;
            }
        }
    }

    public static void simulate_RR(ArrayList<OS_Process> processes, int time_quantum) {
        processes.sort((p1, p2) -> Integer.compare(p1.getArrival_time(), p2.getArrival_time()));
        current_time = 0;
        Queue<OS_Process> RRQueue = new LinkedList<>();

        while (!processes.isEmpty() || !RRQueue.isEmpty()) {
            if (RRQueue.isEmpty()) {
                if (processes.get(0).getArrival_time() > current_time) {
                    current_time++;
                    continue;
                } else {
                    RRQueue.offer(processes.get(0));
                    readyQueue.offer(processes.get(0).getP_id());

                    if (!Memory.tryLoadProcess(processes.get(0).getP_id(), false)) {
                        System.out.println(
                                "Error: For some reason I couldn't load process " + processes.get(0).getP_id());
                    }

                    processes.remove(0);
                }
            }

            current_process = RRQueue.poll();
            readyQueue.remove(current_process.getP_id());
            ProcessController.setProcessState(current_process.getP_id(), ProcessState.Running);

            int execution_time = Math.min(time_quantum,
                    current_process.getBurst_time() - current_process.getExecuted_time());

            for (int i = 0; i < execution_time; i++) {
                if (current_process.isBlocked() == true) {
                    break;
                }

                if (!Memory.processExistsInMemory(current_process.getP_id())) {
                    Memory.tryLoadProcess(current_process.getP_id(), true);
                }

                Memory.printMemory();

                int currectPC = Memory.getPC(current_process.getP_id());
                String instruction = Memory.getInstruction(current_process.getP_id(), currectPC);
                System.out.println("Process " + current_process.getP_id() + " is running at time " + current_time
                        + " executing instruction: " + instruction);

                Parser.parse(instruction);
                Memory.setPC(current_process.getP_id(), currectPC + 1);

                if (unblockedProcessID != -1) {
                    OS_Process unblockedProcess = getProcess(unblockedProcessID);
                    if (unblockedProcess != null) {
                        RRQueue.offer(unblockedProcess);
                        readyQueue.offer(unblockedProcess.getP_id());
                        unblockedProcess.setBlocked(false);
                        ProcessController.setProcessState(unblockedProcessID, ProcessState.Ready);
                    }
                    unblockedProcessID = -1;
                }

                current_time++;
                current_process.set_Executed_time(current_process.getExecuted_time() + 1);
                if (!processes.isEmpty() && processes.get(0).getArrival_time() <= current_time) {
                    RRQueue.offer(processes.get(0));
                    readyQueue.offer(processes.get(0).getP_id());
                    Memory.tryLoadProcess(processes.get(0).getP_id(), false);
                    processes.remove(0);
                }
            }
            if (current_process.isBlocked() == true) {
                continue;
            }
            if (current_process.getExecuted_time() < current_process.getBurst_time()) {
                RRQueue.offer(current_process);
                readyQueue.offer(current_process.getP_id());
                ProcessController.setProcessState(current_process.getP_id(), ProcessState.Ready);
            } else {
                System.out.println("Process " + current_process.getP_id() + " completed at time " + current_time);
                ProcessController.setProcessState(current_process.getP_id(), ProcessState.Terminated);
            }

            current_process = null;
        }

    }

    public static int getPQIndex(List<Queue<OS_Process>> PQs) {
        for (int i = 0; i < PQs.size(); i++) {
            if (!PQs.get(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    public static void simulate_MLFQ(ArrayList<OS_Process> processes) {
        processes.sort((p1, p2) -> Integer.compare(p1.getArrival_time(), p2.getArrival_time()));
        current_time = 0;

        List<Queue<OS_Process>> PQs = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            PQs.add(new LinkedList<>());
        }

        while (!processes.isEmpty() || getPQIndex(PQs) > -1) {
            int pqIndex = getPQIndex(PQs);
            if (pqIndex == -1) {
                if (processes.get(0).getArrival_time() > current_time) {
                    current_time++;
                    continue;
                } else {
                    PQs.get(0).offer(processes.get(0));
                    readyQueue.offer(processes.get(0).getP_id());
                    pqIndex = 0;

                    if (!Memory.tryLoadProcess(processes.get(0).getP_id(), false)) {
                        System.out.println(
                                "Error: For some reason I couldn't load process " + processes.get(0).getP_id());
                    }

                    processes.remove(0);
                }
            }

            current_process = PQs.get(pqIndex).poll();
            readyQueue.remove(current_process.getP_id());
            ProcessController.setProcessState(current_process.getP_id(), ProcessState.Running);

            int execution_time = Math.min(current_process.getBurst_time() - current_process.getExecuted_time(),
                    (int) Math.pow(2, pqIndex));

            for (int i = 0; i < execution_time; i++) {
                if (current_process.isBlocked() == true) {
                    break;
                }

                if (!Memory.processExistsInMemory(current_process.getP_id())) {
                    Memory.tryLoadProcess(current_process.getP_id(), true);
                }

                Memory.printMemory();

                int currectPC = Memory.getPC(current_process.getP_id());
                String instruction = Memory.getInstruction(current_process.getP_id(), currectPC);
                System.out.println("Process " + current_process.getP_id() + " is running at time " + current_time
                        + " executing instruction: " + instruction);

                Parser.parse(instruction);
                Memory.setPC(current_process.getP_id(), currectPC + 1);

                if (unblockedProcessID != -1) {
                    OS_Process unblockedProcess = getProcess(unblockedProcessID);
                    if (unblockedProcess != null) {
                        PQs.get(0).offer(unblockedProcess);
                        readyQueue.offer(unblockedProcess.getP_id());
                        unblockedProcess.setBlocked(false);
                        ProcessController.setProcessState(unblockedProcessID, ProcessState.Ready);
                    }
                    unblockedProcessID = -1;
                }

                current_time++;
                current_process.set_Executed_time(current_process.getExecuted_time() + 1);
                if (!processes.isEmpty() && processes.get(0).getArrival_time() <= current_time) {
                    PQs.get(0).offer(processes.get(0));
                    readyQueue.offer(processes.get(0).getP_id());
                    Memory.tryLoadProcess(processes.get(0).getP_id(), false);
                    processes.remove(0);
                }
            }
            if (current_process.isBlocked() == true) {
                continue;
            }
            if (current_process.getExecuted_time() < current_process.getBurst_time()) {
                if (pqIndex < 3)
                    pqIndex++;
                PQs.get(pqIndex).offer(current_process);
                readyQueue.offer(current_process.getP_id());
                ProcessController.setProcessState(current_process.getP_id(), ProcessState.Ready);
            } else {
                System.out.println("Process " + current_process.getP_id() + " completed at time " + current_time);
                ProcessController.setProcessState(current_process.getP_id(), ProcessState.Terminated);
            }

            current_process = null;
        }

    }

    public static void main(String[] args) {

        // simulate_HRRN(null);

    }
}