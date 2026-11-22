package autocomplete;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Solution 2 for the dictionary service.
 *
 * <p>TODO: Replace this comment with a short description of your solution:
 * <ul>
 *   <li>the data structure(s) used (e.g. "a hash table with separate chaining");</li>
 *   <li>where the code comes from: written by your team, adapted from course code,
 *       or a library class (e.g. java.util.TreeMap). Cite the source;</li>
 *   <li>the expected time complexity of each method below.</li>
 * </ul>
 * The full explanation of the implementation and of its expected performance
 * goes in your report.
 *
 * <p>You may rename this class to something more descriptive (e.g. TreeDictionary).
 * If you do, also rename it in Solution2Test.
 */

/**
 * Solution 2 for the dictionary service.
 * A Red-Black tree, lexicographically ordered
 * 
 * The code is a combination of written by our team, adapted from course code, namely Assignment 4,
 * and several Java library classes
 * 
 * Time Complexity of primary methods:
 * size() O(1)
 * add() O(log n)
 * remove() O(log n)
 * contains() O(log n)
 * frequency() O(log n)
 * complete() Generally O(log n + m log m + k log m), O(n log n) if all words match
 * Where 
 * n = size of the tree
 * m = matches found in complete()
 * k = number of matches to be returned in complete()
 *
 */
public class Solution2RBTree implements Dictionary {

    private static final class Node implements Comparable<Node> {
        private String word;
        private long frequency;
        private Node left;
        private Node right;
        private Node parent;
        private boolean isRed;

        private Node(String word, long frequency, Node parent) {
            this.word = word;
            this.frequency = frequency;
            this.parent = parent;
            this.isRed = true; // A newly inserted node starts red
        }

        // compareTo lib call override for node comparison for heap sorting
        @Override
        public int compareTo(Node other) {
            int frequencyOrder = Long.compare(other.frequency, frequency);
            if (frequencyOrder != 0) {
                return frequencyOrder;
            }
            return word.compareTo(other.word);
        }
    }

    private Node root;
    private int size;
    private boolean instrumentationEnabled;
    private long rotationCount;

