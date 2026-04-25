
import java.util.*;

public class PublicDomain {
    public static final boolean REMOVE_FILES_AFTER_EXECUTION = true;

    public static List<String> FILE_NAMES = new ArrayList<>(Arrays.asList(
        "Program_1.txt",
        "Program_2.txt",
        "Program_3.txt"
    ));

    public static List<Integer> ARRIVAL_TIMES = new ArrayList<>(Arrays.asList(5, 1, 3));

    public static final String SCHEDULING_TECHNIQUE = "MLFQ"; // Options: "HRRN", "MLFQ", "RR"

    public static final int TIME_QUANTUM = 2; // Only used for Round Robin

    public static void configurePrograms(List<String> fileNames, List<Integer> arrivalTimes) {
        if (fileNames == null || arrivalTimes == null || fileNames.size() != arrivalTimes.size()) {
            throw new IllegalArgumentException("Program files and arrival times must be non-null and have the same size.");
        }

        FILE_NAMES = new ArrayList<>(fileNames);
        ARRIVAL_TIMES = new ArrayList<>(arrivalTimes);
    }
}
