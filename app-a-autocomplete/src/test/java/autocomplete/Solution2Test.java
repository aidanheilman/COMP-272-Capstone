package autocomplete;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void redBlackInstrumentationCanBeToggledAndReset() {
        Solution2RBTree tree = new Solution2RBTree();
        tree.setInstrumentationEnabled(true);
        tree.add("c", 1);
        tree.add("b", 1);
        tree.add("a", 1);

        assertEquals(1, tree.getRotationCount());
        assertEquals(2, tree.getTreeHeight());

        tree.setInstrumentationEnabled(false);
        tree.add("e", 1);
        tree.add("d", 1);
        assertEquals(1, tree.getRotationCount());

        tree.resetInstrumentationCounters();
        assertEquals(0, tree.getRotationCount());
    }
}
