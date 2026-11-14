package autocomplete;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reads the datasets of Application A. You must write the two methods below.
 *
 * <p>Datasets (plain text files):
 * <ul>
 *   <li>count_1w.txt from https://norvig.com/ngrams/ — one line per word:
 *       the word, a TAB character, and its count. Example line: {@code the<TAB>23135851162}</li>
 *   <li>Books from https://www.gutenberg.org/ — download the "Plain Text UTF-8" version.</li>
 * </ul>
 *
 * <p>Hints:
 * <ul>
 *   <li>{@code java.nio.file.Files.newBufferedReader(file)} gives a reader;
 *       call {@code readLine()} until it returns null.</li>
 *   <li>{@code line.split("\t")} splits a line at the TAB;
 *       {@code Long.parseLong(text)} converts the count.</li>
 * </ul>
 */
public final class DatasetReader {

    /** One line of the word list. (A "record" is a small class that only holds data.) */
    public record WordCount(String word, long count) { }

    private DatasetReader() { }   // no objects: only static methods

    /**
     * Reads at most maxLines lines of the word-count file, in file order.
     * Use maxLines = Integer.MAX_VALUE to read the whole file.
     */
    public static List<WordCount> readWordCounts(Path file, int maxLines) throws IOException {
        // The caller uses zero or a negative limit to request no records. Returning an
        // empty, mutable ArrayList here also gives the method a consistent return type.
        if (maxLines <= 0) {
            return new ArrayList<>();
        }

        List<WordCount> records = new ArrayList<>();

        // Files.newBufferedReader opens the supplied path using the platform's default
        // character encoding. This try-with-resources block closes the reader even if
        // reading the file or parsing a row causes an exception.
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            int linesRead = 0;

            // readLine() returns null after the final line. We stop after maxLines physical
            // lines, whether or not each of those lines produced a valid WordCount record.
            while (linesRead < maxLines && (line = reader.readLine()) != null) {
                linesRead++;

                // indexOf finds the first TAB. A valid row must have text before and after
                // that TAB: "the\t23135851162", for example.
                int tabIndex = line.indexOf('\t');
                if (tabIndex <= 0 || tabIndex == line.length() - 1) {
                    continue;
                }

                String word = line.substring(0, tabIndex).trim();
                String countText = line.substring(tabIndex + 1).trim();
                if (word.isEmpty() || countText.isEmpty()) {
                    continue;
                }

                try {
                    // Long.parseLong accepts a decimal integer in the range of Java's long.
                    // Counts in this dataset should not be negative, so negative rows are
                    // ignored while positive rows and zero are retained.
                    long count = Long.parseLong(countText);
                    if (count >= 0) {
                        records.add(new WordCount(word, count));
                    }
                } catch (NumberFormatException ignored) {
                    // One malformed data row does not invalidate the rest of the file.
                    // The test program reports malformed rows separately while the parser
                    // simply skips them and continues.
                }
            }
        }

        return records;
    }

    /**
     * Reads a Gutenberg book and returns its words, in order, converted to lowercase,
     * with punctuation removed. Skip the license text before the line starting with
     * "*** START OF" and after the line starting with "*** END OF".
     * Document in your report how you handle apostrophes, hyphens and digits.
     */
    public static List<String> readBookWords(Path file) throws IOException {
        List<String> words = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            boolean insideBook = false;

            while ((line = reader.readLine()) != null) {
                // Lines before the opening marker are license/title material. They are
                // deliberately ignored rather than included in the resulting word list.
                if (!insideBook) {
                    if (line.startsWith("*** START OF")) {
                        insideBook = true;
                    }
                    continue;
                }

                // The first end marker closes the Gutenberg body. Any remaining text,
                // usually a license, is excluded by stopping the read completely.
                if (line.startsWith("*** END OF")) {
                    break;
                }

                // Locale.ROOT makes lowercase conversion predictable and independent of
                // the computer's default language or region settings.
                String normalized = line.toLowerCase(Locale.ROOT);

                // Every run of characters that is not a Unicode letter is replaced by a
                // single space. This deliberately treats punctuation, whitespace, apostrophes,
                // and hyphens as separators, and removes digits rather than retaining them as
                // words. For example: "don't-stop 123" becomes two spaces between "don", "t",
                // "stop", so the split below yields don, t, and stop.
                normalized = normalized.replaceAll("[^\\p{L}]", " ");

                // A line may contain several words or no words at all. split whitespace into
                // tokens and append every non-empty token in original line order.
                String[] tokens = normalized.trim().isEmpty()
                        ? new String[0]
                        : normalized.trim().split("\\s+");
                for (String token : tokens) {
                    if (!token.isEmpty()) {
                        words.add(token);
                    }
                }
            }
        }

        // If a file has no opening marker, the loop never enters the body and this method
        // correctly returns an empty list rather than accidentally treating license text as
        // a book.
        return words;
    }
}
