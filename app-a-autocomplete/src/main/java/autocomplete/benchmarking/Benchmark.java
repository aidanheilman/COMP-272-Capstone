package autocomplete.benchmarking;

import autocomplete.DatasetReader;
import autocomplete.Dictionary;
import autocomplete.Solution1;
import autocomplete.Solution2RBTree;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import java.io.IOException;
import java.nio.file.Path;

public class Benchmark {
    static final int WARMUP_RUNS = 5;
    static final int MEASURED_RUNS = 7;

    /** Measures contains calls and prints one row for each measured run. */
    static void measure(String solution, Supplier<Dictionary> factory,
                        List<DatasetReader.WordCount> words, List<String> queries, int n) {
        long[] times = new long[MEASURED_RUNS];
        long[] rotations = new long[MEASURED_RUNS];
        int[] heights = new int[MEASURED_RUNS];
        long checksum = 0;                         // prevents dead-code elimination

        for (int run = 0; run < WARMUP_RUNS + MEASURED_RUNS; run++) {
            Dictionary dictionary = factory.get(); // a fresh structure each run

            setInstrumentationEnabled(dictionary, true); //enable & reset counters for build
            resetInstrumentationCounters(dictionary);
            for (DatasetReader.WordCount word : words) {
                dictionary.add(word.word(), word.count());
            }
            long buildRotations = getRotationCount(dictionary);
            int buildHeight = getTreeHeight(dictionary);
            setInstrumentationEnabled(dictionary, false);

            long start = System.nanoTime(); // BEGIN MEASUREMENT
            for (String query : queries) {          // a batch of contains calls
                if (dictionary.contains(query)) {
                    checksum++;
                }
            }
            long elapsed = System.nanoTime() - start; // END MEASUREMENT

            if (run >= WARMUP_RUNS) {              // ignore the warm-up runs
                int measuredRun = run - WARMUP_RUNS;
                times[measuredRun] = elapsed;
                rotations[measuredRun] = buildRotations;
                heights[measuredRun] = buildHeight;
            }
        }

        System.err.println("contains hits = " + checksum); // sanity check
        for (int run = 0; run < MEASURED_RUNS; run++) {
            double nsPerContains = (double) times[run] / queries.size();
            System.out.printf("%s,%s,%d,%d,%.1f,%d,%d%n",
                    solution, "contains", n, run + 1, nsPerContains, rotations[run], heights[run]);
        }
    }
    
    /* Stess test - lexicographically sorted insertion */
    // add words from count_1w_sorted.txt - config in main or here?
    // time the additions and measure the counters for additions separately
    static void stressTest(String solution, Supplier<Dictionary> factory,
                        List<DatasetReader.WordCount> words, int n) {
        long[] times = new long[MEASURED_RUNS];
        long[] rotations = new long[MEASURED_RUNS];
        int[] heights = new int[MEASURED_RUNS];
        int checksum = 0; // not necessary for lost vals, but good for test progress
        
        /* Measure time */
        for (int run = 0; run < WARMUP_RUNS + MEASURED_RUNS; run++) {
            Dictionary dictionary = factory.get(); // a fresh structure each run

            long start = System.nanoTime(); //START TIMER
            for (DatasetReader.WordCount word : words) {
                dictionary.add(word.word(), word.count());
            }
            long elapsed = System.nanoTime() - start; // END TIMER

            if (run >= WARMUP_RUNS) {              // ignore the warm-up runs
                int measuredRun = run - WARMUP_RUNS;
                times[measuredRun] = elapsed;
            }
        }
        
        /* Measure rotations/height */
        for (int run = 0; run < WARMUP_RUNS + MEASURED_RUNS; run++) {
            Dictionary dictionary = factory.get(); // a fresh structure each run

            setInstrumentationEnabled(dictionary, true); //enable & reset counters for build
            resetInstrumentationCounters(dictionary);
            for (DatasetReader.WordCount word : words) {
                dictionary.add(word.word(), word.count());
                checksum ++; // add checksum here so as to not skew timed runs
            }
            long buildRotations = getRotationCount(dictionary);
            int buildHeight = getTreeHeight(dictionary);
            setInstrumentationEnabled(dictionary, false);

            if (run >= WARMUP_RUNS) {              // ignore the warm-up runs
                int measuredRun = run - WARMUP_RUNS;
                rotations[measuredRun] = buildRotations;
                heights[measuredRun] = buildHeight;
            }                
        }

        /* Compute and print results */
        System.err.println("stress-test lines processed = " + checksum); // sanity check
        for (int run = 0; run < MEASURED_RUNS; run++) {
            double nsPerAdd = (double) times[run] / words.size();
            System.out.printf("%s,%s,%d,%d,%.1f,%d,%d%n",
                    solution, "stress_test", n, run + 1, nsPerAdd, rotations[run], heights[run]);
        }
    }

