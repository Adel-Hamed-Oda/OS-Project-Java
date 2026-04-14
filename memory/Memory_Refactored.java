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

    // only used when the process is new
    public static void allocateProcess(int processId) throws NotEnoughMemoryException {
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
        memory[startIndex + 1].value = String.valueOf(ProcessState.New);
        memory[startIndex + 1].type = CellType.PCB;

        memory[startIndex + 2].name = "pc";
        memory[startIndex + 2].value = String.valueOf(0);
        memory[startIndex + 2].type = CellType.PCB;

        memory[startIndex + 3].name = "bounds";
        memory[startIndex + 3].value = startIndex + "-" + (startIndex + requiredSpace - 1);
        memory[startIndex + 3].type = CellType.PCB;

        for (int i = 0; i < 3; i++) {
            memory[startIndex + 4 + i].name = null;
            memory[startIndex + 4 + i].value = null;
            memory[startIndex + 4 + i].type = CellType.Variable;
        }

        for (int i = 0; i < instructions.length; i++) {
            memory[startIndex + 7 + i].name = null;
            memory[startIndex + 7 + i].value = instructions[i];
            memory[startIndex + 7 + i].type = CellType.Instruction;
        }

        printMemory();
    }

    public static int getPC(int processId) {
        int[] bounds = findProcessBounds(processId);
        if (bounds[0] == -1 || bounds[1] == -1) {
            System.out.println("Process " + processId + " not found in memory.");
            return -1;
        }

        return Integer.parseInt(memory[bounds[0] + 2].value);
    }

    public static void setPC(int processId, int newPC) {
        int[] bounds = findProcessBounds(processId);
        if (bounds[0] == -1 || bounds[1] == -1) {
            System.out.println("Process " + processId + " not found in memory.");
        }

        memory[bounds[0] + 2].value = String.valueOf(newPC);
    }

    public static ProcessState getProcessState(int processId) {
        int[] bounds = findProcessBounds(processId);
        if (bounds[0] == -1 || bounds[1] == -1) {
            System.out.println("Process " + processId + " not found in memory.");
            return null;
        }

        return ProcessState.valueOf(memory[bounds[0] + 1].value);
    }

    public static void setProcessState(int processId, ProcessState newState) {
        int[] bounds = findProcessBounds(processId);
        if (bounds[0] == -1 || bounds[1] == -1) {
            System.out.println("Process " + processId + " not found in memory.");
            return;
        }

        memory[bounds[0] + 1].value = String.valueOf(newState);
    }

    public static String getInstruction(int processId, int programCounter) {
        int[] bounds = findProcessBounds(processId);
        if (bounds[0] == -1 || bounds[1] == -1) {
            System.out.println("Process " + processId + " not found in memory.");
            return null;
        }

        int instructionStart = bounds[0] + 7;
        int instructionEnd = bounds[1];

        if (programCounter < 0 || instructionStart + programCounter > instructionEnd) {
            System.out.println("Instruction index out of bounds for Process " + processId);
            return null;
        }

        return memory[instructionStart + programCounter].value;
    }

    public static String getVariable(int processId, String varName) {
        int[] bounds = findProcessBounds(processId);
        if (bounds[0] == -1 || bounds[1] == -1) {
            System.out.println("Process " + processId + " not found in memory.");
            return null;
        }

        for (int i = bounds[0] + 4; i < bounds[0] + 7; i++) {
            if (memory[i].name != null && memory[i].name.equals(varName)) {
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
            if (memory[i].name != null && memory[i].name.equals(varName)) {
                memory[i].name = varName;
                memory[i].value = value;
                return;
            }
        }

        // in case the variable is new
        for (int i = bounds[0] + 4; i < bounds[0] + 7; i++) {
            if (memory[i].type == CellType.Variable && memory[i].name == null) {
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

    public static void trySwapOut(int requiredSpace) {
        for (int i = 0; i < MEMORY_SIZE; i++) {
            if (memory[i].type == CellType.PCB) {
                int processId = Integer.parseInt(memory[i].value);
                int[] bounds = findProcessBounds(processId);
                int processSize = bounds[1] - bounds[0] + 1;

                if (processSize >= requiredSpace) {
                    saveContext(processId);

                    compactMemory();

                    return;
                }
            }
        }

        System.out.println("No process could be swapped out to free up " + requiredSpace + " spaces.");
    }

    //#region Utility Methods

    public static void printMemory() {
        System.out.println("=== Current Memory State: =======================================");
        for (int i = 0; i < MEMORY_SIZE; i++) {
            System.out.println("Address " + i + ": " + memory[i]);
        }
        System.out.println("=================================================================");
    }

    public static void printProcess(int processId) {
        int[] bounds = findProcessBounds(processId);
        int lowerBoundary = bounds[0];
        int upperBoundary = bounds[1];

        System.out.println("=== Current Process State: ======================================");
        for (int i = lowerBoundary; i <= upperBoundary; i++) {
            String prefix = (i == getPC(processId) + lowerBoundary + 6) ? "--> " : "    ";

            System.out.println(prefix + "Address " + i + ": " + memory[i]);
        }
        System.out.println("=================================================================");
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
            if (memory[i].type == CellType.PCB && 
                memory[i].name.equals("id") &&
                memory[i].value.equals(String.valueOf(processId)))
            {
                lowerBoundary = memory[i + 3].value != null ? Integer.parseInt(memory[i + 3].value.split("-")[0]) : -1;
                upperBoundary = memory[i + 3].value != null ? Integer.parseInt(memory[i + 3].value.split("-")[1]) : -1;
                break;
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
        this.name = null;
        this.value = null;
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