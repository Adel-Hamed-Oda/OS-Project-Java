package memory;

import java.io.*;
import os_process.*;

public class Memory_Refactored {
    private static final int MEMORY_SIZE = 40;
    private static MemoryCell[] memory;

    public static void initMemory() {
        memory = new MemoryCell[MEMORY_SIZE];
        for (int i = 0; i < MEMORY_SIZE; i++) {
            memory[i] = new MemoryCell();
        }
    }

    public static void allocateProcess(int processId) throws NotEnoughMemoryException {
        PCB pcb = ProcessController.getProcess(processId);
        if (pcb == null) {
            System.out.println("Process " + processId + " not found.");
            return;
        }
        String[] instructions = ProcessController.getInstructions(processId);
        
        int requiredSpace = instructions.length + 3 + 4; // + 3 for variables + 4 for PCB

        int startIndex = findFreeSpace(requiredSpace);
        if (startIndex == -1) {
            throw new NotEnoughMemoryException("Not enough memory to allocate process " + processId);
        }

        memory[startIndex].name = "id";
        memory[startIndex].value = String.valueOf(processId);
        memory[startIndex].type = CellType.PCB;

        memory[startIndex + 1].name = "state";
        memory[startIndex + 1].value = String.valueOf(pcb.processState);
        memory[startIndex + 1].type = CellType.PCB;

        memory[startIndex + 2].name = "pc";
        memory[startIndex + 2].value = String.valueOf(pcb.programCounter);
        memory[startIndex + 2].type = CellType.PCB;

        memory[startIndex + 3].name = "bounds";
        memory[startIndex + 3].value = String.valueOf(pcb.lowerBoundary) + "-" + String.valueOf(pcb.upperBoundary);
        memory[startIndex + 3].type = CellType.PCB;

        for (int i = 0; i < 3; i++) {
            memory[startIndex + 4 + i].name = "";
            memory[startIndex + 4 + i].value = "";
            memory[startIndex + 4 + i].type = CellType.Variable;
        }

        for (int i = 0; i < instructions.length; i++) {
            memory[startIndex + 7 + i].name = "";
            memory[startIndex + 7 + i].value = instructions[i];
            memory[startIndex + 7 + i].type = CellType.Instruction;
        }
    }

    public static String getVariable(int processId, String varName) {
        int[] bounds = findProcessBounds(processId);
        if (bounds[0] == -1 || bounds[1] == -1) {
            System.out.println("Process " + processId + " not found in memory.");
            return null;
        }

        for (int i = bounds[0] + 4; i < bounds[0] + 7; i++) {
            if (memory[i].name.equals(varName)) {
                return memory[i].value;
            }
        }

        System.out.println("Variable " + varName + " not found for Process " + processId);
        return null;
    }

    public static void setVariable(int processId, String varName, String value) {
        int[] bounds = findProcessBounds(processId);
        if (bounds[0] == -1 || bounds[1] == -1) {
            System.out.println("Process " + processId + " not found in memory.");
            return;
        }

        // in case the variable already exists
        for (int i = bounds[0] + 4; i < bounds[0] + 7; i++) {
            if (memory[i].name.equals(varName)) {
                memory[i].name = varName;
                memory[i].value = value;
                return;
            }
        }

        // in case the variable is new
        for (int i = bounds[0] + 4; i < bounds[0] + 7; i++) {
            if (memory[i].type == CellType.Variable && memory[i].name.equals("")) {
                memory[i].name = varName;
                memory[i].value = value;
                return;
            }
        }

        System.out.println("No space to set variable " + varName + " for Process " + processId);
    }

    public static void saveContext(int processId) {
        String filename = "Process_" + processId + "_Context.txt";

        int[] bounds = findProcessBounds(processId);
        int lowerBoundary = bounds[0];
        int upperBoundary = bounds[1];

        if (lowerBoundary == -1 || upperBoundary == -1) {
            System.out.println("Process " + processId + " not found in memory.");
            return;
        }

        try (var writer = new BufferedWriter(new FileWriter(filename))) {
            for (int i = lowerBoundary; i <= upperBoundary; i++) {
                writer.append(memory[i] + "\n");
                memory[i].clear(); // Free memory
            }
        } catch (IOException e) {
            System.out.println("Error creating context file: " + e.getMessage());
        }

        compactMemory();
    }

    public static void loadContext(int processId, int lowerBoundary) {
        String filename = "Process_" + processId + "_Context.txt";

        try (var reader = new BufferedReader(new FileReader(filename))) {
            String line;
            int index = 0;

            while ((line = reader.readLine()) != null) {
                memory[lowerBoundary + index].fromString(line);
                index++;
            }
        } catch (IOException e) {
            System.out.println("Error reading context file: " + e.getMessage());
        }
    }

    //#region Utility Methods

    public static void printMemory() {
        for (int i = 0; i < MEMORY_SIZE; i++) {
            System.out.println("Address " + i + ": " + memory[i]);
        }
    }

    public static void compactMemory() {
        int freeIndex = 0;

        for (int i = 0; i < MEMORY_SIZE; i++) {
            if (memory[i].type != CellType.Free) {
                if (i != freeIndex) {
                    memory[freeIndex].name = memory[i].name;
                    memory[freeIndex].value = memory[i].value;
                    memory[freeIndex].type = memory[i].type;

                    memory[i].clear();
                }
                freeIndex++;
            }
        }
    }

    public static int findFreeSpace(int requiredSpace) {
        int freeCount = 0;

        for (int i = 0; i < MEMORY_SIZE; i++) {
            if (memory[i].type == CellType.Free) {
                freeCount++;
                if (freeCount == requiredSpace) {
                    return i - requiredSpace + 1;
                }
            } else {
                freeCount = 0;
            }
        }
        return -1; // No sufficient free space found
    }

    //#endregion

    //#region Helper Methods

    private static int[] findProcessBounds(int processId) {
        int lowerBoundary = -1;
        int upperBoundary = -1;

        for (int i = 0; i < MEMORY_SIZE; i++) {
            if (memory[i].type == CellType.PCB && memory[i].value.equals(String.valueOf(processId))) {
                lowerBoundary = i;
                break;
            }
        }

        if (lowerBoundary != -1) {
            for (int i = lowerBoundary + 1; i < MEMORY_SIZE; i++) {
                if (memory[i].type == CellType.PCB || memory[i].type == CellType.Free) {
                    upperBoundary = i - 1;
                    break;
                }
            }
            if (upperBoundary == -1) {
                upperBoundary = MEMORY_SIZE - 1;
            }
        }

        return new int[]{lowerBoundary, upperBoundary};
    }

    //#endregion
}

final class MemoryCell {
    public String name;
    public String value;
    public CellType type;

    public MemoryCell() {
        clear();
    }

    public void clear() {
        this.name = "";
        this.value = "";
        this.type = CellType.Free;
    }

    @Override
    public String toString() {
        return String.format("%s %s %s", name, value, type);
    }

    public void fromString(String line) {
        String[] parts = line.split(",");
        if (parts.length == 3) {
            this.name = parts[0];
            this.value = parts[1];
            this.type = CellType.valueOf(parts[2]);
        }
    }
}

enum CellType {
    Free,
    PCB,
    Variable,
    Instruction
}