package autocomplete;

import java.util.List;

/**
 * Solution 1 for the dictionary service.
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
 * <p>You may rename this class to something more descriptive (e.g. HashDictionary).
 * If you do, also rename it in Solution1Test.
 */
public class Solution1 implements Dictionary {
    private class Node {
        String w; long c; int h = 1; Node l, r;
        Node(String word, long count) { w = word; c = count; }
    }
    private Node root; private int size;

    // TODO: declare the fields of your data structure here.

    public Solution1() {
        /*
         * DESCRIPTION: Custom AVL tree holding alphabetically sorted words (A-Z) and frequencies.
         * SOURCE: Code written entirely from scratch by our team.
         * COMPLEXITIES: size(): O(1), contains(): O(log N), frequency(): O(log N),
         * add(): O(log N), remove(): O(log N), complete(): O(log N + M + M log M)
         */
        this.root = null;
        this.size = 0;
    }

    @Override
    public void add(String w, long c) {
        // Check if the input word is valid.
        if (w != null) {
            // Initialize a stack to remember the path taken from the root.
            java.util.Stack<Node> path = new java.util.Stack<>(); Node curr = root, p = null; boolean up = false;
            // Searching the tree to see if the word already exists.
            while (curr != null && !up) {
                int cmp = w.compareTo(curr.w);
                // If the word is found, update its counter and mark as updated.
                // Else, push current node to path and move down the tree.
                if (cmp == 0) { curr.c = c; up = true; } else { path.push(p = curr); curr = cmp < 0 ? curr.l : curr.r; }
            }
            // If the word is new, insert it into the tree.
            if (!up) {
                Node ch = new Node(w, c); size++;
                // Attach the new node to its appropriate parent pointer.
                if (p == null) root = ch; else if (w.compareTo(p.w) < 0) p.l = ch; else p.r = ch;
                // Backtrack up the path to update node heights and fix imbalances.
                while (!path.isEmpty()) {
                    Node n = path.pop(), b = n;
                    int hl = n.l == null ? 0 : n.l.h, hr = n.r == null ? 0 : n.r.h;
                    // Update current node's height.
                    n.h = 1 + Math.max(hl, hr);

                    // This would be Case 3a where the subtree has become left-heavy.
                    if (hl - hr > 1) {
                        // If it's a Left-Right case, perform an initial left rotation on the child.
                        Node l = n.l; if ((l.l == null ? 0 : l.l.h) < (l.r == null ? 0 : l.r.h)) {
                            Node y = l.r; l.r = y.l; y.l = l; n.l = y;
                            l.h = 1 + Math.max(l.l == null ? 0 : l.l.h, l.r == null ? 0 : l.r.h);
                            y.h = 1 + Math.max(y.l == null ? 0 : y.l.h, y.r == null ? 0 : y.r.h);
                        }
                        // Perform the main right rotation on node 'n'.
                        Node x = n.l; n.l = x.r; x.r = n; b = x;
                        n.h = 1 + Math.max(n.l == null ? 0 : n.l.h, n.r == null ? 0 : n.r.h);
                        x.h = 1 + Math.max(x.l == null ? 0 : x.l.h, x.r == null ? 0 : x.r.h);
                    // Case 3b: The subtree has become right-heavy.
                    } else if (hr - hl > 1) {

                         // If it's a Right-Left case, perform an initial right rotation on the child.
                        Node r = n.r; if ((r.r == null ? 0 : r.r.h) < (r.l == null ? 0 : r.l.h)) {
                            Node x = r.l; r.l = x.r; x.r = r; n.r = x;
                            r.h = 1 + Math.max(r.l == null ? 0 : r.l.h, r.r == null ? 0 : r.r.h);
                            x.h = 1 + Math.max(x.l == null ? 0 : x.l.h, x.r == null ? 0 : x.r.h);
                        }
                        // Perform the main left rotation on node 'n'.
                        Node y = n.r; n.r = y.l; y.l = n; b = y;
                        n.h = 1 + Math.max(n.l == null ? 0 : n.l.h, n.r == null ? 0 : n.r.h);
                        y.h = 1 + Math.max(y.l == null ? 0 : y.l.h, y.r == null ? 0 : y.r.h);
                    }
                    // Reconnect the newly balanced subtree back into the tree structure.
                    if (path.isEmpty()) root = b; else if (path.peek().l == n) path.peek().l = b; else path.peek().r = b;
                }
            }
        }
    }

