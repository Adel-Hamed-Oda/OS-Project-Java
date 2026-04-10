import memory.Memory;

import process.ProcessController;

public class Main {

    // example: file1 loadtime1 file2 loadtime2 file3 loadtime3
    public static void main(String[] args) {
        Memory.Init_Memory();

        String[] fileNames = new String[args.length / 2];
        for (int i = 0; i < args.length; i += 2) {
            fileNames[i / 2] = args[i];
        }

        String[] loadTimes = new String[args.length / 2];
        for (int i = 1; i < args.length; i += 2) {
            loadTimes[(i / 2) + 1] = args[i];
        }

        for (String fileName : fileNames) {
            ProcessController.AddNewProcess(fileName);
        }
    }
}
