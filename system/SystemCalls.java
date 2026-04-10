package system;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import memory.Memory;

public class SystemCalls {
    public static String readFile(String fileName)  {
        StringBuilder content = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n");
            }
            br.close();
        } catch (IOException e) {
            
        }
        return content.toString();
    }

    public static void writeFile(String fileName, String data) {
        try {
            FileWriter writer = new FileWriter(fileName);
            writer.write(data);
            writer.close();
        }
        catch (IOException e) {
            
        }
    }

    public static void print(String data) {
        System.out.println(data);
    }

    public static String input() {
        Scanner sc = new Scanner(System.in);
        return sc.nextLine();
    }

    public static String readFromMemory(String var) {
        return Memory.readVariable(var, Scheduler.getCurrentProcessID());
    }

    public static void writeToMemory(String var, String value) {
        Memory.assignVariable(var, value, Scheduler.getCurrentProcessID());
    }
}
