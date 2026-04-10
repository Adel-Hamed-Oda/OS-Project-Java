package memory;

import java.io.*;
import java.util.List;
import process.*;

public class Memory {
    private static MemoryWord[] storage;
    private static final int MAX_SIZE = 40;

    public static void Init_Memory() {
        storage = new MemoryWord[MAX_SIZE];
        for (int i = 0; i < MAX_SIZE; i++) {
            storage[i] = new MemoryWord();
        }
    }

    // Calculates how much space a process needs (Instructions + 3 Variables)
    public static boolean allocateProcess(int processId, List<String> instructions) {
        int requiredSpace = instructions.size() + 3 + 4; // 3 variables required by assignment, 4 for PCB
        int startIndex = findFreeSpace(requiredSpace);

        if (startIndex == -1) {
            System.out.println("Not enough memory for Process " + processId + ". Swapping required.");
            // Here you would trigger swapOut() for an old process, then call allocateProcess again.
            return false; 
        }

        int currentIndex = startIndex;

        // 1. Store PCB (ID, State, PC, Boundaries)
        storage[currentIndex++] = new MemoryWord("PCB_ID", String.valueOf(processId));
        storage[currentIndex++] = new MemoryWord("PCB_State", "Ready");
        storage[currentIndex++] = new MemoryWord("PCB_PC", String.valueOf(currentIndex + 2)); // PC points to first instruction
        storage[currentIndex++] = new MemoryWord("PCB_Bounds", startIndex + "-" + (startIndex + requiredSpace - 1));

        // 2. Store Instructions
        for (int i = 0; i < instructions.size(); i++) {
            storage[currentIndex++] = new MemoryWord("Instruction_" + i, instructions.get(i));
        }

        // 3. Reserve 3 Variable Spaces
        for (int i = 0; i < 3; i++) {
            storage[currentIndex++] = new MemoryWord("Var_" + i, "null");
        }

        System.out.println("Process " + processId + " allocated from index " + startIndex + " to " + (startIndex + requiredSpace - 1));
        return true;
    }

    private static int findFreeSpace(int requiredSpace) {
        int consecutiveFreeSpace = 0;
        int startIndex = -1;

        for (int i = 0; i < MAX_SIZE; i++) {
            if (storage[i].name.equals("")) {
                if (consecutiveFreeSpace == 0) startIndex = i;
                consecutiveFreeSpace++;
                if (consecutiveFreeSpace == requiredSpace) return startIndex;
            } else {
                consecutiveFreeSpace = 0; // Reset if block is not contiguous
            }
        }
        return -1; // Not enough contiguous space
    }

    public static String read(int address, int processID) {
        // This is a simplified version - you would need to retrieve the PCB for the given processID
        // For now, assuming we have access to the PCB
        PCB pcb = ProcessController.getProcess(processID);
        if (address >= pcb.lowerBoundary && address <= pcb.upperBoundary) {
            return storage[address].value;
        }
        return "Error: Memory Access Violation!";
    }

    public static void write(int address, String name, String value, int processID) {
        PCB pcb = ProcessController.getProcess(processID);
        if (address >= pcb.lowerBoundary && address <= pcb.upperBoundary) {
            storage[address].name = name;
            storage[address].value = value;
        } else {
            System.out.println("Error: Memory Access Violation by Process " + pcb.processID);
        }
    }

    public static void swapOut(int processId) {
        PCB pcb = ProcessController.getProcess(processId);
        String filename = "Disk_Process_" + pcb.processID + ".txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (int i = pcb.lowerBoundary; i <= pcb.upperBoundary; i++) {
                writer.write(storage[i].name + "," + storage[i].value + "\n");
                storage[i].clear(); // Free memory
            }
            System.out.println("Process " + pcb.processID + " swapped OUT to disk.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void swapIn(int processId, int requiredSpace) {
        String filename = "Disk_Process_" + processId + ".txt";
        int startIndex = findFreeSpace(requiredSpace);
        
        if(startIndex == -1) {
             System.out.println("Cannot swap in Process " + processId + ": Memory full.");
             return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            int currentIndex = startIndex;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                storage[currentIndex].name = parts[0];
                storage[currentIndex].value = parts[1];
                currentIndex++;
            }
            
            // Update the PCB boundaries in memory
            storage[startIndex + 3].value = startIndex + "-" + (currentIndex - 1);
            System.out.println("Process " + processId + " swapped IN from disk to index " + startIndex);
            
            // Delete the disk file after swapping in
            new File(filename).delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void printMemory() {
        System.out.println("\n--- Current Memory State ---");
        for (int i = 0; i < MAX_SIZE; i++) {
            if (!storage[i].name.equals("")) {
                System.out.println("Word " + i + ": " + storage[i].toString());
            }
        }
        System.out.println("----------------------------\n");
    }
    // Finds the memory address of a specific variable for a process
    public static int getVariableAddress(String varName, int processID) {
        PCB pcb = ProcessController.getProcess(processID);
        
        // Search only within this process's allocated memory boundaries
        for (int i = pcb.lowerBoundary; i <= pcb.upperBoundary; i++) {
            if (storage[i].name.equals(varName)) {
                return i; // Variable found!
            }
        }
        return -1; // Variable not found
    }

    // Assigns a value to a variable (creating it if it doesn't exist)
    public static void assignVariable(String varName, String value, int processID) {
        PCB pcb = ProcessController.getProcess(processID);
        
        int address = getVariableAddress(varName, processID);

        if (address != -1) {
            // Variable exists, just update the value
            storage[address].value = value;
            System.out.println("Process " + pcb.processID + " updated variable '" + varName + "' to " + value);
        } else {
            // Variable doesn't exist, find an empty variable slot
            boolean allocated = false;
            for (int i = pcb.lowerBoundary; i <= pcb.upperBoundary; i++) {
                if (storage[i].value.equals("null")) {
                    storage[i].name = varName;
                    storage[i].value = value;
                    System.out.println("Process " + pcb.processID + " created variable '" + varName + "' = " + value);
                    allocated = true;
                    break;
                }
            }
            if (!allocated) {
                System.out.println("Error: Process " + pcb.processID + " has exceeded its 3 variable limit!");
            }
        }
    }
    // Clears a process's memory block when it is completely finished
    public static void terminateProcess(int processID) {
        PCB pcb = ProcessController.getProcess(processID);

        System.out.println("Terminating Process " + pcb.processID + " and freeing memory...");
        
        for (int i = pcb.lowerBoundary; i <= pcb.upperBoundary; i++) {
            storage[i].clear(); // Resets name and value to "Empty"
        }
        
        // Update the PCB state
        pcb.processState = PCB.ProcessState.Terminated;
        System.out.println("Memory from index " + pcb.lowerBoundary + " to " + pcb.upperBoundary + " is now free.");
    }
}