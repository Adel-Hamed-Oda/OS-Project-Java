package scheduler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;  // ← added
import java.util.Queue;

public class Scheduler {

    public static Queue<Integer> readyQueue = new LinkedList<Integer>();
    public static Queue<Integer> waitingQueue = new LinkedList<Integer>();
    public static Queue<Integer> jobPool = new LinkedList<Integer>();

    final private static ArrayList<Integer> arrival_times = new ArrayList<Integer>(Arrays.asList(0, 1, 4));
    final private static ArrayList<Integer> burst_times = new ArrayList<Integer>(Arrays.asList(7, 6, 1));


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

        while (processes.size() > 0) {
            int index = get_HRRN(processes, current_time);
            if (index != -1) {
                Process process = processes.get(index);
                for (int i = 0; i < process.getBurst_time(); i++) {
                    System.out.println("Process " + process.getP_id() + " is running at time " + current_time);
                    current_time++;
                }
                process.increment_Executed_time(process.getBurst_time());
                processes.remove(index);
                System.out.println("Process " + process.getP_id() + " completed at time " + current_time);
                System.out.println("--------------------------------------------------");
            } else {
                current_time++;
            }
        }
    }

    public static int getCurrentProcessID() {
        // get the id of the current process
    }
    public static void blockProcessOnInput() {
        // put in input blocked queue
    }
    public static void blockProcessOnOutput() {
        // put in output blocked queue
    }
    public static void blockProcessOnMemory() {
        // put in memory blocked queue
    }
    public static void unblockProcessOnInput() {
        // move from input blocked queue to ready queue
    }
    public static void unblockProcessOnOutput() {
        // move from output blocked queue to ready queue
    }
    public static void unblockProcessOnMemory() {
        // move from memory blocked queue to ready queue
    }


    public static void main(String[] args) {

        readyQueue.offer(1);
        readyQueue.offer(2);
        readyQueue.offer(3);

        ArrayList<Process> processes = convertReadyQueueToProcesses();

        simulate_HRRN(processes);
    }
}