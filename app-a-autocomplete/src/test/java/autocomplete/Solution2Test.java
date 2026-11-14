package autocomplete;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Runs all the tests of DictionaryTest on Solution2. You may add tests specific to Solution2 here. */
class Solution2Test extends DictionaryTest {

    @Override
    protected Dictionary createDictionary() {
        return new Solution2RBTree();
    }

    // Red-Black specific structural testing
    // Adds and deletes nodes from a test tree while calling assertValidRB after every change
    @Test
    void redBlackStructureRemainsValidAfterInsertionsAndRemovals() {
        Solution2RBTree tree = new Solution2RBTree();
        String[] words = {"c", "b", "a", "e", "f", "d", "g"};

        for (String word : words) {
            tree.add(word, 1);
            tree.assertValidRB();
        }

        for (String word : new String[] {"a", "f", "c", "g"}) {
            assertTrue(tree.remove(word));
            tree.assertValidRB();
        }
    }
}
