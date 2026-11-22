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
    static final int TOP_K = 10;

    @FunctionalInterface
    interface Workload {
        long run(Dictionary dictionary);
    }

    /** Measures contains calls and prints one row for each measured run. */
    static void measure(String solution, Supplier<Dictionary> factory,
                        List<DatasetReader.WordCount> words, List<String> queries, int n) {
        measureWorkload(solution, "contains", factory, words, queries.size(), n, dictionary -> {
            long hits = 0;
            for (String query : queries) {
                if (dictionary.contains(query)) {
                    hits++;
                }
            }
            return hits;
        });
    }

    /** Measures contains calls for generated one-character mutations of dataset words. */
    static void measureMisspellings(
            String solution,
            Supplier<Dictionary> factory,
            List<DatasetReader.WordCount> words,
            List<String> misspellings,
            int n) {
        measureWorkload(solution, "misspellings", factory, words, misspellings.size(), n, dictionary -> {
            long hits = 0;
            for (String query : misspellings) {
                if (dictionary.contains(query)) {
                    hits++;
                }
            }
            return hits;
        });
    }

    /** Measures autocomplete calls for generated prefixes of dataset words. */
    static void measurePrefixQueries(
            String solution,
            Supplier<Dictionary> factory,
            List<DatasetReader.WordCount> words,
            List<String> prefixes,
            int n) {
        measureWorkload(solution, "prefix_queries", factory, words, prefixes.size(), n, dictionary -> {
            long resultCount = 0;
            for (String prefix : prefixes) {
                resultCount += dictionary.complete(prefix, TOP_K).size();
            }
            return resultCount;
        });
    }

    /** Runs a batch workload on a freshly built dictionary and prints each measured run. */
    private static void measureWorkload(
            String solution,
            String workloadName,
            Supplier<Dictionary> factory,
            List<DatasetReader.WordCount> words,
            int operationCount,
            int n,
            Workload workload) {
        if (operationCount <= 0) {
            throw new IllegalArgumentException("A benchmark workload must contain operations.");
        }

        long[] buildTimes = new long[MEASURED_RUNS];
        long[] times = new long[MEASURED_RUNS];
        long[] memoryBytes = new long[MEASURED_RUNS];
        long[] rotations = new long[MEASURED_RUNS];
        int[] heights = new int[MEASURED_RUNS];
        long checksum = 0;

        for (int run = 0; run < WARMUP_RUNS + MEASURED_RUNS; run++) {
            long memoryBefore = usedMemory();
            Dictionary dictionary = factory.get();

            long buildStart = System.nanoTime();
            for (DatasetReader.WordCount word : words) {
                dictionary.add(word.word(), word.count());
            }
            long buildElapsed = System.nanoTime() - buildStart;
            long memoryAfter = usedMemory();

            long start = System.nanoTime();
            checksum += workload.run(dictionary);
            long elapsed = System.nanoTime() - start;

            Dictionary instrumentedDictionary = factory.get();
            setInstrumentationEnabled(instrumentedDictionary, true);
            resetInstrumentationCounters(instrumentedDictionary);
            for (DatasetReader.WordCount word : words) {
                instrumentedDictionary.add(word.word(), word.count());
            }
            long buildRotations = getRotationCount(instrumentedDictionary);
            int buildHeight = getTreeHeight(instrumentedDictionary);
            setInstrumentationEnabled(instrumentedDictionary, false);

            if (run >= WARMUP_RUNS) {
                int measuredRun = run - WARMUP_RUNS;
                buildTimes[measuredRun] = buildElapsed;
                times[measuredRun] = elapsed;
                memoryBytes[measuredRun] = Math.max(0, memoryAfter - memoryBefore);
                rotations[measuredRun] = buildRotations;
                heights[measuredRun] = buildHeight;
            }
        }

        System.err.println(workloadName + " checksum = " + checksum);
        for (int run = 0; run < MEASURED_RUNS; run++) {
            printResult(solution, workloadName, n, run + 1, buildTimes[run],
                    memoryBytes[run], times[run], operationCount,
                    rotations[run], heights[run]);
        }
    }

    /** Creates reproducible one-character mutations of randomly selected words. */
    private static List<String> generateMisspellings(
            List<DatasetReader.WordCount> words, int queryCount, long seed) {
        if (words.isEmpty()) {
            throw new IllegalArgumentException("Cannot generate queries from an empty word list.");
        }

        Random random = new Random(seed);
        List<String> misspellings = new ArrayList<>(queryCount);
        for (int i = 0; i < queryCount; i++) {
            char[] characters =
                    words.get(random.nextInt(words.size())).word().toCharArray();
            int position = random.nextInt(characters.length);
            characters[position] =
                    characters[position] == 'z' ? 'a' : (char) (characters[position] + 1);
            misspellings.add(new String(characters));
        }
        return misspellings;
    }

    /** Creates reproducible prefixes of length one through four from dataset words. */
    private static List<String> generatePrefixQueries(
            List<DatasetReader.WordCount> words, int queryCount, long seed) {
        if (words.isEmpty()) {
            throw new IllegalArgumentException("Cannot generate prefixes from an empty word list.");
        }

        Random random = new Random(seed);
        List<String> prefixes = new ArrayList<>(queryCount);
        for (int i = 0; i < queryCount; i++) {
            String word = words.get(random.nextInt(words.size())).word();
            int prefixLength = Math.min(word.length(), 1 + random.nextInt(4));
            prefixes.add(word.substring(0, prefixLength));
        }
        return prefixes;
    }
    
    /* Stess test - lexicographically sorted insertion */
    // add words from count_1w_sorted.txt - config in main or here?
    // time the additions and measure the counters for additions separately
    static void stressTest(String solution, Supplier<Dictionary> factory,
                        List<DatasetReader.WordCount> words, int n) {
        long[] times = new long[MEASURED_RUNS];
        long[] memoryBytes = new long[MEASURED_RUNS];
        long[] rotations = new long[MEASURED_RUNS];
        int[] heights = new int[MEASURED_RUNS];
        int checksum = 0; // not necessary for lost vals, but good for test progress
        
        /* Measure time */
        for (int run = 0; run < WARMUP_RUNS + MEASURED_RUNS; run++) {
            long memoryBefore = usedMemory();
            Dictionary dictionary = factory.get(); // a fresh structure each run

            long start = System.nanoTime(); //START TIMER
            for (DatasetReader.WordCount word : words) {
                dictionary.add(word.word(), word.count());
            }
            long elapsed = System.nanoTime() - start; // END TIMER

            if (run >= WARMUP_RUNS) {              // ignore the warm-up runs
                int measuredRun = run - WARMUP_RUNS;
                times[measuredRun] = elapsed;
                memoryBytes[measuredRun] =
                        Math.max(0, usedMemory() - memoryBefore);
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
            printResult(solution, "stress_test", n, run + 1, times[run],
                    memoryBytes[run], times[run], words.size(),
                    rotations[run], heights[run]);
        }
    }

    static void updates(String solution, Supplier<Dictionary> factory,
                        List<DatasetReader.WordCount> words, List<String> queries, int n) {
        long[] buildTimes = new long[MEASURED_RUNS];
        long[] times = new long[MEASURED_RUNS];
        long[] memoryBytes = new long[MEASURED_RUNS];
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
            long memoryBefore = usedMemory();
            Dictionary dictionary = factory.get();
            long buildStart = System.nanoTime();
            for (DatasetReader.WordCount word : words) { // init dictionary, add data
                dictionary.add(word.word(), word.count());
            }
            long buildElapsed = System.nanoTime() - buildStart;
            long memoryAfter = usedMemory();

            long start = System.nanoTime();
            checksum += runUpdateOperations(dictionary, queries, updateWords); 
            long elapsed = System.nanoTime() - start;

            if (run >= WARMUP_RUNS) {
                int measuredRun = run - WARMUP_RUNS;
                buildTimes[measuredRun] = buildElapsed;
                times[measuredRun] = elapsed;
                memoryBytes[measuredRun] = Math.max(0, memoryAfter - memoryBefore);
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
            printResult(solution, "updates_during_lookups", n, run + 1,
                    buildTimes[run], memoryBytes[run], times[run], operationsPerRun,
                    rotations[run], heights[run]);
        }
    }

    private static long usedMemory() {
        System.gc();
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static void printResult(
            String solution,
            String workload,
            int n,
            int run,
            long buildTimeNs,
            long memoryBytes,
            long workloadTimeNs,
            int operationCount,
            long rotations,
            int height) {
        double latencyNs = (double) workloadTimeNs / operationCount;
        double throughputOpsPerSecond =
                operationCount / (workloadTimeNs / 1_000_000_000.0);
        System.out.printf("%s,%s,%d,%d,%.3f,%.2f,%.1f,%.1f,%d,%d%n",
                solution, workload, n, run,
                buildTimeNs / 1_000_000.0,
                Math.max(0, memoryBytes) / 1024.0,
                latencyNs, throughputOpsPerSecond, rotations, height);
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
        System.out.println("solution,workload,n,run,build_ms,memory_kb,"
                + "latency_ns,throughput_ops_sec,rotations,height");
        // for each sublist view (10^2, 10^3, 10^4, 10^5, and entire wordlist), 
        // run benchmark on both solutions 

        /* CONTAINS */
        for (int n : new int[] {100, 1_000, 10_000, 100_000, allWords.size()}) {
            List<DatasetReader.WordCount> words = allWords.subList(0, n);
            measure("avl", Solution1::new, words, queries, n);
            measure("red-black", Solution2RBTree::new, words, queries, n);
        }

        /* MISSSPELLING */
        for (int n : new int[] {100, 1_000, 10_000, 100_000, allWords.size()}) {
            List<DatasetReader.WordCount> words = allWords.subList(0, n);
            List<String> misspellings = generateMisspellings(words, 100_000, 42);
            measureMisspellings("avl", Solution1::new, words, misspellings, n);
            measureMisspellings("red-black", Solution2RBTree::new, words, misspellings, n);
        }

        /* PREFIX-QUERY */
        for (int n : new int[] {100, 1_000, 10_000, 100_000, allWords.size()}) {
            List<DatasetReader.WordCount> words = allWords.subList(0, n);
            List<String> prefixes = generatePrefixQueries(words, 100_000, 42);
            measurePrefixQueries("avl", Solution1::new, words, prefixes, n);
            measurePrefixQueries("red-black", Solution2RBTree::new, words, prefixes, n);
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
        List<String> queries = new ArrayList<>(queryCount);
        for (int i = 0; i < queryCount; i++) {
            queries.add(words.get(random.nextInt(words.size())).word());
        }
        return queries;
    }
}