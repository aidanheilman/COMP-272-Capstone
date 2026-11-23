SOLUTION1 TEST SUITE IS HANGING INDEFITELY

Bug in Solution1.java frequency() method -- I believe it fails on randomOperationsGiveTheSameResultsAsTreeMap() in DictionaryTest.java where 
long count = random.nextInt(1_000) can create an integer value of 0.
When I ran mvn test for Solution1, it would hang indefinitely.

When the frequency == 0, loop compares indefinitely, setting f equal to c.c (current node's count, which is 0), and continuing because the conditional statement "f == 0" is still true, without moving nodes.

    @Override
    public long frequency(String w) {
        long f = 0; Node c = root;

        // Searching the tree while the search word is valid, 
        // we haven't hit a dead end, and the frequency isn't found.
        while (w != null && c != null && f == 0) {
            int cmp = w.compareTo(c.w); 

            // If the word matches the current node, retrieve its count. 
            // Otherwise, branch left or right based on alphabetical order.
            if (cmp == 0) f = c.c; else c = cmp < 0 ? c.l : c.r;
        }
        // Returning the found count (or 0 if the word was not inside the tree).
        return f;
    }

The fix: Use two return statements, and do not use the frequency being 0 as a sentinel value.
Also changed Node c to Node current for readability.

    @Override
    public long frequency(String w) {
        Node current = root;

        // Searching the tree while the search word is valid, 
        // we haven't hit a dead end, and the frequency isn't found.
        while (w != null && current != null) {
            int cmp = w.compareTo(current.w); 

            // If the word matches the current node, return its count. 
            // Otherwise, branch left or right based on alphabetical order.
            if (cmp == 0) {return current.c;} 
            current = cmp < 0 ? current.l : current.r;
        }
        // Return 0 if the word was not inside the tree.
        return 0;
    }

Am now realizing that the dictionary contract for frequency also states that 0 will be returned upon the frequency not being found. No way to distinguish here, unless return value for not found is changed to -1, for example, or if the testing conditions are changed to not use 0 values for frequency, given that that is a plausible edge case to rule out. Either way, both go against the assignment directions to not change the pre-existing test suite or the method parameters, so it will be left ambiguous for now. Frequency() returns 0 both when a frequency == 0 and when the word is not found in the tree. 

Note that there will be a related bug in Solution1.java contains() method, which calls frequency
NOT FIXED 


Additional bug in Solution1.java remove() method, in deletion in a node with two children

The way that this code is written makes it difficult to read and it is laid out differently than the course material, so I used Github Copilot and it pointed out that "When deleting a node with two children, rep is set to c because the target node stays in the tree and receives its successor’s word and frequency. The old code then reconnects the target as one of its own children, which can create a cycle. Skipping that reconnection when rep == c avoids the cycle; the successor is already detached by the preceding code." and provided this fix. This does not occur in the RB tree because it uses the dedicated transplant helper method as implemented in Assignment 4.

// Attaching the replacement node to the parent node's pointer.
                if (path.isEmpty()) root = rep; else { Node parent = path.peek(); if (parent.l == c) parent.l = rep; else parent.r = rep; }


Changed to 
// Attaching the replacement node to the parent node's pointer.
                if (path.isEmpty()) {
                    root = rep;
                } else if (rep != c) {
                    Node parent = path.peek();
                    if (parent.l == c) parent.l = rep;
                    else parent.r = rep;
                }
                
With this fix, the tests all ran (finally), but one failed on Solution1.java in the Random Operations

[ERROR] autocomplete.Solution1Test.randomOperationsGiveTheSameResultsAsTreeMap -- Time elapsed: 0.003 s <<< FAILURE!
org.opentest4j.AssertionFailedError: remove w85 ==> expected: <true> but was: <false>
        at autocomplete.DictionaryTest.randomOperationsGiveTheSameResultsAsTreeMap(DictionaryTest.java:205)
        at java.base/java.lang.reflect.Method.invoke(Method.java:583)
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1612)
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1612)

I think this may be due to the aforementioned bug regarding contains() calling the frequency method.
I'm going to rewrite contains to have it's own traversal logic, that way it will not give innacurate results on its call in remove(). 

Contains currently looks like this:
    @Override
    public boolean contains(String w) {
        // Checking if the word exists by verifying if its recorded frequency is greater than 0.
        return frequency(w) > 0;
    }

New contains, with independent traversal
    @Override
    public boolean contains(String w) {
        Node current = root;

        // Searching the tree while the search word is valid, 
        // we haven't hit a dead end, and the frequency isn't found.
        while (w != null && current != null) {
            int cmp = w.compareTo(current.w); 

            // If the word matches the current node, return its count. 
            // Otherwise, branch left or right based on alphabetical order.
            if (cmp == 0) {return true;} 
            current = cmp < 0 ? current.l : current.r;
        }
        // Return 0 if the word was not inside the tree.
        return false;
    }


11/21/26
Added incrementation counters & tests to both solutions, toggled and returned using methods
    
    public void setInstrumentationEnabled(boolean enabled) {
        instrumentationEnabled = enabled;
    }

    public void resetInstrumentationCounters() {
        rotationCount = 0;
    }

    public long getRotationCount() {
        return rotationCount;
    }

    public int getTreeHeight() {
        return root == null ? 0 : root.h;
    }

Basically, if you want to record rotations, use tree.setInstrumentationEnabled(true),
run the workload, and then return getRotationCount() to a variable. 
getTreeHeight() works regardless of toggle, but costs O(n) for RB tree.
Toggle on, return rotations and measurement on same workload AFTER timed testing.