import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProcessController {
    public static ArrayList<String[]> instructionTable = new ArrayList<>();

    public static String DISK_FILE_NAME = "Disk.txt";

    public static void initProcessController() {
        instructionTable.clear();

        File diskFile = new File(DISK_FILE_NAME);
        if (!diskFile.exists()) {
            try {
                diskFile.delete(); // Ensure we start with a clean slate
                diskFile.createNewFile();
            } catch (IOException e) {
                System.out.println("Error creating disk file: " + e.getMessage());
            }
        }
    }

    public static void AddNewProcess(String fileName) {
        FileReader reader;

        try {
            reader = new FileReader(fileName);
        } catch (FileNotFoundException e) {
            System.out.println("Error: File not found - " + fileName);
            return;
        }
        String[] instructions = new String[0];
        // Read the file line by line and add to instructions list
        try (BufferedReader br = new BufferedReader(reader)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                instructions = java.util.Arrays.copyOf(instructions, instructions.length + 1);
                instructions[instructions.length - 1] = line;
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }

        instructionTable.add(instructions);
    }

    public static String[] getInstructions(int processID) {
        return instructionTable.get(processID);
    }

    public static int getInstructionCount(int processID) {
        String[] instructions = getInstructions(processID);
        return instructions != null ? instructions.length : 0;
    }

    public static boolean contextExists(int processID) {
        File diskFile = new File(DISK_FILE_NAME);
        if (!diskFile.exists())
            return false;

        try (BufferedReader br = new BufferedReader(new FileReader(diskFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.equals("PROCESS_START:" + processID)) {
                    return true;
                }
            }
        } catch (IOException e) {
            System.out.println("Error checking context existence: " + e.getMessage());
        }
        return false;
    }

    public static void deleteContext(int processID) {
        File diskFile = new File(DISK_FILE_NAME);
        if (!diskFile.exists())
            return;

        try {
            List<String> allLines = Files.readAllLines(diskFile.toPath());
            List<String> updatedLines = new ArrayList<>();
            boolean skipping = false;

            for (String line : allLines) {
                if (line.equals("PROCESS_START:" + processID)) {
                    skipping = true;
                    continue;
                }
                if (line.equals("PROCESS_END:" + processID)) {
                    skipping = false;
                    continue;
                }

                if (!skipping) {
                    updatedLines.add(line);
                }
            }

            Files.write(diskFile.toPath(), updatedLines);
        } catch (IOException e) {
            System.out.println("Error deleting context: " + e.getMessage());
        }
    }

    public static void saveContext(int processID, String[] lines) {
        // If it already exists, override it by deleting the old one first
        if (contextExists(processID)) {
            deleteContext(processID);
        }

        try {
            File diskFile = new File(DISK_FILE_NAME);
            List<String> newContext = new ArrayList<>();
            newContext.add("PROCESS_START:" + processID);
            newContext.addAll(Arrays.asList(lines));
            newContext.add("PROCESS_END:" + processID);

            // Append the new context block to the disk file
            Files.write(diskFile.toPath(), newContext, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("Error saving context: " + e.getMessage());
        }
    }

    public static String[] loadContext(int processID) {
        File diskFile = new File(DISK_FILE_NAME);
        if (!diskFile.exists())
            return null;

        List<String> contextLines = new ArrayList<>();
        boolean reading = false;

        try {
            List<String> allLines = Files.readAllLines(diskFile.toPath());
            for (String line : allLines) {
                if (line.equals("PROCESS_START:" + processID)) {
                    reading = true;
                    continue;
                }
                if (line.equals("PROCESS_END:" + processID)) {
                    break;
                }

                if (reading) {
                    contextLines.add(line);
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading context: " + e.getMessage());
        }

        if (contextLines.isEmpty()) {
            return null; // Process not found
        }

        return contextLines.toArray(String[]::new);
    }

    public static void setProcessState(int processID, ProcessState state) {
        if (Memory.processExistsInMemory(processID)) {
            Memory.setProcessState(processID, state);
        } else {
            if (contextExists(processID)) {
                try {
                    File diskFile = new File(DISK_FILE_NAME);
                    List<String> lines = new ArrayList<>(Files.readAllLines(diskFile.toPath()));

                    for (int i = 0; i < lines.size(); i++) {
                        // Check where the process block starts
                        if (lines.get(i).equals("PROCESS_START:" + processID)) {
                            // Ensure we don't go out of bounds just in case of file corruption
                            if (i + 2 < lines.size()) {
                                lines.set(i + 2, "state," + state.name() + ",PCB");
                            }
                            break;
                        }
                    }

                    // Rewrite the file with the modified state line
                    Files.write(diskFile.toPath(), lines);
                } catch (IOException e) {
                    System.out.println("Warning: Failed to update state in disk file for process " + processID + ": "
                            + e.getMessage());
                }
            } else {
                System.out.println("Warning: Cannot set state to " + state + " for process " + processID
                        + " because it is not in memory or disk.");
            }
        }
    }
}
