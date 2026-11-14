package autocomplete;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Runs all the tests of DictionaryTest on Solution1. You may add tests specific to Solution1 here. */
class Solution1Test extends DictionaryTest {

    @Override
    protected Dictionary createDictionary() {
        return new Solution1();
    }

    // AVL specific structural testing
    // Adds and deletes nodes from a test tree while calling assertValidAVL after every change
    @Test
    void avlStructureRemainsValidAfterInsertionsAndRemovals() {
        Solution1 tree = new Solution1();
        String[] words = {"c", "b", "a", "e", "f", "d", "g"};

        for (String word : words) {
            tree.add(word, 1);
            tree.assertValidAVL();
        }

        for (String word : new String[] {"a", "f", "c", "g"}) {
            assertTrue(tree.remove(word));
            tree.assertValidAVL();
        }
    }
}