    static void updates(String solution, Supplier<Dictionary> factory,
                        List<DatasetReader.WordCount> words, List<String> queries, int n) {
        long[] times = new long[MEASURED_RUNS];
        long[] rotations = new long[MEASURED_RUNS];
        int[] heights = new int[MEASURED_RUNS];
        long checksum = 0;

        // every run block does 20 ops: 9 contains, 1 remove, 9 contains (10% add/remove)
        int updatePairs = queries.size() / 20;
        if (updatePairs == 0) {
            throw new IllegalArgumentException("At least 20 queries are required.");
        }

        List<String> updateWords = new ArrayList<>(updatePairs); // list == size of queries
        Set<String> existingWords = new HashSet<>();
        for (DatasetReader.WordCount word : words) {
            existingWords.add(word.word()); // add dataset reader into hash
        }
        for (int i = 0; i < updatePairs; i++) { // HELP
            String word = "__benchmark_update_" + i;
            while (existingWords.contains(word)) {
                word = "_" + word;
            }
            existingWords.add(word);
            updateWords.add(word);
        }

        int operationsPerRun = updatePairs * 20;

        /* Measure time */
        for (int run = 0; run < WARMUP_RUNS + MEASURED_RUNS; run++) {
            Dictionary dictionary = factory.get();
            for (DatasetReader.WordCount word : words) { // init dictionary, add data
                dictionary.add(word.word(), word.count());
            }

            long start = System.nanoTime();
            checksum += runUpdateOperations(dictionary, queries, updateWords); 
            long elapsed = System.nanoTime() - start;

            if (run >= WARMUP_RUNS) {
                times[run - WARMUP_RUNS] = elapsed;
            }
        }

        /* Measure rotations and final height separately from the timed workload */
        for (int run = 0; run < WARMUP_RUNS + MEASURED_RUNS; run++) {
            Dictionary dictionary = factory.get();
            for (DatasetReader.WordCount word : words) {
                dictionary.add(word.word(), word.count());
            }
            int startingSize = dictionary.size();

            resetInstrumentationCounters(dictionary);
            setInstrumentationEnabled(dictionary, true);
            runUpdateOperations(dictionary, queries, updateWords);
            setInstrumentationEnabled(dictionary, false);

            if (dictionary.size() != startingSize) {
                throw new IllegalStateException("Update workload changed the dictionary size.");
            }

            if (run >= WARMUP_RUNS) {
                int measuredRun = run - WARMUP_RUNS;
                rotations[measuredRun] = getRotationCount(dictionary);
                heights[measuredRun] = getTreeHeight(dictionary);
            }
        }

        System.err.println("update workload checksum = " + checksum);
        for (int run = 0; run < MEASURED_RUNS; run++) {
            double nsPerOperation = (double) times[run] / operationsPerRun;
            System.out.printf("%s,%s,%d,%d,%.1f,%d,%d%n",
                    solution, "updates_during_lookups", n, run + 1,
                    nsPerOperation, rotations[run], heights[run]);
        }
    }

    private static long runUpdateOperations(
            Dictionary dictionary, List<String> queries, List<String> updateWords) {
        long checksum = 0;
        int queryIndex = 0;

        for (int block = 0; block < updateWords.size(); block++) {
            for (int i = 0; i < 9; i++) {
                checksum += dictionary.contains(queries.get(queryIndex++)) ? 1 : 0;
            }

            String word = updateWords.get(block);
            dictionary.add(word, block + 1L);

            for (int i = 0; i < 9; i++) {
                checksum += dictionary.contains(queries.get(queryIndex++)) ? 1 : 0;
            }

            checksum += dictionary.remove(word) ? 1 : 0;
        }

        return checksum;
    }
    private static void setInstrumentationEnabled(Dictionary dictionary, boolean enabled) {
        if (dictionary instanceof Solution1 tree) {
            tree.setInstrumentationEnabled(enabled);
        } else if (dictionary instanceof Solution2RBTree tree) {
            tree.setInstrumentationEnabled(enabled);
        } else {
            throw new IllegalArgumentException("Unsupported dictionary: "
                    + dictionary.getClass().getName());
        }
    }

