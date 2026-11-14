package autocomplete;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
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

    @Test // This is what is reffered to below // Add complete() to this mix of methods
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
    //@Disabled("Remove this line once DatasetReader.readWordCounts works "
    //        + "and count_1w.txt is in the data folder")
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
    // ADNAN DOES BELOW 
    //  1. Tests whose expected values YOU read from the dataset. For example: open
    //    count_1w.txt, find the 5 most frequent words that start with "pro", and check
    //    that complete("pro", 5) returns exactly them, in that order.
    //  2. A few words from a Gutenberg book that are NOT in the word list. // ASK PROFESSOR 
    //     -- Run words through the methods that are not in wordlist to test behavior
    //     -- Words should be from Gutenberg book  
    //
    // AIDAN DOES BELOW
    //  3. complete with prefixes of length 1, 2, 3 and 5 on the full word list.
    //  4. A random comparison like the one above that also checks complete(). // ASK PROFESSOR
    //     -- see above 
    //  5. Any special case of YOUR implementation (e.g. resizing, deleting in open
    //     addressing, rebalancing after remove).
    // ------------------------------------------------------------------

    /* ADDITIONAL TESTS START HERE */

    /* Test 1 */
    /** Helper to load the dataset from count_1w.txt into dict. */
    private void loadDataset(int limit) throws IOException {
        List<DatasetReader.WordCount> words =
                DatasetReader.readWordCounts(Path.of("data", "count_1w.txt"), limit);
        for (DatasetReader.WordCount wc : words) {
            dict.add(wc.word(), wc.count());
        }
    }

    @Test
    void completeProReturnsTopFiveFromDataset() throws IOException {
        loadDataset(10_000);

        // Verified directly from data/count_1w.txt
        List<String> expected = List.of(
                "products",
                "product",
                "program",
                "project",
                "profile"
        );
        assertEquals(expected, dict.complete("pro", 5));
    }


    /* Test 2 */
    @Test
    void gutenbergWordsNotInDatasetWorkAcrossAllMethods() throws IOException {
        loadDataset(10_000);
        int initialSize = dict.size();

        // Rare words from Project Gutenberg books absent from count_1w.txt:
        // "uproariousness" (Tom Jones), "sesquipedalianism" (Anatomy of Melancholy),
        // and "unconscionableness" (Pamela)
        List<String> absentWords = List.of(
                "uproariousness",
                "sesquipedalianism",
                "unconscionableness"
        );

        for (String word : absentWords) {
            // 1. Verify initial absence across all query/delete methods
            assertFalse(dict.contains(word), "Should not contain: " + word);
            assertEquals(0, dict.frequency(word), "Frequency should be 0: " + word);
            assertEquals(List.of(), dict.complete(word, 5), "No completions expected: " + word);
            assertFalse(dict.remove(word), "Removing absent word should return false: " + word);
            assertEquals(initialSize, dict.size());

            // 2. Add the word and verify it is now found and returned by complete
            dict.add(word, 42);
            assertTrue(dict.contains(word), "Should contain after add: " + word);
            assertEquals(42, dict.frequency(word), "Frequency should match added count: " + word);
            assertEquals(List.of(word), dict.complete(word, 5), "Should complete to itself: " + word);
            assertEquals(initialSize + 1, dict.size());
            
            // 3. Remove the word and verify it is completely gone again
            assertTrue(dict.remove(word), "Removing present word should return true: " + word);
            assertFalse(dict.contains(word), "Should not contain after remove: " + word);
            assertEquals(0, dict.frequency(word), "Frequency should reset to 0: " + word);
            assertEquals(List.of(), dict.complete(word, 5), "No completions after remove: " + word);
            assertEquals(initialSize, dict.size());
        }
    }


    /* Test 3 */
    /* Checks complete() method against full wordlist with prefixes of len 1, 2, 3, && 5 */
    @Test
    void completeMethodAgainstWordlist() throws IOException {
        List<DatasetReader.WordCount> words =
            DatasetReader.readWordCounts(Path.of("data", "count_1w.txt"), Integer.MAX_VALUE); // init 
        for (DatasetReader.WordCount wc : words) {
            dict.add(wc.word(), wc.count()); // add all
        }
        // run complete() prefixes == 1,2,3,5 with k == 3
        // need to find words and note by hand what expected outcomes are for assert.equals statemets
        // could use grep for this
            /**
             * GREP RESULTS 
             * 'w' top 3 frequency are with, was, we
             * 'mo' top 3 frequency are more, most, money
             * 'cli' top 3 frequency are click, client, clients
             * 'radio' top 3 frequency are radio, radios, radioactive
            */
        assertEquals(List.of("with", "was", "we"), dict.complete("w", 3));
        assertEquals(List.of("more", "most", "money"), dict.complete("mo", 3));
        assertEquals(List.of("click", "client", "clients"), dict.complete("cli", 3));
        assertEquals(List.of("radio", "radios", "radioactive"), dict.complete("radio", 3));
    }
    
    /* Test 4 */
    // basically take above random test and check it with addition of complete
    // Use loops and a list or set to add the words
    @Test // This is what is reffered to below // Add complete() to this mix of methods
    void randomOperationsGiveTheSameResultsViaComplete() {
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

            // Randomly check autocomplete every 100 operations.
            if (step % 100 == 0) {
                List<String> expected = new ArrayList<>();
                String prefix = "w" + random.nextInt(100); // prefix will be w0 through w99
                int k = random.nextInt(11); // k can be 0-10

                // Only run sorting logic if k is not 0 
                if (k != 0) {
                // Collect the words that currently match the random prefix.
                    for (String candidate : reference.keySet()) {
                        if (candidate.startsWith(prefix)) {
                        expected.add(candidate);
                        }
                    }
                // Rank matches by frequency descending, then alphabetically ascending.
                // Uses an internal comparator object passed into java's .sort() function
                // Comparator is used to sort the list in place by frequency, rather than lexicographically
                    expected.sort(new Comparator<String>() {
                        @Override
                        public int compare(String first, String second) {
                            int frequencyOrder =
                                Long.compare(reference.get(second), reference.get(first));

                            if (frequencyOrder != 0) {
                                return frequencyOrder;
                            }

                            return first.compareTo(second);
                        }
                    });

                    // Keep only the first k words, if there are more than k matches.
                    if (expected.size() > k) {
                        expected = new ArrayList<>(expected.subList(0, k));
                    }
                }
            // Compare the expected answer with the dictionary's result.
            assertEquals(expected, dict.complete(prefix, k));   
            }
        }
    }

    /* Test 5 */
    // Internal testing
    // See Solution1Test && Solution2Test
}