    // fields
    public Solution2RBTree() {
        root = null;
        size = 0;
        instrumentationEnabled = false;
        rotationCount = 0;
    }

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
        return heightOf(root);
    }

    private int heightOf(Node node) {
        if (node == null) return 0;
        return 1 + Math.max(heightOf(node.left), heightOf(node.right));
    }

     // Find where the node belongs, or update its frequency if it is already present.
    @Override
    public void add(String word, long count) {
        Node parent = null;
        Node current = root;
        int comparison = 0;

        // Search, update frequency if found
        while (current != null) {
            parent = current;
            comparison = word.compareTo(current.word);

            if (comparison == 0) {
                current.frequency = count;
                return;
            } else if (comparison < 0) {
                current = current.left;
            } else {
                current = current.right;
            }
        }

        // The word was not found, so add a new red node at the proper BST position.
        Node inserted = new Node(word, count, parent);

        if (parent == null) {
            root = inserted;
        } else if (comparison < 0) {
            parent.left = inserted;
        } else {
            parent.right = inserted;
        }

        size++;

        // Restore red-black properties, then ensure the root is black.
        insertFixup(inserted);
        root.isRed = false;
    }

    // Adapted from course code
    // Finds node to delete, returns true on success, false otherwise
    @Override
    public boolean remove(String word) {
        Node target = findNode(word);
        if (target == null) {
            return false;
        }

        // If target has two children, copy its in-order successor's data into it.
        // The successor is then the node physically removed, and has at most one child.
        if (target.left != null && target.right != null) {
            Node successor = minimum(target.right);
            target.word = successor.word;
            target.frequency = successor.frequency;
            target = successor;
        }

        Node replacement = target.left != null ? target.left : target.right;
        Node replacementParent = target.parent;
        boolean removedWasRed = target.isRed;

        transplant(target, replacement);
        size--;

        // Removing a black node can reduce the black-height of one path. Restore
        // the red-black properties, passing the parent separately when replacement
        // is null because null leaves do not store parent pointers.
        if (!removedWasRed) {
            deleteFixup(replacement, replacementParent);
        }

        return true;
    }

    @Override
    public boolean contains(String word) {
        return findNode(word) != null;
    }

    // Returns frequency of node, 0 if not found
    // Note that frequency() also returns 0 if frequency of node is 0
    @Override
    public long frequency(String word) {
        Node node = findNode(word);
        if (node == null) {
            return 0;
        } else {
            return node.frequency;
        }
    }

    // Given prefix and k, return k highest frequency words beginning with given prefix as a list
    // Return empty list if k = 0, all matching words if k < matches, pull from entire tree if prefix = ""
    //     
    @Override
    public List<String> complete(String prefix, int k) {
        List<String> result = new ArrayList<>();
        if (k <= 0) {
            return result;
        }

        // Declare priority queue for sorting by frequency
        PriorityQueue<Node> matches = new PriorityQueue<>();

        // Prefix matches occupy one contiguous range in the ordered tree. 
        // Start at the first match not less than the prefix rather
        // than scanning unrelated words before that range using findlowerbound()
        // Add all to priority queue
        Node current = findLowerBound(prefix);
        while (current != null && current.word.startsWith(prefix)) {
            matches.add(current);
            current = successor(current);
        }

        // The queue is ordered by the frequency because node was made to be comparable
        // so removing its head repeatedly returns up to k words in the correct order.
        // Add to list and return
        int resultsToReturn = Math.min(k, matches.size());
        for (int i = 0; i < resultsToReturn; i++) {
            result.add(matches.remove().word);
        }

        return result;
    }

    // Size updated with add and remove function calls
    @Override
    public int size() {
        return size;
    }

    // ------------------------------------------------------------------
    // Helper methods
    // ------------------------------------------------------------------

    /**
     * Restores the red-black properties after inserting a red node.
     * A null child is treated as a black external node.
     */
    private void insertFixup(Node inserted) {
        Node z = inserted;

        while (z.parent != null && z.parent.isRed) {
            Node parent = z.parent;
            Node grandparent = parent.parent;

            if (parent == grandparent.left) {
                Node uncle = grandparent.right;

                // Case 1: a red uncle; recolor and continue from the grandparent.
                if (isRed(uncle)) {
                    parent.isRed = false;
                    uncle.isRed = false;
                    grandparent.isRed = true;
                    z = grandparent;
                } else {
                    // Case 2: turn the triangle into a straight-line case.
                    if (z == parent.right) {
                        z = parent;
                        rotateLeft(z);
                        parent = z.parent;
                        grandparent = parent.parent;
                    }

                    // Case 3: recolor and rotate to repair the red-red violation.
                    parent.isRed = false;
                    grandparent.isRed = true;
                    rotateRight(grandparent);
                }
            } else {
                Node uncle = grandparent.left;

                // Mirror of Case 1.
                if (isRed(uncle)) {
                    parent.isRed = false;
                    uncle.isRed = false;
                    grandparent.isRed = true;
                    z = grandparent;
                } else {
                    // Mirror of Case 2.
                    if (z == parent.left) {
                        z = parent;
                        rotateRight(z);
                        parent = z.parent;
                        grandparent = parent.parent;
                    }

                    // Mirror of Case 3.
                    parent.isRed = false;
                    grandparent.isRed = true;
                    rotateLeft(grandparent);
                }
            }
        }

        // The root must always be black, including after recoloring cases.
        root.isRed = false;
    }

    /** Returns true only for non-null red nodes; null nodes count as black. */
    private boolean isRed(Node node) {
        return node != null && node.isRed;
    }

    /** Returns the leftmost node in a non-empty subtree. */
    private Node minimum(Node node) {
        while (node.left != null) {
            node = node.left;
        }
        return node;
    }

    /** Replaces one subtree with another and repairs the replacement's parent link. */
    private void transplant(Node oldNode, Node newNode) {
        if (oldNode.parent == null) {
            root = newNode;
        } else if (oldNode == oldNode.parent.left) {
            oldNode.parent.left = newNode;
        } else {
            oldNode.parent.right = newNode;
        }

        if (newNode != null) {
            newNode.parent = oldNode.parent;
        }
    }

    /** Sets a node's color; a null external node is always treated as black. */
    private void setRed(Node node, boolean red) {
        if (node != null) {
            node.isRed = red;
        }
    }

    /** Rotates the subtree rooted at oldRoot to the left and returns its new root. */
    protected Node rotateLeft(Node oldRoot) {
        if (instrumentationEnabled) rotationCount++;
        Node newRoot = oldRoot.right;
        Node middle = newRoot.left;

        transplant(oldRoot, newRoot);

        newRoot.left = oldRoot;
        oldRoot.parent = newRoot;

        oldRoot.right = middle;
        if (middle != null) {
            middle.parent = oldRoot;
        }
        return newRoot;
    }

    /** Rotates the subtree rooted at oldRoot to the right and returns its new root. */
    protected Node rotateRight(Node oldRoot) {
        if (instrumentationEnabled) rotationCount++;
        Node newRoot = oldRoot.left;
        Node middle = newRoot.right;

        transplant(oldRoot, newRoot);

        newRoot.right = oldRoot;
        oldRoot.parent = newRoot;

        oldRoot.left = middle;
        if (middle != null) {
            middle.parent = oldRoot;
        }
        return newRoot;
    }

    /** Finds the node whose word equals the argument, or returns null if absent. */
    private Node findNode(String word) {
        Node current = root;

        while (current != null) {
            int comparison = word.compareTo(current.word);
            if (comparison == 0) {
                return current;
            } else if (comparison < 0) {
                current = current.left;
            } else {
                current = current.right;
            }
        }

        return null;
    }

    /** Finds the smallest node whose word is lexicographically at least key. */
    private Node findLowerBound(String key) {
        Node current = root;
        Node candidate = null;

        while (current != null) {
            if (current.word.compareTo(key) >= 0) {
                candidate = current;
                current = current.left;
            } else {
                current = current.right;
            }
        }

        return candidate;
    }

    /** Finds the next node in lexicographic order, or null if this is the maximum. */
    private Node successor(Node node) {
        if (node.right != null) {
            return minimum(node.right);
        }

        Node parent = node.parent;
        while (parent != null && node == parent.right) {
            node = parent;
            parent = parent.parent;
        }
        return parent;
    }

    /**
     * Repairs a possible extra black at node, which may be null. In the null case,
     * parent identifies the position of that external leaf in the tree.
     */
    private void deleteFixup(Node node, Node parent) {
        while (node != root && !isRed(node)) {
            if (parent == null) {
                break;
            }

            if (node == parent.left) {
                Node sibling = parent.right;

                // Case 1: a red sibling becomes a black sibling after rotation.
                if (isRed(sibling)) {
                    setRed(sibling, false);
                    setRed(parent, true);
                    rotateLeft(parent);
                    sibling = parent.right;
                }

                Node siblingLeft = sibling == null ? null : sibling.left;
                Node siblingRight = sibling == null ? null : sibling.right;

                // Case 2: both sibling children are black; move the extra black up.
                if (!isRed(siblingLeft) && !isRed(siblingRight)) {
                    setRed(sibling, true);
                    node = parent;
                    parent = node.parent;
                } else {
                    // Case 3: convert to Case 4 so the sibling's far child is red.
                    if (!isRed(siblingRight)) {
                        setRed(siblingLeft, false);
                        setRed(sibling, true);
                        if (sibling != null) {
                            rotateRight(sibling);
                        }
                        sibling = parent.right;
                    }

                    // Case 4: rotate the parent to remove the extra black.
                    setRed(sibling, parent.isRed);
                    setRed(parent, false);
                    setRed(sibling == null ? null : sibling.right, false);
                    rotateLeft(parent);
                    node = root;
                    parent = null;
                }
            } else {
                // Mirror the cases above for a right-side extra black.
                Node sibling = parent.left;

                if (isRed(sibling)) {
                    setRed(sibling, false);
                    setRed(parent, true);
                    rotateRight(parent);
                    sibling = parent.left;
                }

                Node siblingLeft = sibling == null ? null : sibling.left;
                Node siblingRight = sibling == null ? null : sibling.right;

                if (!isRed(siblingLeft) && !isRed(siblingRight)) {
                    setRed(sibling, true);
                    node = parent;
                    parent = node.parent;
                } else {
                    if (!isRed(siblingLeft)) {
                        setRed(siblingRight, false);
                        setRed(sibling, true);
                        if (sibling != null) {
                            rotateLeft(sibling);
                        }
                        sibling = parent.left;
                    }

                    setRed(sibling, parent.isRed);
                    setRed(parent, false);
                    setRed(sibling == null ? null : sibling.left, false);
                    rotateRight(parent);
                    node = root;
                    parent = null;
                }
            }
        }

        setRed(node, false);
    }

    // TESTING HELPERS //

    // Called by Solution2Test.java //
    // Calls validateNode() starting at the root to check the whole tree, and checks size.
    void assertValidRB() {
        if (root != null && root.parent != null) {
            throw new AssertionError("the root must not have a parent");
        }
        if (isRed(root)) {
            throw new AssertionError("the root must be black");
        }

        // Uses an identity hash map to compare by object identity
        // Wrapped inside of a set for simplicity/applicability of set methods
        Set<Node> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        validateNode(root, null, null, null, visited);

        if (visited.size() != size) {
            throw new AssertionError(
                    "stored size is " + size + ", but tree contains " + visited.size() + " nodes");
        }
    }

    // Checks ordering, parent links, and red-black properties recursively.
    // Throws a descriptive assertion error if any check fails.
    // Called by assertValidRB.
    private int validateNode(
            Node node, String lowerBound, String upperBound, Node expectedParent, Set<Node> visited) {
        // A null child is a black external leaf, so its black-height is 1.
        if (node == null) return 1;

        // If visited.add node returns false, the node was reached before
        // Each node is checked once, so this should not occur
        if (!visited.add(node)) {
            throw new AssertionError("tree contains a cycle or a node with multiple parents");
        }
        if (node.parent != expectedParent) {
            throw new AssertionError("incorrect parent link at " + node.word);
        }
        if (lowerBound != null && node.word.compareTo(lowerBound) <= 0) {
            throw new AssertionError("node " + node.word + " is not greater than its lower bound");
        }
        if (upperBound != null && node.word.compareTo(upperBound) >= 0) {
            throw new AssertionError("node " + node.word + " is not less than its upper bound");
        }

        if (node.isRed && (isRed(node.left) || isRed(node.right))) {
            throw new AssertionError("red node " + node.word + " has a red child");
        }

        int leftBlackHeight =
                validateNode(node.left, lowerBound, node.word, node, visited);
        int rightBlackHeight =
                validateNode(node.right, node.word, upperBound, node, visited);

        if (leftBlackHeight != rightBlackHeight) {
            throw new AssertionError("unequal black-heights below " + node.word);
        }

        return leftBlackHeight + (node.isRed ? 0 : 1);
    }

}
