package autocomplete;

import java.util.List;

/**
 * The dictionary service of Application A (autocomplete and spell-checking).
 *
 * <p>This interface IS the functional requirements specification. Both of your
 * solutions ({@link Solution1} and {@link Solution2}) must implement it exactly
 * as documented here. Do not change the method names or signatures.
 *
 * <p>Words are case-sensitive: the dictionary stores them exactly as given.
 * Converting text to lowercase and removing punctuation is the job of the code
 * that reads the dataset, not of the dictionary.
 */
public interface Dictionary {

    /**
     * Adds a word with its frequency count. If the word is already present,
     * its count is replaced by the new one (the size does not change).
     *
     * @param word  a non-empty word
     * @param count its frequency, at least 0
     */
    void add(String word, long count);

    /**
     * Removes a word.
     *
     * @return true if the word was present (and is now removed), false otherwise
     */
    boolean remove(String word);

    /** Returns true if the word is in the dictionary. */
    boolean contains(String word);

    /** Returns the count of the word, or 0 if the word is not in the dictionary. */
    long frequency(String word);

    /**
     * Returns the k most frequent words that start with the given prefix.
     *
     * <ul>
     *   <li>The result is sorted by count, highest first.</li>
     *   <li>Words with the same count are sorted alphabetically
     *       (as {@link String#compareTo} does).</li>
     *   <li>A word counts as starting with itself: complete("cat", k) may return "cat".</li>
     *   <li>The empty prefix "" matches every word.</li>
     *   <li>If fewer than k words match, all of them are returned.</li>
     *   <li>If k is 0 or less, the result is an empty list.</li>
     * </ul>
     *
     * @return a new list; never null
     */
    List<String> complete(String prefix, int k);

    /** Returns the number of words in the dictionary. */
    int size();
}
