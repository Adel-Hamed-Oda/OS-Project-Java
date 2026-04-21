package src;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ProcessController {
    public static ArrayList<String[]> instructionTable = new ArrayList<>();

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

    public static boolean contextFileExists(int processID) {
        String contextFileName = "Process_" + processID + "_Context.txt";
        java.io.File contextFile = new java.io.File(contextFileName);
        return contextFile.exists();
    }

    public static void setProcessState(int processID, ProcessState state) {
        if (Memory_Refactored.processExistsInMemory(processID)) {
            Memory_Refactored.setProcessState(processID, state);
        } else {
            if (contextFileExists(processID)) {
                try {
                    File contextFile = new File("Process_" + processID + "_Context.txt"); // use however you build the file path
                    List<String> lines = new ArrayList<>(Files.readAllLines(contextFile.toPath()));

                    for (int i = 0; i < lines.size(); i++) {
                        // Matches lines like "state: Ready" or "state = Running" etc.
                        if (lines.get(i).toLowerCase().startsWith("state")) {
                            lines.set(i, "state," + state.toString() + ",PCB"); // Update the state line with the new state
                            break;
                        }
                    }

                    Files.write(contextFile.toPath(), lines);

                } catch (IOException e) {
                    System.out.println("Warning: Failed to update state in context file for process " + processID + ": " + e.getMessage());
                }
            } else {
                System.out.println("Warning: Cannot set state to " + state + " for process " + processID + " because it is not in memory.");
            }
        }
    }
}
