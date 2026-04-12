package scheduler;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import memory.Memory;

public class Scheduler {

    public static Queue<Integer> readyQueue = new LinkedList<Integer>();  // should things from the waitnig queue return to the beginning or the end of the queue?
    public static Queue<Integer> waitingQueueInput = new LinkedList<Integer>();
    public static Queue<Integer> waitingQueueOutput = new LinkedList<Integer>();
    public static Queue<Integer> waitingQueueMemory = new LinkedList<Integer>();
    public static Queue<Integer> jobPool = new LinkedList<Integer>();

    final public static ArrayList<Integer> arrival_times = new ArrayList<>();
    final public static ArrayList<Integer> burst_times = new ArrayList<>();

    public static int getCurrentProcessID() {
        return readyQueue.peek();
    }

    public static void blockProcessOnInput() {
        int currentProcessID = readyQueue.poll();
        if (currentProcessID != -1) {
            waitingQueueInput.offer(currentProcessID);
        }
    }

    public static void blockProcessOnOutput() {
        int currentProcessID = readyQueue.poll();
        if (currentProcessID != -1) {
            waitingQueueOutput.offer(currentProcessID);
        }
    }

    public static void blockProcessOnMemory() {
        int currentProcessID = readyQueue.poll();
        if (currentProcessID != -1) {
            waitingQueueMemory.offer(currentProcessID);
        }
    }

    public static void unblockProcessOnInput() {
        if (!waitingQueueInput.isEmpty()) {
            int processID = waitingQueueInput.poll();
            readyQueue.offer(processID);
        }
    }

    public static void unblockProcessOnOutput() {
        if (!waitingQueueOutput.isEmpty()) {
            int processID = waitingQueueOutput.poll();
            readyQueue.offer(processID);
        }
    }

    public static void unblockProcessOnMemory() {
        if (!waitingQueueMemory.isEmpty()) {
            int processID = waitingQueueMemory.poll();
            readyQueue.offer(processID);
        }
    }

    public static ArrayList<OS_Process> convertReadyQueueToProcesses() {
        ArrayList<OS_Process> processes = new ArrayList<OS_Process>();
        ArrayList<Integer> readyQueueArr = new ArrayList<>(readyQueue);
        for (int i = 0; i < readyQueueArr.size(); i++) {
            int p_id = readyQueueArr.get(i);
            int arrival_time = arrival_times.get(i);
            int burst_time = burst_times.get(i);
            processes.add(new OS_Process(p_id, arrival_time, burst_time));
        }
        return processes;
    }

    public static int get_HRRN(ArrayList<OS_Process> processes, int current_time) {
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

    public static void simulate_HRRN(ArrayList<OS_Process> processes) {
        int current_time = 0;

        while (!processes.isEmpty()) {
            int index = get_HRRN(processes, current_time);
            if (index != -1) {
                OS_Process process = processes.get(index);
                for (int i = 0; i < process.getBurst_time(); i++) {
                    //System.out.println("Process " + process.getP_id() + " is running at time " + current_time);

                    Memory.printMemory();

                    current_time++;
                }
                process.set_Executed_time(process.getBurst_time());
                processes.remove(index);
                //System.out.println("Process " + process.getP_id() + " completed at time " + current_time);
            } else {
                current_time++;
            }
        }
    }

    public static boolean isProcessInRRQueue(int processID, Queue<OS_Process> RRQueue) {
        for (OS_Process process : RRQueue) {
            if (process.getP_id() == processID) {
                return true;
            }
        }
        return false;
    }

    public static void simulate_RR(ArrayList<OS_Process> processes, int time_quantum) {
        processes.sort((p1, p2) -> Integer.compare(p1.getArrival_time(), p2.getArrival_time()));
        int current_time = 0;
        Queue<OS_Process> RRQueue = new LinkedList<>();

        while(!processes.isEmpty() || !RRQueue.isEmpty()) {
            if(RRQueue.isEmpty()) {
                if(processes.get(0).getArrival_time() > current_time) {
                    current_time++;
                } else {
                    RRQueue.offer(processes.get(0));
                    processes.remove(0);
                }
            }
            OS_Process current_process = RRQueue.poll();
            int execution_time = Math.min(time_quantum, current_process.getBurst_time() - current_process.getExecuted_time());
            for (int i = 0; i < execution_time; i++) {
                System.out.println("Process " + current_process.getP_id() + " is running at time " + current_time);
                current_time++;
                current_process.set_Executed_time(current_process.getExecuted_time() + 1);
                if(processes.size() > 0 && processes.get(0).getArrival_time() <= current_time) {
                    RRQueue.offer(processes.get(0));
                    processes.remove(0);
                }
            }
            if(current_process.getExecuted_time() < current_process.getBurst_time()) {
                RRQueue.offer(current_process);
            } else {
                System.out.println("Process " + current_process.getP_id() + " completed at time " + current_time);
            }
        }
            
    }

    public static void main(String[] args) {

        readyQueue.offer(1); 
        readyQueue.offer(2);
        readyQueue.offer(3);

        ArrayList<OS_Process> processes = convertReadyQueueToProcesses();

        simulate_RR(processes, 2);

        // simulate_HRRN(processes);
    }
}