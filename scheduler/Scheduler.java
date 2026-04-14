package scheduler;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import memory.*;
import os_process.*;
import parser.*;

public class Scheduler {

    public static Queue<Integer> readyQueue = new LinkedList<>();  // should things from the waitnig queue return to the beginning or the end of the queue?
    public static Queue<Integer> waitingQueueInput = new LinkedList<>();
    public static Queue<Integer> waitingQueueOutput = new LinkedList<>();
    public static Queue<Integer> waitingQueueMemory = new LinkedList<>();
    public static Queue<Integer> jobPool = new LinkedList<>();

    final public static ArrayList<Integer> arrival_times = new ArrayList<>();
    final public static ArrayList<Integer> burst_times = new ArrayList<>();
    public static int current_time = 0;
    public static OS_Process current_process;

    public static int getCurrentProcessID() {
        if (current_process == null) {
            return -1;
        }
        return current_process.getP_id();
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

    //For testing purposes
    public static void printReadyQueue() {
        System.out.print("Ready Queue: ");
        for (Integer processID : readyQueue) {
            System.out.print(processID + " ");
        }
        System.out.println();
        System.out.println("----End of Ready Queue----");
    }

    public static void updateReadyQueue(ArrayList<OS_Process> processes) {
        for (OS_Process process : processes) {
            if (process.getArrival_time() <= current_time && !process.is_in_ready_queue() && process!= current_process) {
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
            int processId = processes.get(index).getP_id();
            if (index != -1) {
                try {
                    Memory_Refactored.allocateProcess(processId);
                } catch (NotEnoughMemoryException e) {
                    int requiredSpace = ProcessController.getInstructions(processId).length + 3 + 4;
                    
                    Memory_Refactored.trySwapOut(requiredSpace);
                    try {
                        Memory_Refactored.allocateProcess(processId);
                    } catch (NotEnoughMemoryException ex) {
                        System.out.println("Error: Not enough memory to allocate process " + processId);
                        return;
                    }
                }

                current_process = processes.get(index);
                updateReadyQueue(processes);

                for (int i = 0; i < current_process.getBurst_time(); i++) {
                    System.out.println("Process " + current_process.getP_id() + " is running at time " + current_time);
                    
                    int currectPC = Memory_Refactored.getPC(processId);
                    String instruction = Memory_Refactored.getInstruction(processId, currectPC);
                    Parser.parse(instruction);
                    Memory_Refactored.setPC(processId, currectPC + 1);

                    Memory_Refactored.printProcess(processId);
                    
                    System.out.println("Press Enter to continue...");
                    @SuppressWarnings("resource") // same as system calls
                    Scanner sc = new Scanner(System.in);
                    sc.nextLine();

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

        while(!processes.isEmpty() || !RRQueue.isEmpty()) {
            if(RRQueue.isEmpty()) {
                if(processes.get(0).getArrival_time() > current_time) {
                    current_time++;
                    continue;
                } else {
                    RRQueue.offer(processes.get(0));
                    readyQueue.offer(processes.get(0).getP_id());
                    processes.remove(0);
                }
            }
            current_process = RRQueue.poll();
            readyQueue.remove(current_process.getP_id());
            int execution_time = Math.min(time_quantum, current_process.getBurst_time() - current_process.getExecuted_time());
            for (int i = 0; i < execution_time; i++) {
                System.out.println("Process " + current_process.getP_id() + " is running at time " + current_time);
                current_time++;
                current_process.set_Executed_time(current_process.getExecuted_time() + 1);
                if(!processes.isEmpty() && processes.get(0).getArrival_time() <= current_time) {
                    RRQueue.offer(processes.get(0));
                    readyQueue.offer(processes.get(0).getP_id());
                    processes.remove(0);
                }
            }
            if(current_process.getExecuted_time() < current_process.getBurst_time()) {
                RRQueue.offer(current_process);
                readyQueue.offer(current_process.getP_id());
            } else {
                System.out.println("Process " + current_process.getP_id() + " completed at time " + current_time);
            }
            current_process = null;
        }
            
    }
}