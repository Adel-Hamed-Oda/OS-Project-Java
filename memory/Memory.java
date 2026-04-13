package memory;

import java.io.*;
import java.util.List;
import java.util.Queue;

import os_process.*;

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
            getIntoMemory(processId, new java.util.LinkedList<>(scheduler.Scheduler.readyQueue)); // Here you would trigger swapOut() for an old process, then call allocateProcess again.
            return false; 
        }

        int currentIndex = startIndex;

        // 1. Store PCB info (Type: PCB)
        storage[currentIndex++] = new MemoryWord("PCB_ID", String.valueOf(processId), MemoryWord.WordType.PCB);
        storage[currentIndex++] = new MemoryWord("PCB_State", "Ready", MemoryWord.WordType.PCB);
        storage[currentIndex++] = new MemoryWord("PCB_PC", String.valueOf(currentIndex + 2), MemoryWord.WordType.PCB); 
        storage[currentIndex++] = new MemoryWord("PCB_Bounds", startIndex + "-" + (startIndex + requiredSpace - 1), MemoryWord.WordType.PCB);

        // 2. Store Instructions (Type: Instruction)
        for (int i = 0; i < instructions.size(); i++) {
            storage[currentIndex++] = new MemoryWord("Instruction_" + i, instructions.get(i), MemoryWord.WordType.Instruction);
        }

        // 3. Reserve 3 Variable Spaces (Type: Variable)
        // No more "Var_" prefix needed!
        for (int i = 0; i < 3; i++) {
            storage[currentIndex++] = new MemoryWord("null", "null", MemoryWord.WordType.Variable); 
        }

        PCB pcb = ProcessController.getProcess(processId);
        if (pcb != null) {
            pcb.lowerBoundary = startIndex;
            pcb.upperBoundary = startIndex + requiredSpace - 1;
            pcb.programCounter = startIndex + 4;
        }

        System.out.println("Process " + processId + " allocated from index " + startIndex + " to " + (startIndex + requiredSpace - 1));
        return true;
    }

    private static int findFreeSpace(int requiredSpace) {
        int consecutiveFreeSpace = 0;
        int startIndex = -1;

        for (int i = 0; i < MAX_SIZE; i++) {
            // Using the new Enum to check if it's free
            if (storage[i].type == MemoryWord.WordType.Free) {
                if (consecutiveFreeSpace == 0) startIndex = i;
                consecutiveFreeSpace++;
                if (consecutiveFreeSpace == requiredSpace) return startIndex;
            } else {
                consecutiveFreeSpace = 0; 
            }
        }
        return -1; 
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
        
        // 1. Create a designated temporary file, leaving input files untouched
        String filename = "temp_swap_process_" + pcb.processID + ".txt";
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (int i = pcb.lowerBoundary; i <= pcb.upperBoundary; i++) {
                writer.write(storage[i].name + "," + storage[i].value + "\n");
                storage[i].clear(); 
            }
            
            // 2. Convert absolute PC to a relative index before losing boundaries
            int relativePC = pcb.programCounter - pcb.lowerBoundary - 4;
            pcb.programCounter = relativePC; 
            
            // 3. Mark PCB as on-disk
            pcb.lowerBoundary = -1;
            pcb.upperBoundary = -1;
            System.out.println("Process " + pcb.processID + " swapped OUT to temporary file.");
        } catch (IOException e) {
            System.out.println("Error swapping out process: " + e.getMessage());
        }
    }

    public static void swapIn(int processId) {
        PCB pcb = ProcessController.getProcess(processId);
        int requiredSpace = ProcessController.getInstructionCount(processId) + 7;
        
        int startIndex = findFreeSpace(requiredSpace);
        if(startIndex == -1) {
             System.out.println("Cannot swap in Process " + processId + ": Memory full.");
             return;
        }

        // 1. Read from the temporary file we created
        String filename = "temp_swap_process_" + processId + ".txt";
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            int currentIndex = startIndex;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                storage[currentIndex].name = parts[0];
                storage[currentIndex].value = (parts.length > 1) ? parts[1] : "";
                currentIndex++;
            }
            
            // 2. Safely delete the temp file so it doesn't conflict or clutter
            new File(filename).delete();
            
            // 3. Get the relative PC we saved during swapOut and recalculate absolute addresses
            int relativePC = pcb.programCounter;
            pcb.lowerBoundary = startIndex;
            pcb.upperBoundary = startIndex + requiredSpace - 1;
            pcb.programCounter = startIndex + 4 + relativePC;

            // 4. Update PCB values inside the physical memory array
            storage[startIndex + 2].value = String.valueOf(pcb.programCounter);
            storage[startIndex + 3].value = pcb.lowerBoundary + "-" + pcb.upperBoundary;
            
            System.out.println("Process " + processId + " swapped IN from temporary file to index " + startIndex);
        } catch (IOException e) {
            System.out.println("Error swapping in process: " + e.getMessage());
        }
    }

    public static void printMemory() {
        System.out.println("\n--- Current Memory State ---");
        for (int i = 0; i < MAX_SIZE; i++) {
            if (storage[i].name != null) {
                System.out.println("Word " + i + ": " + storage[i].toString());
            }
        }
        System.out.println("----------------------------\n");
    }
    // Finds the memory address of a specific variable for a process
    public static int getVariableAddress(String varName, int processID) {
        PCB pcb = ProcessController.getProcess(processID);
        for (int i = pcb.lowerBoundary; i <= pcb.upperBoundary; i++) {
            // Check if it is a Variable type AND matches the name
            if (storage[i].type == MemoryWord.WordType.Variable && storage[i].name.equals(varName)) {
                return i; 
            }
        }
        return -1; 
    }

   public static void assignVariable(String varName, String value, int processID) {
        PCB pcb = ProcessController.getProcess(processID);
        int address = getVariableAddress(varName, processID);

        if (address != -1) {
            storage[address].value = value; // Update existing
        } else {
            boolean allocated = false;
            for (int i = pcb.lowerBoundary; i <= pcb.upperBoundary; i++) {
                // Find an unused variable slot (name is still "null")
                if (storage[i].type == MemoryWord.WordType.Variable && storage[i].name.equals("null")) {
                    storage[i].name = varName; // Give it the real name
                    storage[i].value = value;
                    allocated = true;
                    break;
                }
            }
            if (!allocated) {
                System.out.println("Error: Process " + pcb.processID + " exceeded 3 variable limit!");
            }
        }
    }
    // Clears a process's memory block when it is completely finished
   public static void terminateProcess(int processID) {
        PCB pcb = ProcessController.getProcess(processID);
        if (pcb.lowerBoundary == -1) return; 

        int lower = pcb.lowerBoundary;
        int upper = pcb.upperBoundary;
        int height = upper - lower + 1;

        for (int i = lower; i <= upper; i++) {
            storage[i].clear();
        }

        for (int i = lower; i < MAX_SIZE - height; i++) {
            storage[i].type  = storage[i + height].type; // Shift Type too!
            storage[i].name  = storage[i + height].name;
            storage[i].value = storage[i + height].value;
        }

        for (int i = MAX_SIZE - height; i < MAX_SIZE; i++) {
            storage[i].clear();
        }

        for (PCB other : ProcessController.processTable) {
            if (other.processID == processID || other.lowerBoundary == -1) continue;

            if (other.lowerBoundary > upper) {
                other.lowerBoundary -= height;
                other.upperBoundary -= height;
                other.programCounter -= height; 

                storage[other.lowerBoundary + 3].value = other.lowerBoundary + "-" + other.upperBoundary;
                storage[other.lowerBoundary + 2].value = String.valueOf(other.programCounter);
            }
        }
        
        pcb.processState = os_process.ProcessState.Terminated;
    }
    	 public static void getIntoMemory(int processID, Queue<Integer> readyQueue) {
        PCB pcb = ProcessController.getProcess(processID);
        int requiredSpace = ProcessController.getInstructionCount(processID) + 7;

        for (int i = 0; i < MAX_SIZE; i++) {
            if (storage[i].name != null && storage[i].name.equals("PCB_ID") && storage[i].value.equals(String.valueOf(processID))) {
                return; // Already in memory
            }
        }

        while (findFreeSpace(requiredSpace) == -1) {
            int victimID = -1;
            int maxPositionInQueue = -1;
            List<Integer> queueList = new java.util.ArrayList<>(readyQueue);

            for (int i = 0; i < MAX_SIZE; i++) {
                if (storage[i].name != null && storage[i].name.equals("PCB_ID")) {
                    int inMemoryProcessID = Integer.parseInt(storage[i].value);
                    int position = queueList.indexOf(inMemoryProcessID);
                    
                    if (position > maxPositionInQueue) {
                        maxPositionInQueue = position;
                        victimID = inMemoryProcessID;
                    }
                }
            }

            if (victimID == -1) { 
                for (int i = 0; i < MAX_SIZE; i++) {
                    if (storage[i].name != null && storage[i].name.equals("PCB_ID")) {
                        victimID = Integer.parseInt(storage[i].value);
                        break;
                    }
                }
            }

            if (victimID != -1) {
                swapOut(victimID);
            } else {
                return; 
            }
        }
        
        swapIn(processID); 
    } 
}
