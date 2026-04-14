package os_system;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import memory.Memory_Refactored;
import scheduler.Scheduler;

public class SystemCalls {
    public static String readFile(String fileName)  {
        StringBuilder content = new StringBuilder();
        
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }

        return content.toString();
    }

    public static void writeFile(String fileName, String data) {
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write(data);
        } catch (IOException e) {
            System.out.println("Error writing to file: " + e.getMessage());
        }
    }

    public static void print(String data) {
        System.out.println(data);
    }

    public static String input() {
        @SuppressWarnings("resource") // I added this because I hate the yellow lines, ignore it

        Scanner sc = new Scanner(System.in);
        return sc.nextLine();
    }

    public static String readFromMemory(String var) {
        return Memory_Refactored.getVariable(Scheduler.getCurrentProcessID(), var);
    }

    public static void writeToMemory(String var, String value) {
        Memory_Refactored.setVariable(Scheduler.getCurrentProcessID(), var, value);
    }

    public static void printFromTo(String var1, String var2) {
        int pid = Scheduler.getCurrentProcessID();
        
        String value1 = Memory_Refactored.getVariable(pid, var1);
        String value2 = Memory_Refactored.getVariable(pid, var2);
        
        int num1 = Integer.parseInt(value1);
        int num2 = Integer.parseInt(value2);

        for (int i = num1; i <= num2; i++) {
            System.out.print(i + " ");
        }
        System.out.println();
    }
}
