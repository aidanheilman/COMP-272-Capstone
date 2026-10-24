package autocomplete;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Tests shared by both solutions. They are written once, against the Dictionary
 * interface; Solution1Test and Solution2Test run them on each solution.
 *
 * <p>These tests are a STARTING POINT. You must add more (see the TODO list at the end).
 */
abstract class DictionaryTest {

    /** Returns a new, empty dictionary of the solution being tested. */
    protected abstract Dictionary createDictionary();

    private Dictionary dict;

    @BeforeEach
    void setUp() {
        dict = createDictionary();   // a fresh, empty dictionary before every test
    }

    // ------------------------------------------------------------------
    // add, contains, frequency, size
    // ------------------------------------------------------------------

    @Test
    void newDictionaryIsEmpty() {
        assertEquals(0, dict.size());
        assertFalse(dict.contains("the"));
    }

    @Test
    void addedWordIsContained() {
        dict.add("cat", 5);
        assertTrue(dict.contains("cat"));
        assertEquals(1, dict.size());
    }

    @Test
    void missingWordIsNotContainedAndHasFrequencyZero() {
        dict.add("cat", 5);
        assertFalse(dict.contains("dog"));
        assertEquals(0, dict.frequency("dog"));
    }

    @Test
    void frequencyReturnsTheCount() {
        dict.add("cat", 5);
        dict.add("dog", 12);
        assertEquals(5, dict.frequency("cat"));
        assertEquals(12, dict.frequency("dog"));
    }

    @Test
    void addingAnExistingWordReplacesItsCount() {
        dict.add("cat", 5);
        dict.add("cat", 40);
        assertEquals(40, dict.frequency("cat"));
        assertEquals(1, dict.size());
    }

    @Test
    void wordsAreCaseSensitive() {
        dict.add("Cat", 5);
        assertTrue(dict.contains("Cat"));
        assertFalse(dict.contains("cat"));
    }

    // ------------------------------------------------------------------
    // remove
    // ------------------------------------------------------------------

    @Test
    void removingAPresentWordReturnsTrueAndRemovesIt() {
        dict.add("cat", 5);
        dict.add("dog", 12);
        assertTrue(dict.remove("cat"));
        assertFalse(dict.contains("cat"));
        assertEquals(0, dict.frequency("cat"));
        assertEquals(1, dict.size());
        assertTrue(dict.contains("dog"));
    }

    @Test
    void removingAMissingWordReturnsFalse() {
        dict.add("cat", 5);
        assertFalse(dict.remove("dog"));
        assertEquals(1, dict.size());
    }

    @Test
    void removingTheLastWordEmptiesTheDictionary() {
        dict.add("cat", 5);
        dict.remove("cat");
        assertEquals(0, dict.size());
        assertEquals(List.of(), dict.complete("", 10));
    }

    // ------------------------------------------------------------------
    // complete
    // ------------------------------------------------------------------

    @Test
    void completeReturnsTheMostFrequentWordsFirst() {
        dict.add("the", 100);
        dict.add("they", 30);
        dict.add("then", 50);
        dict.add("there", 70);
        dict.add("cat", 1000);    // does not start with "th"
        assertEquals(List.of("the", "there", "then"), dict.complete("th", 3));
    }

    @Test
    void completeBreaksTiesAlphabetically() {
        dict.add("tea", 10);
        dict.add("ten", 10);
        dict.add("tab", 10);
        dict.add("toe", 20);
        assertEquals(List.of("toe", "tab", "tea", "ten"), dict.complete("t", 10));
    }

    @Test
    void completeIncludesThePrefixItselfWhenItIsAWord() {
        dict.add("car", 10);
        dict.add("cart", 5);
        assertEquals(List.of("car", "cart"), dict.complete("car", 5));
    }

    @Test
    void emptyPrefixMatchesEveryWord() {
        dict.add("b", 1);
        dict.add("a", 2);
        dict.add("c", 3);
        assertEquals(List.of("c", "a", "b"), dict.complete("", 10));
    }

    @Test
    void completeReturnsAllMatchesWhenFewerThanK() {
        dict.add("dog", 3);
        dict.add("door", 7);
        assertEquals(List.of("door", "dog"), dict.complete("do", 100));
    }

    @Test
    void completeWithNoMatchReturnsAnEmptyList() {
        dict.add("dog", 3);
        assertEquals(List.of(), dict.complete("x", 5));
    }

    @Test
    void completeWithKZeroReturnsAnEmptyList() {
        dict.add("dog", 3);
        assertEquals(List.of(), dict.complete("d", 0));
    }

    @Test
    void completeDoesNotReturnRemovedWords() {
        dict.add("sun", 9);
        dict.add("sum", 8);
        dict.remove("sun");
        assertEquals(List.of("sum"), dict.complete("su", 5));
    }

    // ------------------------------------------------------------------
    // Larger inputs
    // ------------------------------------------------------------------

    @Test
    void manyWordsAddedInSortedOrderAreAllFound() {
        // Sorted insertions are the worst case for an unbalanced BST,
        // and force a hash table to resize several times.
        int n = 20_000;
        for (int i = 0; i < n; i++) {
            dict.add(String.format("w%06d", i), i);
        }
        assertEquals(n, dict.size());
        for (int i = 0; i < n; i++) {
            assertEquals(i, dict.frequency(String.format("w%06d", i)));
        }
    }

    @Test
    void randomOperationsGiveTheSameResultsAsTreeMap() {
        // java.util.TreeMap is used here only as a REFERENCE to check your answers.
        TreeMap<String, Long> reference = new TreeMap<>();
        Random random = new Random(42);   // fixed seed: the test is repeatable
        for (int step = 0; step < 50_000; step++) {
            String word = "w" + random.nextInt(2_000);
            int operation = random.nextInt(3);
            if (operation == 0) {
                long count = random.nextInt(1_000);
                dict.add(word, count);
                reference.put(word, count);
            } else if (operation == 1) {
                assertEquals(reference.remove(word) != null, dict.remove(word), "remove " + word);
            } else {
                assertEquals(reference.getOrDefault(word, 0L), dict.frequency(word), "frequency " + word);
            }
            assertEquals(reference.size(), dict.size());
        }
    }

    // ------------------------------------------------------------------
    // Tests from the dataset
    // ------------------------------------------------------------------

    @Test
    @Disabled("Remove this line once DatasetReader.readWordCounts works "
            + "and count_1w.txt is in the data folder")
    void everyWordOfTheDatasetIsFoundWithItsCount() throws IOException {
        List<DatasetReader.WordCount> words =
                DatasetReader.readWordCounts(Path.of("data", "count_1w.txt"), 10_000);
        for (DatasetReader.WordCount wc : words) {
            dict.add(wc.word(), wc.count());
        }
        assertEquals(words.size(), dict.size());
        for (DatasetReader.WordCount wc : words) {
            assertTrue(dict.contains(wc.word()), wc.word());
            assertEquals(wc.count(), dict.frequency(wc.word()), wc.word());
        }
    }

    // ------------------------------------------------------------------
    // TODO: add your own tests. At least:
    //  - Tests whose expected values YOU read from the dataset. For example: open
    //    count_1w.txt, find the 5 most frequent words that start with "pro", and check
    //    that complete("pro", 5) returns exactly them, in that order.
    //  - A few words from a Gutenberg book that are NOT in the word list.
    //  - complete with prefixes of length 1, 2, 3 and 5 on the full word list.
    //  - A random comparison like the one above that also checks complete().
    //  - Any special case of YOUR implementation (e.g. resizing, deleting in open
    //    addressing, rebalancing after remove).
    // ------------------------------------------------------------------
}
