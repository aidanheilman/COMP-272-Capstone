package autocomplete;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

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
        // TODO
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Reads a Gutenberg book and returns its words, in order, converted to lowercase,
     * with punctuation removed. Skip the license text before the line starting with
     * "*** START OF" and after the line starting with "*** END OF".
     * Document in your report how you handle apostrophes, hyphens and digits.
     */
    public static List<String> readBookWords(Path file) throws IOException {
        // TODO
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
