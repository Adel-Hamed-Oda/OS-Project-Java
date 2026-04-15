package src;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class SystemCalls {
    // there are spaces at the end of the output, sheleha please
    public static String readFile(String fileName)  {
        StringBuilder content = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append(" ");
            }
            br.close();
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
        return content.toString();
    }

    public static void writeFile(String fileName, String data) {
        File file = new File(fileName);
        if (PublicDomain.REMOVE_FILES_AFTER_EXECUTION) {
            file.deleteOnExit();
        }

        try (FileWriter writer = new FileWriter(file)) {
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
