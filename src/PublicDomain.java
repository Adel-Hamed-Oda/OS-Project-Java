
import java.util.*;

public class PublicDomain {
    public static final boolean REMOVE_FILES_AFTER_EXECUTION = true;

    public static List<String> FILE_NAMES = new ArrayList<>(Arrays.asList(
            "Program_1.txt",
            "Program_2.txt",
            "Program_3.txt"));

    public static List<Integer> ARRIVAL_TIMES = new ArrayList<>(Arrays.asList(5, 1, 3));

    public static final String SCHEDULING_TECHNIQUE = "MLFQ"; // Options: "HRRN", "MLFQ", "RR"

    public static final int TIME_QUANTUM = 2; // Only used for Round Robin

    private static class Pair<K, V> {
        private final K key;
        private final V value;

        public Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }
    }

    private static void sortNamesbyArrivalTimed(ArrayList<String> fileNames, ArrayList<Integer> arrivalTimes) {
        List<Pair<String, Integer>> pairs = new ArrayList<>();
        for (int i = 0; i < fileNames.size(); i++) {
            pairs.add(new Pair<>(fileNames.get(i), arrivalTimes.get(i)));
        }

        pairs.sort(Comparator.comparingInt(Pair::getValue));

        for (int i = 0; i < pairs.size(); i++) {
            fileNames.set(i, pairs.get(i).getKey());
            arrivalTimes.set(i, pairs.get(i).getValue());
        }
    }

    public static void configurePrograms(List<String> fileNames, List<Integer> arrivalTimes) {
        if (fileNames == null || arrivalTimes == null || fileNames.size() != arrivalTimes.size()) {
            throw new IllegalArgumentException(
                    "Program files and arrival times must be non-null and have the same size.");
        }
        ArrayList<String> fileNamesCopy = new ArrayList<>(fileNames);
        ArrayList<Integer> arrivalTimesCopy = new ArrayList<>(arrivalTimes);
        sortNamesbyArrivalTimed(fileNamesCopy, arrivalTimesCopy);

        FILE_NAMES = new ArrayList<>(fileNamesCopy);
        ARRIVAL_TIMES = new ArrayList<>(arrivalTimesCopy);
    }
}
