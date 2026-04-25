
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

public class SystemCalls {
    private static final Object INPUT_LOCK = new Object();
    private static final LinkedBlockingQueue<String> GUI_INPUT_QUEUE = new LinkedBlockingQueue<>();
    private static volatile boolean awaitingInput = false;
    private static volatile Integer awaitingInputProcessID = null;

    public static String readFile(String fileName) {
        StringBuilder content = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append(" ");
            }
            br.close();
        } catch (IOException e) {
            Dashboard.appendProgramOutput("ERROR: File Not Found");
            System.out.println("Error reading file: " + e.getMessage());
        } catch (Exception e) {
            Dashboard.appendProgramOutput("ERROR");
            System.out.println("Unexpected error: " + e.getMessage());
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
                        Dashboard.appendProgramOutput("ERROR");
            System.out.println("Error writing to file: " + e.getMessage());
        } catch (Exception e) {
            Dashboard.appendProgramOutput("ERROR");
            System.out.println("Unexpected error: " + e.getMessage());
        }
    }

    public static void print(String data) {
        Dashboard.appendProgramOutput(data);
        System.out.println(data);
    }

    public static String input() {
        synchronized (INPUT_LOCK) {
            awaitingInput = true;
            awaitingInputProcessID = Scheduler.getCurrentProcessID();
            GUI_INPUT_QUEUE.clear();
        }

        try {
            return GUI_INPUT_QUEUE.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "";
        } finally {
            synchronized (INPUT_LOCK) {
                awaitingInput = false;
                awaitingInputProcessID = null;
            }
        }
    }

    public static boolean isAwaitingInput() {
        return awaitingInput;
    }

    public static Integer getAwaitingInputProcessID() {
        return awaitingInputProcessID;
    }

    public static boolean provideInput(String inputValue) {
        if (!awaitingInput) {
            return false;
        }
        GUI_INPUT_QUEUE.offer(inputValue == null ? "" : inputValue);
        return true;
    }

    public static String readFromMemory(String var) {
        return Memory.getVariable(Scheduler.getCurrentProcessID(), var);
    }

    public static void writeToMemory(String var, String value) {
        Memory.setVariable(Scheduler.getCurrentProcessID(), var, value);
    }

    public static void printFromTo(String var1, String var2) {
        try {
            int pid = Scheduler.getCurrentProcessID();

            String value1 = Memory.getVariable(pid, var1);
            String value2 = Memory.getVariable(pid, var2);

            int num1 = Integer.parseInt(value1);
            int num2 = Integer.parseInt(value2);

            StringBuilder output = new StringBuilder();
            for (int i = num1; i <= num2; i++) {
                if (output.length() > 0) {
                    output.append(' ');
                }
                output.append(i);
            }

            Dashboard.appendProgramOutput(output.toString());
            System.out.println(output);
        } catch (NumberFormatException e) {
            Dashboard.appendProgramOutput("ERROR: Invalid Input");
            System.out.println("Error: Variables must contain valid integers for printFromTo.");
        } catch (Exception e) {
            Dashboard.appendProgramOutput("ERROR");
            System.out.println("Error in printFromTo: " + e.getMessage());
        }
    }
}
