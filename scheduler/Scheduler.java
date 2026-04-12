package scheduler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class Scheduler {

    public static Queue<Integer> readyQueue = new LinkedList<Integer>();
    public static Queue<Integer> waitingQueue = new LinkedList<Integer>();
    public static Queue<Integer> jobPool = new LinkedList<Integer>();

    final private static ArrayList<Integer> arrival_times = new ArrayList<Integer>(Arrays.asList(0, 7, 4));
    final private static ArrayList<Integer> burst_times = new ArrayList<Integer>(Arrays.asList(7, 6, 1));

    public static int getCurrentProcessID() {
        return readyQueue.peek();
    }

    public static void blockCurrentProcess(String str) {
        int currentProcessID = readyQueue.poll();
        if (currentProcessID != -1) {
            waitingQueue.offer(currentProcessID); // multiple waiting queues for each mutex??
        }
    }

    public static void unblockProcessOnInput() {
        if (!waitingQueue.isEmpty()) {
            int processID = waitingQueue.poll();
            readyQueue.offer(processID);
        }
    }

    public static void unblockProcessOnOutput() {
        if (!waitingQueue.isEmpty()) {
            int processID = waitingQueue.poll();
            readyQueue.offer(processID);
        }
    }

    public static void unblockProcessOnMemory() {
        if (!waitingQueue.isEmpty()) {
            int processID = waitingQueue.poll();
            readyQueue.offer(processID);
        }
    }

    public static ArrayList<Process> convertReadyQueueToProcesses() {
        ArrayList<Process> processes = new ArrayList<Process>();
        ArrayList<Integer> readyQueueArr = new ArrayList<>(readyQueue);
        for (int i = 0; i < readyQueueArr.size(); i++) {
            int p_id = readyQueueArr.get(i);
            int arrival_time = arrival_times.get(i);
            int burst_time = burst_times.get(i);
            processes.add(new Process(p_id, arrival_time, burst_time));
        }
        return processes;
    }

    public static int get_HRRN(ArrayList<Process> processes, int current_time) {
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

    public static void simulate_HRRN(ArrayList<Process> processes) {
        int current_time = 0;

        while (!processes.isEmpty()) {
            int index = get_HRRN(processes, current_time);
            if (index != -1) {
                Process process = processes.get(index);
                for (int i = 0; i < process.getBurst_time(); i++) {
                    System.out.println("Process " + process.getP_id() + " is running at time " + current_time);
                    current_time++;
                }
                process.set_Executed_time(process.getBurst_time());
                processes.remove(index);
                System.out.println("Process " + process.getP_id() + " completed at time " + current_time);
                System.out.println("--------------------------------------------------");
            } else {
                current_time++;
            }
        }
    }

    public static boolean isProcessInRRQueue(int processID, Queue<Process> RRQueue) {
        for (Process process : RRQueue) {
            if (process.getP_id() == processID) {
                return true;
            }
        }
        return false;
    }

    public static void simulate_RR(ArrayList<Process> processes, int time_quantum) {
        processes.sort((p1, p2) -> Integer.compare(p1.getArrival_time(), p2.getArrival_time()));
        int current_time = 0;
        Queue<Process> RRQueue = new LinkedList<>(processes);

        while(!processes.isEmpty() || !RRQueue.isEmpty()) {
            if(RRQueue.isEmpty()) {
                if(processes.get(0).getArrival_time() > current_time) {
                    current_time++;
                } else {
                    RRQueue.offer(processes.get(0));
                    processes.remove(0);
                }
            }
            Process current_process = RRQueue.poll();
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

        readyQueue.offer(1); // ← offer() instead of enqueue()
        readyQueue.offer(2);
        readyQueue.offer(3);

        ArrayList<Process> processes = convertReadyQueueToProcesses();

        simulate_RR(processes, 2);
    }
}