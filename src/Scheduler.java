package src;

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
    }

    public static void blockProcessOnOutput() {
        int currentProcessID = getCurrentProcessID();
        if (currentProcessID != -1) {
            waitingQueueOutput.offer(currentProcessID);
        }
        current_process.setBlocked(true);
    }

    public static void blockProcessOnMemory() {
        int currentProcessID = getCurrentProcessID();
        if (currentProcessID != -1) {
            waitingQueueMemory.offer(currentProcessID);
        }
        current_process.setBlocked(true);
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
            }
        }
    }

    public static void simulate_HRRN(ArrayList<OS_Process> processes) {
        processes.sort((p1, p2) -> Integer.compare(p1.getArrival_time(), p2.getArrival_time()));
        current_time = 0;

        while (!processes.isEmpty()) {
            updateReadyQueue(processes);
            int index = get_HRRN(processes);
            // int processId = processes.get(index).getP_id();
            if (index != -1) {
                // if (!Memory_Refactored.tryLoadProcess(processId, false)) {
                // System.out.println("Error: Not enough memory to load process " + processId);
                // return;
                // }

                current_process = processes.get(index);
                updateReadyQueue(processes);

                for (int i = 0; i < current_process.getBurst_time(); i++) {
                    // Memory_Refactored.printProcess(processId);
                    System.out.println("Process " + current_process.getP_id() + " is running at time " + current_time);

                    // int currectPC = Memory_Refactored.getPC(processId);
                    // String instruction = Memory_Refactored.getInstruction(processId, currectPC);
                    // Parser.parse(instruction);

                    // Memory_Refactored.setPC(processId, currectPC + 1);

                    current_time++;
                }

                current_process.set_Executed_time(current_process.getBurst_time());
                readyQueue.remove(current_process.getP_id());
                processes.remove(index);

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
            // For testing purposes
            // System.out.println("Queue state at start of RR iteration (time " +
            // current_time + "):");
            // printRRAndReadyQueues(RRQueue);

            if (RRQueue.isEmpty()) {
                if (processes.get(0).getArrival_time() > current_time) {
                    current_time++;
                    continue;
                } else {
                    RRQueue.offer(processes.get(0));
                    readyQueue.offer(processes.get(0).getP_id());
                    processes.remove(0);
                }
            }

            current_process = RRQueue.poll();
            // int processId = current_process.getP_id();
            // OS_Process dead_current_process=current_process;
            // if (!Memory_Refactored.tryLoadProcess(processId, true)) {
            // System.out.println("Error: Not enough memory to load process " + processId);
            // return;
            // }

            readyQueue.remove(current_process.getP_id());
            int execution_time = Math.min(time_quantum,
                    current_process.getBurst_time() - current_process.getExecuted_time());

            // For testing purposes
            // if (current_time == 2) {
            // System.out.println("Blocking process " + current_process.getP_id() + " on
            // input at time " + current_time);
            // blockProcessOnInput();
            // }

            // if (current_time == 5) {
            // unblockProcessOnInput();
            // System.out.println("Unblocking process " + unblockedProcessID + " from input
            // at time " + current_time);
            // }

            for (int i = 0; i < execution_time; i++) {
                if (current_process.isBlocked() == true) {
                    break;
                }
                // Memory_Refactored.printProcess(processId);
                System.out.println("Process " + current_process.getP_id() + " is running at time " + current_time);

                // int currectPC = Memory_Refactored.getPC(processId);
                // String instruction = Memory_Refactored.getInstruction(processId, currectPC);
                // Parser.parse(instruction);
                // Memory_Refactored.setPC(processId, currectPC + 1);

                if (unblockedProcessID != -1) {
                    OS_Process unblockedProcess = getProcess(unblockedProcessID);
                    if (unblockedProcess != null) {
                        RRQueue.offer(unblockedProcess);
                        readyQueue.offer(unblockedProcess.getP_id());
                        unblockedProcess.setBlocked(false);
                    }
                    unblockedProcessID = -1;
                }
                current_time++;
                current_process.set_Executed_time(current_process.getExecuted_time() + 1);
                if (!processes.isEmpty() && processes.get(0).getArrival_time() <= current_time) {
                    RRQueue.offer(processes.get(0));
                    readyQueue.offer(processes.get(0).getP_id());
                    processes.remove(0);
                }
            }
            if (current_process.isBlocked() == true) {
                current_time++;
                continue;
            }
            if (current_process.getExecuted_time() < current_process.getBurst_time()) {
                RRQueue.offer(current_process);
                readyQueue.offer(current_process.getP_id());
            } else {
                System.out.println("Process " + current_process.getP_id() + " completed at time " + current_time);
            }

            // For testing purposes
            // System.out.println("Queue state at end of RR iteration (time " + current_time
            // + "):");
            // printRRAndReadyQueues(RRQueue);

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
                    processes.remove(0);
                    pqIndex = 0;
                }
            }

            current_process = PQs.get(pqIndex).poll();
            readyQueue.remove(current_process.getP_id());
            int execution_time = Math.min(current_process.getBurst_time() - current_process.getExecuted_time(),
                    (int) Math.pow(2, pqIndex));

            for (int i = 0; i < execution_time; i++) {
                if (current_process.isBlocked() == true) {
                    break;
                }
                System.out.println("Process " + current_process.getP_id() + " is running at time " + current_time);

                if (unblockedProcessID != -1) {
                    OS_Process unblockedProcess = getProcess(unblockedProcessID);
                    if (unblockedProcess != null) {
                        PQs.get(0).offer(unblockedProcess);
                        readyQueue.offer(unblockedProcess.getP_id());
                        unblockedProcess.setBlocked(false);
                    }
                    unblockedProcessID = -1;
                }
                current_time++;
                current_process.set_Executed_time(current_process.getExecuted_time() + 1);
                if (!processes.isEmpty() && processes.get(0).getArrival_time() <= current_time) {
                    PQs.get(0).offer(processes.get(0));
                    readyQueue.offer(processes.get(0).getP_id());
                    processes.remove(0);
                }
            }
            if (current_process.isBlocked() == true) {
                current_time++;
                continue;
            }
            if (current_process.getExecuted_time() < current_process.getBurst_time()) {
                if(pqIndex<3)
                    pqIndex++;
                PQs.get(pqIndex).offer(current_process);
                readyQueue.offer(current_process.getP_id());
            } else {
                System.out.println("Process " + current_process.getP_id() + " completed at time " + current_time);
            }

            // For testing purposes
            // System.out.println("Queue state at end of RR iteration (time " + current_time
            // + "):");
            // printRRAndReadyQueues(RRQueue);

            current_process = null;
        }

    }

    public static void main(String[] args) {

        // simulate_HRRN(null);

    }
}