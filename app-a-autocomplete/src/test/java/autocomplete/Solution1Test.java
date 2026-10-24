package autocomplete;

/** Runs all the tests of DictionaryTest on Solution1. You may add tests specific to Solution1 here. */
class Solution1Test extends DictionaryTest {

    @Override
    protected Dictionary createDictionary() {
        return new Solution1();
    }
}