    private static void resetInstrumentationCounters(Dictionary dictionary) {
        if (dictionary instanceof Solution1 tree) {
            tree.resetInstrumentationCounters();
        } else if (dictionary instanceof Solution2RBTree tree) {
            tree.resetInstrumentationCounters();
        } else {
            throw new IllegalArgumentException("Unsupported dictionary: "
                    + dictionary.getClass().getName());
        }
    }

    private static long getRotationCount(Dictionary dictionary) {
        if (dictionary instanceof Solution1 tree) {
            return tree.getRotationCount();
        } else if (dictionary instanceof Solution2RBTree tree) {
            return tree.getRotationCount();
        }
        throw new IllegalArgumentException("Unsupported dictionary: "
                + dictionary.getClass().getName());
    }

    private static int getTreeHeight(Dictionary dictionary) {
        if (dictionary instanceof Solution1 tree) {
            return tree.getTreeHeight();
        } else if (dictionary instanceof Solution2RBTree tree) {
            return tree.getTreeHeight();
        }
        throw new IllegalArgumentException("Unsupported dictionary: "
                + dictionary.getClass().getName());
    }

    public static void main(String[] args) throws IOException {
        // set paths for datasets
        Path datasetPath = Path.of("data", "count_1w.txt");
        Path datasetSortedPath = Path.of("data", "count_1w_sorted.txt");
        // read entire datasets into lists, ensure lists not empty
        List<DatasetReader.WordCount> allWords = 
                DatasetReader.readWordCounts(datasetPath, Integer.MAX_VALUE);
        if (allWords.isEmpty()) {
            throw new IllegalArgumentException("The dataset contains no valid words: " + datasetPath);
        }
        List<DatasetReader.WordCount> allSortedWords = 
                DatasetReader.readWordCounts(datasetSortedPath, Integer.MAX_VALUE);
        if (allSortedWords.isEmpty()) {
            throw new IllegalArgumentException("The dataset contains no valid words: " + datasetSortedPath);
        }

        // generate 100,000 queries using the dataset
        List<String> queries = sampleQueries(allWords, 100_000, 42);
        // CSV header
        System.out.println("solution,workload,n,run,ns_per_operation,rotations,height");
        // for each sublist view (10^2, 10^3, 10^4, 10^5, and entire wordlist), 
        // run benchmark on both solutions 

        /* CONTAINS (adapted from example) */
        for (int n : new int[] {100, 1_000, 10_000, 100_000, allWords.size()}) {
            List<DatasetReader.WordCount> words = allWords.subList(0, n);
            measure("avl", Solution1::new, words, queries, n);
            measure("red-black", Solution2RBTree::new, words, queries, n);
        }

        /* MIXED UPDATES */
        for (int n : new int[] {100, 1_000, 10_000, 100_000, allWords.size()}) {
            List<DatasetReader.WordCount> words = allWords.subList(0, n);
            updates("avl", Solution1::new, words, queries, n);
            updates("red-black", Solution2RBTree::new, words, queries, n);
        }

        /* STRESS TEST */
        for (int n : new int[] {100, 1_000, 10_000, 100_000, allSortedWords.size()}) {
            List<DatasetReader.WordCount> words = allSortedWords.subList(0, n);
            stressTest("avl", Solution1::new, words, n);
            stressTest("red-black", Solution2RBTree::new, words, n);
        }
    }

    private static List<String> sampleQueries(
            List<DatasetReader.WordCount> words, int queryCount, long seed) {
        Random random = new Random(seed);
        List<String> queries = new java.util.ArrayList<>(queryCount);
        for (int i = 0; i < queryCount; i++) {
            queries.add(words.get(random.nextInt(words.size())).word());
        }
        return queries;
    }
}

// write workload method for random add/del operations mixed in w contains (10%)
// merge Adnan's methods
// write workload method for stress test -> lexicographic insertion // DONE