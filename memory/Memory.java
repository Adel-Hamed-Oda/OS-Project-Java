package memory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Memory {
    private MemoryWord[] storage;
    private final int MAX_SIZE = 40;

    public Memory() {
        storage = new MemoryWord[MAX_SIZE];
        for (int i = 0; i < MAX_SIZE; i++) {
            storage[i] = new MemoryWord("Empty", "Empty");
        }
    }

    // Calculates how much space a process needs (Instructions + 3 Variables + 4 PCB Words)
    public boolean allocateProcess(int processId, List<String> instructions) {
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

    private int findFreeSpace(int requiredSpace) {
        int consecutiveFreeSpace = 0;
        int startIndex = -1;

        for (int i = 0; i < MAX_SIZE; i++) {
            if (storage[i].getName().equals("Empty")) {
                if (consecutiveFreeSpace == 0) startIndex = i;
                consecutiveFreeSpace++;
                if (consecutiveFreeSpace == requiredSpace) return startIndex;
            } else {
                consecutiveFreeSpace = 0; // Reset if block is not contiguous
            }
        }
        return -1; // Not enough contiguous space
    }

    public String read(int address, PCB pcb) {
        if (address >= pcb.getLowerBoundary() && address <= pcb.getUpperBoundary()) {
            return storage[address].getValue();
        }
        return "Error: Memory Access Violation!";
    }

    public void write(int address, String name, String value, PCB pcb) {
        if (address >= pcb.getLowerBoundary() && address <= pcb.getUpperBoundary()) {
            storage[address].setName(name);
            storage[address].setValue(value);
        } else {
            System.out.println("Error: Memory Access Violation by Process " + pcb.getProcessID());
        }
    }

    public void swapOut(PCB pcb) {
        String filename = "Disk_Process_" + pcb.getProcessID() + ".txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (int i = pcb.getLowerBoundary(); i <= pcb.getUpperBoundary(); i++) {
                writer.write(storage[i].getName() + "," + storage[i].getValue() + "\n");
                storage[i].clear(); // Free memory
            }
            System.out.println("Process " + pcb.getProcessID() + " swapped OUT to disk.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void swapIn(int processId, int requiredSpace) {
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
                storage[currentIndex].setName(parts[0]);
                storage[currentIndex].setValue(parts[1]);
                currentIndex++;
            }
            
            // Update the PCB boundaries in memory
            storage[startIndex + 3].setValue(startIndex + "-" + (currentIndex - 1));
            System.out.println("Process " + processId + " swapped IN from disk to index " + startIndex);
            
            // Delete the disk file after swapping in
            new File(filename).delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void printMemory() {
        System.out.println("\n--- Current Memory State ---");
        for (int i = 0; i < MAX_SIZE; i++) {
            if (!storage[i].getName().equals("Empty")) {
                System.out.println("Word " + i + ": " + storage[i].toString());
            }
        }
        System.out.println("----------------------------\n");
    }
}