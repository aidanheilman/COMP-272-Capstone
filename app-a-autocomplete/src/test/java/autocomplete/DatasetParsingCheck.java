package autocomplete;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

// Supplemental data parsing test written by GitHub Copilot

/**
 * Plain-Java check program for the Application A data files.
 *
 * This file intentionally uses only the Java standard library; it does not use
 * Maven or JUnit. Run it from the app-a-autocomplete directory with:
 *   javac -d out src/main/java/autocomplete/DatasetReader.java src/test/java/autocomplete/DatasetParsingCheck.java
 *   java -cp out autocomplete.DatasetParsingCheck
 */

public final class DatasetParsingCheck {

    private DatasetParsingCheck() { }

    public static void main(String[] args) throws IOException {
        Path dataDirectory = args.length == 0 ? Path.of("data") : Path.of(args[0]);

        System.out.println("=== Application A dataset parsing check ===");
        System.out.println("Data directory: " + dataDirectory.toAbsolutePath());
        System.out.println();

        checkWordCountFile(dataDirectory.resolve("count_1w.txt"));

        String[] bookNames = {"pg84.txt", "pg100.txt", "pg1661.txt", "pg2701.txt"};
        for (String bookName : bookNames) {
            checkBookFile(dataDirectory.resolve(bookName));
        }

        System.out.println();
        System.out.println("All data files were present and produced parsed output.");
    }

    private static void checkWordCountFile(Path file) throws IOException {
        System.out.println("WORD-COUNT FILE: " + file.getFileName());
        requireFile(file);

        int physicalLines = 0;
        int parsedRows = 0;
        int missingTabOrValue = 0;
        int invalidCount = 0;
        int negativeCount = 0;

        // Independently inspect every physical line so this check reports exactly which
        // kinds of rows the implemented parser accepted or skipped.
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                physicalLines++;
                int tabIndex = line.indexOf('\t');

                if (tabIndex <= 0 || tabIndex == line.length() - 1) {
                    missingTabOrValue++;
                    continue;
                }

                String countText = line.substring(tabIndex + 1).trim();
                try {
                    long count = Long.parseLong(countText);
                    if (count < 0) {
                        negativeCount++;
                    } else {
                        parsedRows++;
                    }
                } catch (NumberFormatException ex) {
                    invalidCount++;
                }
            }
        }

        // Exercise the actual implementation for the complete file, not only the
        // independent line-count inspection performed above.
        List<DatasetReader.WordCount> records =
                DatasetReader.readWordCounts(file, Integer.MAX_VALUE);
        if (records.isEmpty()) {
            throw new AssertionError("No records were parsed from " + file);
        }

        int skippedRows = physicalLines - parsedRows;
        System.out.println("  Physical lines read:        " + physicalLines);
        System.out.println("  Rows parsed successfully:   " + parsedRows);
        System.out.println("  Rows skipped in total:      " + skippedRows);
        System.out.println("  Skipped: missing TAB/value: " + missingTabOrValue);
        System.out.println("  Skipped: invalid count:     " + invalidCount);
        System.out.println("  Skipped: negative count:    " + negativeCount);
        System.out.println("  Actual parser record count: " + records.size());
        System.out.println("  First parsed record:        " + records.get(0));
        System.out.println("  Last parsed record:         " + records.get(records.size() - 1));

        if (records.size() != parsedRows) {
            throw new AssertionError("Parser returned " + records.size()
                    + " records, but independent validation found " + parsedRows);
        }

        System.out.println();
    }

    private static void checkBookFile(Path file) throws IOException {
        System.out.println("BOOK FILE: " + file.getFileName());
        requireFile(file);

        List<String> words = DatasetReader.readBookWords(file);
        if (words.isEmpty()) {
            throw new AssertionError("No words were parsed from " + file);
        }

        int totalLines = 0;
        int licenseLinesBeforeStart = 0;
        int bookBodyLines = 0;
        int blankBookLines = 0;
        int licenseLinesAfterEnd = 0;
        boolean insideBook = false;
        boolean foundStart = false;
        boolean foundEnd = false;

        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                totalLines++;

                if (!insideBook) {
                    // Before the opening marker, all lines belong to excluded front matter.
                    if (line.startsWith("*** START OF")) {
                        foundStart = true;
                        insideBook = true;
                        // Count the marker itself with the license/front-matter lines.
                        licenseLinesBeforeStart++;
                    } else {
                        licenseLinesBeforeStart++;
                    }
                    continue;
                }

                if (line.startsWith("*** END OF")) {
                    foundEnd = true;
                    insideBook = false;
                    licenseLinesAfterEnd++;

                    // Once the end marker is seen, all lines that follow are excluded
                    // license/back-matter lines.
                    String remainingLine;
                    while ((remainingLine = reader.readLine()) != null) {
                        totalLines++;
                        licenseLinesAfterEnd++;
                    }
                    break;
                }

                bookBodyLines++;
                String normalized = line.toLowerCase(java.util.Locale.ROOT)
                        .replaceAll("[^\\p{L}]", " ").trim();
                if (normalized.isEmpty()) {
                    blankBookLines++;
                }
            }
        }

        if (!foundStart) {
            throw new AssertionError(file + " has no Gutenberg START marker");
        }
        if (!foundEnd) {
            throw new AssertionError(file + " has no Gutenberg END marker");
        }

        int bodyLinesWithWords = bookBodyLines - blankBookLines;
        int licenseOrMarkerLinesSkipped = licenseLinesBeforeStart + licenseLinesAfterEnd;

        System.out.println("  Total physical lines:       " + totalLines);
        System.out.println("  License/marker lines skipped: " + licenseOrMarkerLinesSkipped);
        System.out.println("  Book body lines read:       " + bookBodyLines);
        System.out.println("  Body lines with words:      " + bodyLinesWithWords);
        System.out.println("  Blank/no-letter body lines: " + blankBookLines);
        System.out.println("  Individual words parsed:    " + words.size());
        System.out.println("  First parsed words:         " + preview(words, 8));
        System.out.println("  Last parsed words:          " + previewLast(words, 8));

        int accounting = licenseOrMarkerLinesSkipped + bookBodyLines;
        if (accounting != totalLines) {
            throw new AssertionError("Line accounting mismatch for " + file
                    + ": " + accounting + " != " + totalLines);
        }

        System.out.println();
    }

    private static List<String> preview(List<String> words, int count) {
        int size = Math.min(count, words.size());
        return words.subList(0, size);
    }

    private static List<String> previewLast(List<String> words, int count) {
        int start = Math.max(0, words.size() - count);
        return words.subList(start, words.size());
    }

    private static void requireFile(Path file) {
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Missing data file: " + file.toAbsolutePath());
        }
    }
}
