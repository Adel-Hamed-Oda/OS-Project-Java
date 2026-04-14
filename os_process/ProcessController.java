package os_process;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

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
}
