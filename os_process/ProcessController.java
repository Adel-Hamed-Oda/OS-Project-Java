package os_process;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class ProcessController {
    // this is just a list to reference all processes by their id, by no means is this the list of loaded processes
    public static ArrayList<PCB> processTable = new ArrayList<>();
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

        PCB newProcess = new PCB(ProcessState.New, 0, -1, -1);
        processTable.add(newProcess);
        instructionTable.add(instructions);
    }
    
    public static PCB getProcess(int processID) {
        for (PCB pcb : processTable) {
            if (pcb.processID == processID) {
                return pcb;
            }
        }
        return null;
    }

    public static String[] getInstructions(int processID) {
        for (int i = 0; i < processTable.size(); i++) {
            if (processTable.get(i).processID == processID) {
                return instructionTable.get(i);
            }
        }
        return null;
    }

    public static int getInstructionCount(int processID) {
        String[] instructions = getInstructions(processID);
        return instructions != null ? instructions.length : 0;
    }
}
