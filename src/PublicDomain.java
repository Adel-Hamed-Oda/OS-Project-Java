
import java.util.*;

public class PublicDomain {
    public static final boolean REMOVE_FILES_AFTER_EXECUTION = true;

    public static final List<String> FILE_NAMES = Arrays.asList(
        "Program_1.txt",
        "Program_2.txt",
        "Program_3.txt"
    );

    public static final List<Integer> ARRIVAL_TIMES = Arrays.asList(0, 1, 4);

    public static final String SCHEDULING_TECHNIQUE = "MLFQ"; // Options: "HRRN", "MLFQ", "RR"

    public static final int TIME_QUANTUM = 2; // Only used for Round Robin
}