    @Override
    public boolean remove(String w) {
        boolean rem = false;

        // Check if the input is valid and the word actually exists in the tree.
        if (w != null && contains(w)) {
            // Initializing a stack to keep track of the traversal path for rebalancing.
            java.util.Stack<Node> path = new java.util.Stack<>(); Node c = root;

            // Finding the target node 'c' to remove and record the path.
            while (c != null && !c.w.equals(w)) { path.push(c); c = w.compareTo(c.w) < 0 ? c.l : c.r; }
            if (c != null) {
                size--; rem = true; Node rep;

                // If node has 0 or 1 children, replacement is the non-null child (or null).
                if (c.l == null || c.r == null) rep = c.l != null ? c.l : c.r;
                else {
                    // If 2 children, find the in-order successor (leftmost node of right subtree).
                    path.push(c); Node m = c.r; while (m.l != null) { path.push(m); m = m.l; }

                    // Overwrite target data with successor data and bypass the old successor node.
                    c.w = m.w; c.c = m.c; if (path.peek() == c) c.r = m.r; else path.peek().l = m.r; rep = c;
                }
                // Attaching the replacement node to the parent node's pointer.
                if (path.isEmpty()) root = rep; else { Node parent = path.peek(); if (parent.l == c) parent.l = rep; else parent.r = rep; }

                // Unwinding the stack to fix heights and rotate any unbalanced AVL ancestors.
                while (!path.isEmpty()) {
                    Node n = path.pop(), b = n;
                    int hl = n.l == null ? 0 : n.l.h, hr = n.r == null ? 0 : n.r.h; n.h = 1 + Math.max(hl, hr);

                    // Rebalancing Left-Heavy subtrees (Left-Right and Left-Left cases).
                    if (hl - hr > 1) {
                        Node l = n.l; if ((l.l == null ? 0 : l.l.h) < (l.r == null ? 0 : l.r.h)) {
                            Node y = l.r; l.r = y.l; y.l = l; n.l = y;
                            l.h = 1 + Math.max(l.l == null ? 0 : l.l.h, l.r == null ? 0 : l.r.h);
                            y.h = 1 + Math.max(y.l == null ? 0 : y.l.h, y.r == null ? 0 : y.r.h);
                        }
                        Node x = n.l; n.l = x.r; x.r = n; b = x;
                        n.h = 1 + Math.max(n.l == null ? 0 : n.l.h, n.r == null ? 0 : n.r.h);
                        x.h = 1 + Math.max(x.l == null ? 0 : x.l.h, x.r == null ? 0 : x.r.h);

                    // Rebalancing Right-Heavy subtrees (Right-Left and Right-Right cases).
                    } else if (hr - hl > 1) {
                        Node r = n.r; if ((r.r == null ? 0 : r.r.h) < (r.l == null ? 0 : r.l.h)) {
                            Node x = r.l; r.l = x.r; x.r = r; n.r = x;
                            r.h = 1 + Math.max(r.l == null ? 0 : r.l.h, r.r == null ? 0 : r.r.h);
                            x.h = 1 + Math.max(x.l == null ? 0 : x.l.h, x.r == null ? 0 : x.r.h);
                        }
                        Node y = n.r; n.r = y.l; y.l = n; b = y;
                        n.h = 1 + Math.max(n.l == null ? 0 : n.l.h, n.r == null ? 0 : n.r.h);
                        y.h = 1 + Math.max(y.l == null ? 0 : y.l.h, y.r == null ? 0 : y.r.h);
                    }
                    // Connecting the newly balanced parent or subtree root back to its ancestor.
                    if (path.isEmpty()) root = b; else if (path.peek().l == n) path.peek().l = b; else path.peek().r = b;
                }
            }
        }
        return rem;
    }

    @Override
    public boolean contains(String w) {
        // Checking if the word exists by verifying if its recorded frequency is greater than 0.
        return frequency(w) > 0;
    }

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

    @Override
    public List<String> complete(String p, int k) {
        java.util.List<Node> m = new java.util.ArrayList<>();
        // Check if the prefix and the tree are both valid.
        if (p != null && root != null) {
            java.util.Stack<Node> s = new java.util.Stack<>(); Node c = root;
            // Performing an optimized in-order traversal to gather all matching words.
            while (c != null || !s.isEmpty()) {
                // Push left children. 
                // Go left if the node matches the prefix or if the prefix belongs in the left subtree.
                while (c != null) { s.push(c); c = (c.w.startsWith(p) || p.compareTo(c.w) < 0) ? c.l : null; }
                // Processing the current node. If it begins with the prefix, collect it.
                c = s.pop(); if (c.w.startsWith(p)) m.add(c);
                // Exploring the right child. 
                // Go right if the node matches the prefix or if the prefix belongs in the right subtree.
                c = (c.w.startsWith(p) || p.compareTo(c.w) > 0) ? c.r : null;
            }
        }
        // Sort results primarily by descending frequency, and secondarily by ascending alphabetical order.
        m.sort((a, b) -> a.c != b.c ? Long.compare(b.c, a.c) : a.w.compareTo(b.w));

        // Extract the words from the top 'k' highest-ranked nodes.
        java.util.List<String> res = new java.util.ArrayList<>();
        for (int i = 0; i < Math.min(k, m.size()); i++) res.add(m.get(i).w);

        // Returning the result.
        return res;
    }

    @Override
    public int size() {
        // Returning size.
        return size;
    }
    
    // Test method.
    public static void main(String[] args) {
        Solution1 d = new Solution1();
        String[] w = {"apple", "banana", "cherry", "apricot"};
        long[] c = {10, 20, 30, 100};
        for (int i = 0; i < 4; i++) d.add(w[i], c[i]);
        System.out.println("Size (4): " + d.size());
        System.out.println("Freq banana (20): " + d.frequency("banana"));
        System.out.println("Contains grape (false): " + d.contains("grape"));
        System.out.println("Autocomplete 'ap' [apricot, apple]: " + d.complete("ap", 2));
        d.remove("banana");
        System.out.println("Size after delete (3): " + d.size());
        System.out.println("Contains banana (false): " + d.contains("banana"));
    }
}
