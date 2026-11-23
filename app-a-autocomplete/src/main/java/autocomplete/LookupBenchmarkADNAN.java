package autocomplete;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public class LookupBenchmarkADNAN {
    static final int WARMUP_RUNS = 5;
    static final int MEASURED_RUNS = 7;
    static final int QUERY_COUNT = 10_000;
    static final int TOP_K = 10;

    static class Entry {
        String word;
        long freq;

        Entry(String word, long freq) {
            this.word = word;
            this.freq = freq;
        }
    }

    static class Metrics {
        double buildMs;
        double memoryKb;
        double latencyNs;
        double throughputOpsPerSec;

        Metrics(double buildMs, double memoryKb, double latencyNs, double throughputOpsPerSec) {
            this.buildMs = buildMs;
            this.memoryKb = memoryKb;
            this.latencyNs = latencyNs;
            this.throughputOpsPerSec = throughputOpsPerSec;
        }
    }

    interface Workload {
        long run(Dictionary dict);
    }

    static Metrics runBenchmark(Supplier<Dictionary> factory, List<Entry> entries,
                                int opCount, Workload workload) {
        // Initialize arrays to store build time, query time, and memory for each measured run
        long[] buildNs = new long[MEASURED_RUNS];
        long[] queryNs = new long[MEASURED_RUNS];
        long[] memBytes = new long[MEASURED_RUNS];
        long checksum = 0;                         // prevents dead-code elimination

        // Run warmup iterations first, followed by measured iterations
        for (int run = 0; run < WARMUP_RUNS + MEASURED_RUNS; run++) {
            // Snapshot baseline heap memory before building the tree
            System.gc();
            long memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

            // Create a fresh dictionary and measure total insertion time (build time)
            Dictionary dict = factory.get();       // a fresh structure each run
            long t0 = System.nanoTime();
            for (Entry e : entries) {
                dict.add(e.word, e.freq);
            }
            long bTime = System.nanoTime() - t0;

            // Snapshot post-build heap memory to calculate memory footprint
            System.gc();
            long memAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

            // Execute the workload batch and measure total elapsed query time
            long t1 = System.nanoTime();
            checksum += workload.run(dict);
            long qTime = System.nanoTime() - t1;

            // Discard warmup runs; record metrics once warmed up
            if (run >= WARMUP_RUNS) {              // ignore the warm-up runs
                int i = run - WARMUP_RUNS;
                buildNs[i] = bTime;
                queryNs[i] = qTime;
                memBytes[i] = Math.max(0, memAfter - memBefore);
            }
        }
        System.err.println("checksum = " + checksum);

        // Sort results to find the median, then compute latency (time / ops) and throughput (ops / time)
        Arrays.sort(buildNs);
        Arrays.sort(queryNs);
        Arrays.sort(memBytes);

        int mid = MEASURED_RUNS / 2;
        double lat = (double) queryNs[mid] / opCount;
        return new Metrics(buildNs[mid] / 1e6, memBytes[mid] / 1024.0, lat, opCount / (queryNs[mid] / 1e9));
    }

    // Workload 1: run contains() on each misspelled word and count how many return true
    static Metrics measureMisspellings(Supplier<Dictionary> factory, List<Entry> entries, List<String> queries) {
        return runBenchmark(factory, entries, queries.size(), dict -> {
            long hits = 0;
            for (String q : queries) {
                if (dict.contains(q)) hits++;
            }
            return hits;
        });
    }

    // Workload 2: run complete(prefix, TOP_K) on each prefix and sum the result list sizes
    static Metrics measurePrefixQueries(Supplier<Dictionary> factory, List<Entry> entries, List<String> prefixes) {
        return runBenchmark(factory, entries, prefixes.size(), dict -> {
            long total = 0;
            for (String p : prefixes) {
                total += dict.complete(p, TOP_K).size();
            }
            return total;
        });
    }

    // Pick random words using a fixed seed and shift one character by +1 to create misspellings
    static List<String> generateMisspellings(List<Entry> entries, int count, long seed) {
        Random rng = new Random(seed);
        List<String> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            char[] c = entries.get(rng.nextInt(entries.size())).word.toCharArray();
            int pos = rng.nextInt(c.length);
            c[pos] = (c[pos] == 'z') ? 'a' : (char) (c[pos] + 1);
            list.add(new String(c));
        }
        return list;
    }

    // Pick random words using a fixed seed and slice prefixes of length 1 to 4
    static List<String> generatePrefixQueries(List<Entry> entries, int count, long seed) {
        Random rng = new Random(seed);
        List<String> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String w = entries.get(rng.nextInt(entries.size())).word;
            list.add(w.substring(0, Math.min(w.length(), 1 + rng.nextInt(4))));
        }
        return list;
    }

    // Read CSV lines, parse [word, freq], and skip blank lines or text header rows
    static List<Entry> loadEntries(String path) throws IOException {
        List<Entry> entries = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            for (String line; (line = br.readLine()) != null; ) {
                String[] parts = line.trim().split("\\s+");
                if (parts[0].isEmpty()) continue;
                try {
                    long freq = parts.length > 1 ? Long.parseLong(parts[1].trim()) : 1L;
                    entries.add(new Entry(parts[0].trim(), freq));
                } catch (NumberFormatException ignored) {
                    // skip header row if present
                }
            }
        }
        return entries;
    }

    // Print one formatted CSV row to stdout
    static void printRow(String sol, String work, int n, Metrics m) {
        System.out.printf("%s,%s,%d,%.3f,%.2f,%.1f,%.1f%n",
                sol, work, n, m.buildMs, m.memoryKb, m.latencyNs, m.throughputOpsPerSec);
    }

    public static void main(String[] args) throws IOException {
        // Verify CLI argument and load the full dataset from disk
        if (args.length < 1) {
            System.err.println("Usage: java autocomplete.LookupBenchmark <count_1w.txt filepath>");
            return;
        }
        List<Entry> all = loadEntries(args[0]);

        // Set scalability sizes (10^2 through full dataset) and print the CSV header
        int[] sizes = {100, 1_000, 10_000, 100_000, all.size()};
        System.out.println("solution,workload,n,build_ms,memory_kb,latency_ns,throughput_ops_sec");

        // Loop through each input size n, generate identical workloads, and benchmark both trees
        for (int i = 0; i < sizes.length; i++) {
            int n = sizes[i];
            if (n > all.size() || (i == sizes.length - 1 && n == 100_000)) continue;

            // Slice dataset to size n and build deterministic query lists
            List<Entry> sub = all.subList(0, n);
            List<String> miss = generateMisspellings(sub, QUERY_COUNT, 42);
            List<String> pref = generatePrefixQueries(sub, QUERY_COUNT, 42);

            // Measure and print results for Solution1 (AVL) and Solution2RBTree (Red-Black)
            printRow("Solution1", "misspellings", n, measureMisspellings(Solution1::new, sub, miss));
            printRow("Solution1", "prefix_queries", n, measurePrefixQueries(Solution1::new, sub, pref));
            printRow("Solution2RBTree", "misspellings", n, measureMisspellings(Solution2RBTree::new, sub, miss));
            printRow("Solution2RBTree", "prefix_queries", n, measurePrefixQueries(Solution2RBTree::new, sub, pref));
        }
    }
}
