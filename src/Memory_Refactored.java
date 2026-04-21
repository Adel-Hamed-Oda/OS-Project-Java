package src;

import java.io.*;

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
        memory[startIndex + 1].value = String.valueOf(ProcessState.Ready);
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
    }

    public static boolean tryAllocateProcess(int processId) {
        try {
            allocateProcess(processId);
            return true;
        } catch (NotEnoughMemoryException e) {
            return false;
        }
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

        File file = new File(filename);
        if (PublicDomain.REMOVE_FILES_AFTER_EXECUTION) {
            file.deleteOnExit();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (int i = lowerBoundary; i <= upperBoundary; i++) {
                writer.append(memory[i] + "\n");
                memory[i].clear(); // Free memory
            }
        } catch (IOException e) {
            System.out.println("Error creating context file: " + e.getMessage());
        }

        compactMemory();
    }

    public static void loadContext(int processId) throws NotEnoughMemoryException {
        String filename = "Process_" + processId + "_Context.txt";

        try {
            // First pass: count lines without consuming the reader we'll use for loading.
            long lineCount;
            try (var counter = new BufferedReader(new FileReader(filename))) {
                lineCount = counter.lines().count();
            }

            int lowerBoundary = findFreeSpace((int) lineCount);
            if (lowerBoundary == -1) {
                throw new NotEnoughMemoryException("Not enough memory to load context for process " + processId);
            }

            // Second pass: actually read and load the lines.
            int index = 0;
            try (var reader = new BufferedReader(new FileReader(filename))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    memory[lowerBoundary + index].fromString(line);
                    index++;
                }
            }

            updateProcessBounds(lowerBoundary);

        } catch (IOException e) {
            System.out.println("Error reading context file: " + e.getMessage());
        }
    }

    // Improved trySwapOut handles multiple processes, prioritizes victims based on state,
    // and completely discards terminated processes instead of saving them to disk.
    public static void swapOut(int requiredSpace) {
        while (getAmountOfFreeSpace() < requiredSpace) {
            int victimId = selectVictimProcess();

            if (victimId == -1) {
                System.out.println("No suitable process could be swapped out to free up " + requiredSpace + " spaces.");
                break;
            }

            ProcessState state = getProcessState(victimId);
            
            // If the process is terminated, we don't need to save its context.
            // Just wipe it from memory and delete its leftover text file (if any).
            if (state != null && state.name().equals("Terminated")) {
                System.out.println("Process " + victimId + " is Terminated. Removing from memory completely.");
                removeProcessFromMemory(victimId);
                deleteContextFile(victimId);
            } else {
                // Otherwise, it's Blocked, Ready, etc. We must save its context to disk.
                System.out.println("Swapping out Process " + victimId + " (State: " + state + ") to free memory.");
                saveContext(victimId); // Note: saveContext() already clears the memory cells
            }

            // Compact memory after each removal to group free spaces together
            compactMemory();
        }
    }

    //#region Swap-Out Helper Methods

    // Helper method to prioritize which process to kick out of memory.
    // Priorities: 1. Terminated, 2. Blocked/Waiting, 3. Ready, 4. New.
    // It avoids swapping out a "Running" process.
    private static int selectVictimProcess() {
        int victimId = findProcessByState("Terminated");
        
        if (victimId == -1) {
            victimId = findProcessByState("Blocked"); // Change to "Waiting" if that's what your enum uses
        }
        
        if (victimId == -1) {
            victimId = findProcessByState("Ready");
        }
        
        if (victimId == -1) {
            victimId = findProcessByState("New");
        }

        // Fallback: Just grab the first process we can find that isn't currently "Running"
        if (victimId == -1) {
            for (int i = 0; i < MEMORY_SIZE; i++) {
                if (memory[i].type == CellType.PCB && "id".equals(memory[i].name)) {
                    int processId = Integer.parseInt(memory[i].value);
                    ProcessState state = getProcessState(processId);
                    if (state != null && !state.name().equals("Running")) {
                        return processId;
                    }
                }
            }
        }

        return victimId; // Return a random ID that likely doesn't exist to trigger the "no suitable process" case
    }

    // Searches memory for a process that matches the specific state name
    private static int findProcessByState(String targetStateName) {
        for (int i = 0; i < MEMORY_SIZE; i++) {
            if (memory[i].type == CellType.PCB && "id".equals(memory[i].name)) {
                int processId = Integer.parseInt(memory[i].value);
                ProcessState state = getProcessState(processId);
                
                if (state != null && state.name().equalsIgnoreCase(targetStateName)) {
                    return processId;
                }
            }
        }
        return -1;
    }

    // Simply clears the memory cells belonging to a process without saving
    private static void removeProcessFromMemory(int processId) {
        int[] bounds = findProcessBounds(processId);
        if (bounds[0] != -1 && bounds[1] != -1) {
            for (int i = bounds[0]; i <= bounds[1]; i++) {
                memory[i].clear();
            }
        }
    }

    // Deletes the context text file for a process from the hard drive
    private static void deleteContextFile(int processId) {
        File file = new File("Process_" + processId + "_Context.txt");
        if (file.exists()) {
            file.delete();
        }
    }

    //#endregion

    public static boolean tryLoadProcess(int processId, boolean tryFindingContext) {
        if (processExistsInMemory(processId)) {
            System.out.println("Process " + processId + " is already in memory. No need to load.");
            return true; // Process is already in memory, no need to load
        }

        System.out.println("Attempting to load Process " + processId + " into memory...");

        // Check if we should load an existing context or allocate a new one
        boolean hasContext = tryFindingContext && ProcessController.contextFileExists(processId);

        try {
            if (hasContext) {
                loadContext(processId);
            } else {
                allocateProcess(processId);
                System.out.println("Process " + processId + " allocated in memory.");
            }

            return true; // Success on the first try
            
        } catch (NotEnoughMemoryException e) {
            System.out.println("Warning: Not enough memory to allocate process " + processId);
            // Memory is full! Calculate how much space we need to free up.
            // (Instructions length + 3 variables + 4 PCB slots = length + 7)
            int requiredSpace = ProcessController.getInstructions(processId).length + 7;
            
            // Attempt to swap out an older process to free up space
            swapOut(requiredSpace);
            
            // Try loading/allocating one more time now that (hopefully) space is freed
            try {
                if (hasContext) {
                    loadContext(processId);
                } else {
                    allocateProcess(processId);
                    System.out.println("Process " + processId + " allocated in memory after swapping.");
                }

                return true; // Success on the second try
                
            } catch (NotEnoughMemoryException ex) {
                // If it fails again, we are completely out of options
                System.out.println("Error: Not enough memory to allocate process " + processId);
                return false;
            }
        }
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
            String prefix = (i == getPC(processId) + lowerBoundary + 7) ? "--> " : "    ";

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

        // Scan for every process's id cell and update its bounds to reflect the new position.
        for (int i = 0; i < MEMORY_SIZE; i++) {
            if (memory[i].type == CellType.PCB && "id".equals(memory[i].name)) {
                updateProcessBounds(i);
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

    public static boolean processExistsInMemory(int processId) {
        for (int i = 0; i < MEMORY_SIZE; i++) {
            if (memory[i].type == CellType.PCB && 
                memory[i].name.equals("id") &&
                memory[i].value.equals(String.valueOf(processId)))
            {
                return true;
            }
        }
        return false;
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

    private static int getAmountOfFreeSpace() {
        int freeCount = 0;

        for (int i = 0; i < MEMORY_SIZE; i++) {
            if (memory[i].type == CellType.Free) {
                freeCount++;
            }
        }
        return freeCount;
    }

    private static void updateProcessBounds(int newStart) {
        String[] parts = memory[newStart + 3].value.split("-");
        int oldStart = Integer.parseInt(parts[0]);
        int oldEnd   = Integer.parseInt(parts[1]);
        int blockSize = oldEnd - oldStart + 1;
        memory[newStart + 3].value = newStart + "-" + (newStart + blockSize - 1);
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
        // Write empty string instead of "null" so fromString can round-trip correctly
        return String.format("%s,%s,%s",
            name  == null ? "" : name,
            value == null ? "" : value,
            type);
    }

    public void fromString(String line) {
        // Limit to 3 parts so a comma inside a value doesn't break the split
        String[] parts = line.split(",", 3);
        if (parts.length == 3) {
            this.name  = parts[0].isEmpty() ? null : parts[0];
            this.value = parts[1].isEmpty() ? null : parts[1];
            this.type  = CellType.valueOf(parts[2]);
        }
    }
}

enum CellType {
    Free,
    PCB,
    Variable,
    Instruction
}