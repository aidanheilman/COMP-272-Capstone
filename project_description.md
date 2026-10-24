# COMP 272/400C — Term Project

## Data Structures in Practice: Design, Implement, Evaluate

| | |
|---|---|
| **Duration** | Week 9 to Week 15 |
| **Teams** | 2–3 students |
| **Language** | Java 17 or later |
| **Starter code** | Provided for each proposed application: interface, two solution skeletons, dataset reader skeleton, JUnit tests |
| **Final deliverables** | Code (with tests and benchmarks), a written engineering report (Week 14), a recorded presentation with a live demo (Week 15) |

---

## 1. What This Project Is About

So far, you have studied data structures one at a time. You implemented them, traced them by hand, and analyzed their complexity. In practice, engineers work the other way around:

1. They start from a **problem**: a system that users need.
2. They write down precisely what the system must do, and **how well** it must do it.
3. They consider several **candidate designs**.
4. They build them, **test** that they are correct, and **measure** them on realistic data.
5. Only then do they **choose** one, and justify the choice.

In this project, you will follow exactly this process. You will build a small system around a real, large dataset, implement it with **two different data structure solutions**, compare them experimentally, and recommend one.

There is no single correct answer. A well-argued recommendation backed by careful measurements earns full marks, even if it differs from what theory predicts. Explaining **why** it differs is part of the work.

### 1.1 The five stages at a glance

| Stage | What you do | Outcome | Due |
|---|---|---|---|
| 1. Application, team, requirements | Choose an application and a team; specify functional and non-functional requirements | Part of the **proposal** | Week 10 |
| 2. Data structure choice and design | Choose two solutions; design them; state hypotheses | Part of the **proposal** | Week 10 |
| 3. Implementation and correctness testing | Read the dataset; implement both solutions behind the common interface; extend the tests | **Code** that passes its tests | Week 12 |
| 4. Experimental evaluation | Build a benchmark harness; run experiments; collect results | **Benchmark code, raw results (CSV), plots** | Week 13 |
| 5. Trade-off analysis and recommendation | Analyze the results; recommend a design | **Report** (Week 14) and **recorded presentation** with a demo (Week 15) | Weeks 14–15 |

Throughout this handout, boxes marked **Concept** briefly introduce software engineering ideas that are not part of COMP 272. Each box gives a reference if you want to read more.

---

## 2. The Applications

Three applications are proposed, each described in its own **application sheet**:

| Sheet | Application |
|---|---|
| Application A | Autocomplete and spell-checking service |
| Application B | Movie catalogue search backend |
| Application C | Web page cache for a proxy server |

Each sheet gives the scenario, a free dataset, the functional and non-functional requirements, the workloads to test, suggested data structure pairs, and questions your analysis should answer.

Each proposed application also comes with a **starter code** project (Section 5). All three datasets are plain text or CSV files.

### 2.1 Proposing your own application

You may propose a different application instead. It must be approved with your Week 10 proposal. Your proposal must show that the application and its dataset meet these criteria:

| Criterion | Requirement |
|---|---|
| **Free and legal** | The dataset is freely available for educational use. You name its license or terms of use and cite it. |
| **Accessible** | Anyone can download it from a public link (which goes in your README). No paid accounts, no scraping. |
| **Large** | At least about **1 million records**, so that differences between data structures become visible. |
| **Real** | Real-world data, not randomly generated. (You may *add* generated workloads on top of it.) |
| **Text files** | Plain text or CSV/TSV files (possibly compressed, e.g. `.gz` or `.zip`) that you can read line by line in Java. **No relational databases, SQL dumps, or web APIs.** |
| **Rich enough** | The application needs **at least 3 different operations**, and at least one of them should favour one design over the other (e.g. exact lookup vs. ordered range query). |

There is no starter code for your own application: you write the interface, the two solution classes, and the tests yourselves, following the same structure as the starter code of the proposed applications.

Good places to look: [Kaggle Datasets](https://www.kaggle.com/datasets), [data.gov](https://data.gov/), [Stanford SNAP](https://snap.stanford.edu/data/), [UCI Machine Learning Repository](https://archive.ics.uci.edu/), and the open data portal of your city.

---

## 3. Stage 1 — Application, Team, and Requirements Specification

### What you do

1. Form your team and choose your application.
2. Download the dataset and check that you can parse it.
3. Write the **requirements specification** of your system: its functional and non-functional requirements.

> **Concept — Requirements specification**
>
> A requirements specification states what a system must do, before anyone decides how to build it. Requirements come in two kinds:
>
> - **Functional requirements** say **what** the system does: its operations, their inputs, and their outputs. Example: *"`lookup(name)` returns the phone number of the contact with this name, or `null` if there is none."*
> - **Non-functional requirements** say **how well** it does it: speed, memory, scalability, start-up time, and so on. Example: *"`lookup` takes less than 1 µs on average with 1 million contacts."*
>
> Two different data structures can satisfy the **same** functional requirements. The non-functional requirements are what tell them apart, which is why they are central to this project.
>
> *Reference:* I. Sommerville, *Software Engineering*, 10th ed., Pearson, 2015 — Chapter 4, "Requirements Engineering" (Section 4.1: functional and non-functional requirements).

### 3.1 Functional requirements

List every operation your system must support, with its parameters, its result, and what happens in special cases (missing key, duplicate key, empty range). These operations become the methods of your Java interface in Stage 3.

For the three proposed applications, the functional requirements are given: the **interface in the starter code** is their exact specification (each method's comment says what it must do, including the special cases). You may add operations, but you may not change the given ones.

### 3.2 Non-functional requirements

For the three proposed applications, the application sheet gives **target** non-functional requirements. You must check, in your report, whether each of your designs meets them. You may refine them or add your own.

If you propose your own application, you must write them yourself. Here is how.

#### A good non-functional requirement is measurable

A requirement you cannot measure is not a requirement. Compare:

| ✗ Not measurable | ✓ Measurable |
|---|---|
| "Search must be fast." | "`search` takes at most 1 ms on average with the full dataset of 2 million records." |
| "It must scale." | "When the dataset grows 10×, the average time of `get` grows by at most 2×." |
| "It must not use too much memory." | "The index uses at most 100 bytes per record, and the whole system runs with `-Xmx2g`." |

Each requirement should name: **the operation**, **the metric** (average time, worst time, throughput, memory, …), **the data size**, and **the target value**.

#### How to choose the target values

Work from the scenario of your application:

1. **Who waits for the answer, and how long can they wait?** For an interactive user, about **0.1 s** feels instantaneous, and about **1 s** keeps their flow of thought uninterrupted ([Nielsen, *Response Times: The 3 Important Limits*](https://www.nngroup.com/articles/response-times-3-important-limits/)). If one user action triggers many operations (e.g. one keystroke triggers a search), divide the budget among them.
2. **How many requests per second?** Estimate the load. Example: 5,000 users, each making one request every 2 seconds, is 2,500 requests/s, so one server must handle each request in well under 0.4 ms.
3. **How big is the data, and how fast does it grow?** Use the size of your dataset, and plan for growth (e.g. 10× in a few years).
4. **What hardware is available?** Assume a typical laptop: e.g. 8 GB of RAM, of which your program may use at most 2 GB.
5. **How often does the system start?** If the data is loaded at every start-up, the build time matters (e.g. "loads in less than 5 s").

#### Examples

| Application | Non-functional requirement | Derived from |
|---|---|---|
| Bike-sharing station finder | `nearestStation(lat, lon)` answers in ≤ 10 ms with 50,000 stations | Interactive map; must feel instantaneous |
| Bank transaction log | Sustains ≥ 100,000 `insert` per second | Peak load estimate |
| Library catalogue | `booksByYear(from, to)` returns in ≤ 50 ms for a 10-year range, 5 million books | Interactive search page |
| Sensor monitoring | Uses ≤ 64 bytes of overhead per reading for 10 million readings | Runs on a small server with limited memory |
| Any system loaded at start-up | Builds the index from the dataset in ≤ 10 s | Restart time after a crash |

Your targets do not have to be perfect. They must be **reasonable**, **justified** in one sentence each, and **measurable**. At the end of the project, it is perfectly acceptable to report that a design does **not** meet a target, as long as you explain why.

> **Concept — Version control with Git**
>
> As a team, you will edit the same code. I propose you use a Git repository (e.g. a **private** GitHub or GitLab repository) so that every change is recorded, you can merge each other's work, and you can go back to a version that worked. Commit small changes often, with clear messages. Your instructor may ask to see the commit history.
>
> *Reference:* S. Chacon and B. Straub, [*Pro Git*](https://git-scm.com/book/en/v2), 2nd ed. — free online. Chapters 1–3 are enough for this project.

### Outcome of Stage 1 → Proposal, Part 1

- Team members, and the application chosen (for your own application: the criteria of Section 2.1).
- Proof that you downloaded and parsed the dataset: e.g. *"We loaded 87,585 movies; 12 lines skipped because the year was missing."*
- The functional requirements.
- The non-functional requirements, with a one-sentence justification for each target you wrote or changed.

---

## 4. Stage 2 — Data Structure Choice and Design

### What you do

1. Choose **two different data structure solutions** for your application.
2. Design each one on paper.
3. Predict how they will compare: write **hypotheses**.

> **Concept — ADT vs. implementation**
>
> An **abstract data type (ADT)** describes *what* operations are available (e.g. a Map: `put`, `get`, `remove`). An **implementation** describes *how* they are carried out (e.g. a hash table with chaining, or a Red-Black tree). Your two solutions can differ at either level:
>
> - **Two different ADTs**, e.g. an unordered map (hash table) vs. an ordered map (Red-Black tree). An ordered map supports range queries; an unordered map does not, at least not efficiently.
> - **One ADT, two implementations**, e.g. a hash table with separate chaining vs. one with linear probing.
>
> A solution can also **combine** structures, e.g. a hash map plus a doubly linked list.

### 4.1 Structures covered in the course

| Family | Structures |
|---|---|
| Lists | Array-based list (dynamic array), singly and doubly linked lists, stacks, queues, deques |
| Trees | Binary search tree, AVL tree, Red-Black tree |
| Hashing | Separate chaining, open addressing (linear probing), cuckoo hashing, Bloom filters |
| Priority queues | Binary heap |
| Graphs | Adjacency matrix, adjacency list; graph traversals (breadth-first and depth-first search) |

Binary heaps and graphs are covered later in the term. You may use them before then. If you do, read ahead in the course material and explain the structure briefly in your Design section. You do **not** need the full introduction described in Section 4.2.

### 4.2 Structures not covered in the course

You **may** use a structure that we have not covered, for example:

| Structure | Typical use |
|---|---|
| Trie (prefix tree) or radix tree | Prefix search, autocomplete |
| Skip list | Ordered map; a randomized alternative to balanced trees |
| Treap or splay tree | Randomized or self-adjusting search trees |
| B-tree / B+ tree | Ordered data with many keys per node; range scans |
| Robin Hood hashing, quadratic probing, double hashing | Other open-addressing strategies |
| Count-min sketch | Approximate frequency counting |

The rules are:

1. **At least one** of your two solutions must use a structure covered in the course (Section 4.1).
2. Your report must include an **introduction to the new structure** (1.5–3 pages) with:
   - its definition and invariants (what must always be true),
   - a **diagram** of an example instance,
   - how the main operations work, with a **worked example** traced step by step,
   - the time and space complexity of each operation, with a short justification,
   - at least **one cited reference** (a textbook, a paper, or lecture notes).
3. Your presentation must include **2–3 slides** that teach the new structure to the class. Assume your audience knows only what was covered in COMP 272.
4. Using a non-covered structure is **not required** and earns no bonus on its own. It is graded like everything else: on correctness and on the clarity of your explanation.

### 4.3 Where the implementation comes from

For each solution, you may use any of the following sources:

- **code written by your team**;
- **code from class** (e.g. the hash tables, AVL tree, or Red-Black tree from the course modules and assignments);
- a **library class**: the Java Collections Framework (`HashMap`, `TreeMap`, `LinkedHashMap`, `PriorityQueue`, `ArrayList`, …) or a well-known open-source library (e.g. [Guava](https://github.com/google/guava), [Apache Commons Collections](https://commons.apache.org/proper/commons-collections/));
- **another source** (a textbook, a website, …).

Whatever the source, the conditions are the same:

1. **Cite** the source, in the class comment and in your report.
2. **Wrap** it in the solution class of the starter code (`Solution1` or `Solution2`), which implements the application's interface with all its functional requirements. For a library class, this solution class is an **adapter** (see the Concept box in Section 5).
3. **Explain the implementation in detail** in your report, in your own words:
   - the data structures it uses internally, with a diagram;
   - the main **implementation choices**: e.g. hash function, collision handling, load factor and resizing policy, balancing rule, how deletion is handled;
   - for a library class: the exact class and version (e.g. `java.util.HashMap`, JDK 21), and how it **differs** from the version studied in class. For example, `HashMap` stores an array of buckets with separate chaining; when a bucket's list becomes long, it is converted into a Red-Black tree; and the table doubles in size when the load factor exceeds 0.75. Your sources here are the official API documentation and the source code (e.g. the [OpenJDK source on GitHub](https://github.com/openjdk/jdk));
   - the **expected performance** of each operation of the interface: time complexity (average and worst case) and memory, with a short justification.

**Things to keep in mind:**

- Library classes are usually heavily optimized. When you compare a library class with code written by your team or from class, discuss whether a performance difference comes from the **data structure** itself or from **engineering** (tuning, constant factors).
- You cannot add instrumentation counters inside a library class (Section 5.3). You will have to explain its measured behaviour from its documented design.
- Code you write yourselves gives you more to measure and more to explain. A library lets you focus on design and evaluation. Both are acceptable; choose deliberately.

### 4.4 Designing each solution

For each of your two solutions, describe, **before** coding:

- the structure(s) used, with a **diagram**;
- how each operation of your functional requirements is carried out with this structure;
- the **expected** time complexity of each operation (average and worst case), and the space used;
- the main **implementation choices**: e.g. initial capacity, load factor, resizing policy, how deletion is handled, what the keys are.

> **Concept — Hypothesis**
>
> A hypothesis is a **testable prediction** made before the experiment. It turns "let's see what happens" into "we expect X, because Y". Example: *"We expect the hash table to answer `lookup` faster than the Red-Black tree for large n, because its average cost is O(1) instead of O(log n). We expect the opposite for `namesStartingWith(prefix)`, because the hash table must scan every entry."*
>
> Your experiments will **confirm or refute** each hypothesis. A refuted hypothesis is not a failure: explaining it is often the most interesting part of a report.

### Outcome of Stage 2 → Proposal, Part 2

- Your two solutions, and why you chose them (which trade-off you expect them to reveal).
- For each: where the code will come from (your team, class, a library, another source); if it is a non-covered structure, its name and your reference.
- A short design of each (structures, diagram, expected complexity of each operation).
- At least **three hypotheses**.

The **proposal** (Stages 1 and 2) is 2–4 pages and is due at the end of **Week 10**. You will receive feedback early in Week 11.

---

## 5. Stage 3 — Implementation and Correctness Testing

### The starter code

For each proposed application, you receive a starter project (open it in IntelliJ IDEA, Eclipse, or VS Code; its README explains how). It contains:

| File | Content | Your work |
|---|---|---|
| The interface (`Dictionary`, `MovieCatalogue`, or `PageCache`) | The functional requirements, as Java methods with comments | Read it carefully. Do not change it. |
| `Solution1`, `Solution2` | The two solution classes; every method throws "Not implemented yet" | Implement them |
| `DatasetReader` | Methods that read the dataset files; not written | Write them |
| A shared test class and `Solution1Test`, `Solution2Test` | JUnit tests run on both solutions, including one dataset test marked `@Disabled` | Make them pass, then **add more** |

### What you do

1. Download the dataset and write `DatasetReader`.
2. Implement `Solution1` and `Solution2` (from any source, see Section 4.3).
3. **Extend the tests**: enable the dataset test, add **tests from the dataset**, and add tests for edge cases and for the special cases of your implementations.
4. Add **instrumentation counters** to the structures written by your team or taken from class.

Correctness testing belongs in this stage, not later: an implementation is not finished until its tests pass. **Performance** testing is a separate stage (Stage 4), because measuring a structure that is not yet correct gives meaningless numbers.

### 5.1 A common interface

Both solutions implement **the same interface**, given in the starter code. Your tests, your benchmark harness, and your demo then work with the interface only, and run unchanged on either solution. This guarantees a fair comparison.

> **Concept — Programming to an interface, and the Adapter pattern**
>
> **Programming to an interface** means that the rest of your code depends only on the interface, never on a specific class. Switching solutions is then a one-line change.
>
> When you use a library class (e.g. `TreeMap`), its methods do not match your interface. An **adapter** is a small class that implements your interface and **translates** each call into calls to the library class. It contains no data structure logic of its own.
>
> *Reference:* E. Gamma, R. Helm, R. Johnson, J. Vlissides, *Design Patterns: Elements of Reusable Object-Oriented Software*, Addison-Wesley, 1994 — "Adapter". An accessible online explanation: [refactoring.guru — Adapter](https://refactoring.guru/design-patterns/adapter).

**Example.** The starter code applies this idea to your application. Here it is on a smaller example: a contact book with three operations. The interface:

```java
import java.util.List;

public interface ContactBook {
    /** Adds a contact, or replaces the number if the name is already present. */
    void add(String name, String phone);

    /** Returns the phone number for this name, or null if there is none. */
    String lookup(String name);

    /** Returns all names that start with the prefix, in alphabetical order. */
    List<String> namesStartingWith(String prefix);

    int size();
}
```

An adapter around `java.util.TreeMap` (a library Red-Black tree):

```java
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class TreeMapContactBook implements ContactBook {
    private final TreeMap<String, String> map = new TreeMap<>();

    @Override
    public void add(String name, String phone) {
        map.put(name, phone);
    }

    @Override
    public String lookup(String name) {
        return map.get(name);
    }

    @Override
    public List<String> namesStartingWith(String prefix) {
        // Every key that starts with prefix lies between prefix and prefix + Character.MAX_VALUE.
        return new ArrayList<>(map.subMap(prefix, prefix + Character.MAX_VALUE).keySet());
    }

    @Override
    public int size() {
        return map.size();
    }
}
```

A second solution, e.g. `HashContactBook`, would implement the same interface with a hash table written by the team.

### 5.2 Unit testing with JUnit

> **Concept — Unit testing**
>
> A **unit test** is a small, automatic check of one behaviour of one class: *"after adding Ada, `lookup("Ada")` returns her number."* A test suite is run after every change, so that a bug introduced today is caught today, not during your demo.
>
> **JUnit** is the standard unit-testing framework for Java. Each test is a method annotated with `@Test`. It calls your code and checks the result with **assertions** such as `assertEquals(expected, actual)`, `assertTrue(condition)`, or `assertNull(value)`. When an assertion fails, JUnit reports which test failed, what was expected, and what was obtained.
>
> IntelliJ IDEA, Eclipse, and VS Code (with the Java extensions) all include JUnit support: you can run a test class with a click and see green (pass) or red (fail) results.
>
> *Reference:* the official [JUnit User Guide](https://docs.junit.org/) — read "Writing Tests" (annotations and assertions) and "Running Tests" (IDE support). A short tutorial with examples: [Baeldung — A Guide to JUnit 5](https://www.baeldung.com/junit-5). JUnit 5 and JUnit 6 are written the same way for everything used in this project.

**How the provided tests are organized.** The tests are written **once**, in an abstract class (`DictionaryTest`, `MovieCatalogueTest`, or `PageCacheTest`), against the interface. A method `createDictionary()` (or similar) returns a new, empty structure. `Solution1Test` and `Solution2Test` are tiny subclasses that only say which solution to create:

```java
class Solution1Test extends DictionaryTest {
    @Override
    protected Dictionary createDictionary() {
        return new Solution1();
    }
}
```

Running `Solution1Test` runs **all** the shared tests on `Solution1`. A test that is specific to one solution goes in its own test class.

**Tests from the dataset.** The provided tests use small, hand-made inputs. You must add tests whose expected values come from the **real dataset**: open the file (in a text editor or a spreadsheet), pick a few records, and check that your program gives the answer you read in the file. For example, for Application B:

```java
@Test
void toyStoryIsReadCorrectly() throws IOException {
    for (Movie m : DatasetReader.readMovies(Path.of("data", "movies.csv"))) {
        cat.insertMovie(m);
    }
    Movie toy = cat.getById(1);
    assertEquals("Toy Story", toy.title());   // values read in movies.csv
    assertEquals(1995, toy.year());
}
```

Each shared test class also contains one dataset test marked `@Disabled`: remove that line once your `DatasetReader` works and the dataset file is in the `data` folder.

**What to test:**

- **Normal cases:** each operation of your functional requirements.
- **Edge cases:** empty structure, a single element, missing keys, duplicate keys, empty ranges, removing the last element.
- **Stress cases:** many insertions that force resizing or rebalancing; keys inserted in sorted order.
- **Random comparison with a reference:** apply the same long random sequence of operations (e.g. 100,000, with a fixed seed) to your solution and to a Java Collections class, and check after each operation that both give the same answer. This one test finds most of the bugs the others miss. The starter tests contain one such test; extend it to the operations it does not check yet.

### 5.3 Instrumentation counters

For each structure **written by your team or taken from class**, add counters that record what happens inside it, for example: number of rotations, tree height, number of probes per search, longest chain, number of resizes. Provide a method to read and reset them.

These counters let you **explain** your timings in Stage 5, not just report them. For example: *"`get` became slower at n = 10⁶ because the average number of probes rose from 1.4 to 3.8 when the load factor reached 0.9."*

Counters slow the structure down slightly. Make it possible to switch them off (e.g. a `static final boolean` flag) when you measure time.

### Outcome of Stage 3 → Code

Due at the end of **Week 12**:

- `DatasetReader`, both solutions, and any other classes you added.
- The extended JUnit test suite, which passes on both solutions, including tests from the dataset.
- A short progress note (half a page): what works, what does not, what is next.

---

## 6. Stage 4 — Experimental Evaluation

### What you do

1. Plan your experiments: what you vary, what you measure.
2. Build a **benchmark harness**: a program that runs the experiments automatically and saves the results.
3. Run all experiments and produce tables and plots.

> **Concept — Experimental design**
>
> An experiment changes some things on purpose and measures the effect, while keeping everything else fixed.
>
> - **Independent variables** are what you change on purpose: the solution, the input size n, the workload.
> - **Dependent variables** are what you measure: time, memory, counters.
> - **Controlled conditions** are what you keep fixed: same machine, same Java version and settings, same input data in the same order, nothing else running.
>
> Every result must be **reproducible**: someone else following your README should obtain similar numbers.
>
> *Reference:* C. Wohlin et al., *Experimentation in Software Engineering*, Springer, 2012 — Chapters 6–8 (planning and running experiments).

### 6.1 What to vary

- **Input size:** at least **5 sizes** spanning at least 3 orders of magnitude (e.g. 10³, 10⁴, 10⁵, 10⁶, and the full dataset). Take the first n records, or a random sample of n records with a fixed seed.
- **Workload:** at least **3 workloads**, i.e. a sequence of operations with a given mix and order. Include the realistic workload from the dataset and at least one **stress** workload (sorted insertions, all missing keys, very large ranges, …). The application sheets suggest workloads.
- **Solution:** your two solutions, run on exactly the same inputs. You may add a Java Collections class as a third, clearly labeled **baseline**.

### 6.2 What to measure

> **Concept — Performance metrics**
>
> - **Latency:** the time one operation takes. You usually report the **average** latency over many operations (total time ÷ number of operations), in nanoseconds (ns) or microseconds (µs).
> - **Throughput:** how many operations are done per second. For one thread, throughput = 1 ÷ average latency.
> - **Build time:** the time to create the structure from the dataset.
> - **Memory footprint:** how much memory the structure uses, often reported **per element** (bytes per record).
> - **Scalability:** how each metric changes when n grows. Compare it with the predicted complexity.

| Metric | Unit | How |
|---|---|---|
| Average latency per operation | ns or µs | Time a **batch** of operations, divide by the number of operations |
| Throughput | operations/s | Number of operations ÷ elapsed time |
| Build time | ms or s | Time to load the full dataset into the structure |
| Memory footprint | bytes per element | Measured (see 6.4) and estimated from your node layout |
| Structure-specific | — | Your instrumentation counters: height, rotations, probes, chain length, hit rate, … |

### 6.3 Benchmarking in Java

> **Concept — Benchmarking and microbenchmarks**
>
> A **benchmark** is a program that measures performance under controlled conditions. A **microbenchmark** measures one small operation (e.g. one `get`), which is what you will do. Microbenchmarks in Java are easy to get wrong, because the Java Virtual Machine (JVM) changes your program while it runs:
>
> - **JIT warm-up.** The JVM starts by interpreting your code, then compiles the frequently used parts to fast machine code (just-in-time compilation). The first runs are therefore much slower. Always run the experiment a few times **before** you start measuring.
> - **Dead-code elimination.** If the result of an operation is never used, the JIT compiler may remove the operation entirely, and you measure nothing. Always **use** your results, e.g. add them to a checksum that you print.
> - **Garbage collection (GC).** The JVM pauses from time to time to free unused memory. A pause during a measurement inflates it. Repeat measurements and report the **median**, which is not affected by one slow run.
> - **Timer resolution.** `System.nanoTime()` is precise, but timing a single operation of a few nanoseconds is not meaningful. Time **batches** of thousands of operations.
>
> The professional tool for Java microbenchmarks is **JMH** (Java Microbenchmark Harness), developed by the OpenJDK team. It handles warm-up, repetitions, and dead-code elimination for you. Using JMH is **optional**; the simple harness below is enough for this project.
>
> *References:* the [JMH project](https://github.com/openjdk/jmh), and in particular its **samples** (folder `jmh-samples`), each of which explains one pitfall with a runnable example (e.g. `JMHSample_08_DeadCode`). A step-by-step tutorial: [Jakob Jenkov — JMH](https://jenkov.com/java-performance/jmh.html).

**Example of a simple benchmark harness.** It measures the average time of `lookup` for one solution and one input size, and prints one CSV line:

```java
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class LookupBenchmark {
    static final int WARMUP_RUNS = 5;
    static final int MEASURED_RUNS = 7;

    /** Returns the median time per lookup, in nanoseconds. */
    static double measure(Supplier<ContactBook> factory,
                          List<String> names, List<String> queries) {
        long[] times = new long[MEASURED_RUNS];
        long checksum = 0;                         // prevents dead-code elimination

        for (int run = 0; run < WARMUP_RUNS + MEASURED_RUNS; run++) {
            ContactBook book = factory.get();      // a fresh structure each run
            for (String name : names) {
                book.add(name, "x");
            }

            long start = System.nanoTime();
            for (String q : queries) {             // a batch of many lookups
                if (book.lookup(q) != null) {
                    checksum++;
                }
            }
            long elapsed = System.nanoTime() - start;

            if (run >= WARMUP_RUNS) {              // ignore the warm-up runs
                times[run - WARMUP_RUNS] = elapsed;
            }
        }
        Arrays.sort(times);
        System.err.println("checksum = " + checksum);
        return (double) times[MEASURED_RUNS / 2] / queries.size();
    }

    public static void main(String[] args) {
        List<String> allNames = DataLoader.loadNames(args[0]);
        System.out.println("solution,n,ns_per_lookup");
        for (int n : new int[] {1_000, 10_000, 100_000, 1_000_000}) {
            List<String> names = allNames.subList(0, n);
            List<String> queries = DataLoader.sampleQueries(allNames, 100_000, 42); // fixed seed
            System.out.printf("tree,%d,%.1f%n", n,
                    measure(TreeMapContactBook::new, names, queries));
            System.out.printf("hash,%d,%.1f%n", n,
                    measure(HashContactBook::new, names, queries));
        }
    }
}
```

Redirect the output to a file (`java LookupBenchmark data.csv > results/lookup.csv`) and open it in a spreadsheet or plotting tool.

**Methodology checklist:**

- [ ] At least 5 warm-up runs, then at least **5 measured runs** per data point; report the **median** and the spread (min–max).
- [ ] Batches of many operations, timed with `System.nanoTime()`.
- [ ] Results are used (checksum).
- [ ] Fixed random seeds for every generated input.
- [ ] Both solutions receive exactly the same inputs, in the same order.
- [ ] Machine recorded: CPU, RAM, operating system, Java version, heap size (`-Xmx`).
- [ ] Laptop plugged in, other programs closed.
- [ ] Raw results saved as CSV files, not only as plots.

### 6.4 Measuring memory

Java does not tell you the size of an object directly. Use two methods and compare them:

1. **Measure:** record the used heap before and after building the structure, after asking for garbage collection a few times:

   ```java
   static long usedMemory() {
       Runtime rt = Runtime.getRuntime();
       for (int i = 0; i < 3; i++) {
           System.gc();
       }
       return rt.totalMemory() - rt.freeMemory();
   }
   ```

   `System.gc()` is only a request, so this measurement is approximate. Keep a reference to the structure until after the second measurement.

2. **Estimate:** compute the size of your nodes. On a typical 64-bit JVM, an object has a 12–16 byte header, a reference takes 4 bytes (8 on very large heaps), an `int` 4 bytes, a `long` 8 bytes, and objects are padded to a multiple of 8 bytes. For example, a Red-Black tree node with a key, a value, three references (left, right, parent), and a `boolean` color is about 12 + 5 × 4 + 1 = 33, padded to **40 bytes**, not counting the key and value objects themselves.

### 6.5 Presenting results

> **Concept — Log-log plots**
>
> When n spans several orders of magnitude (10³ to 10⁷), a normal plot squeezes all the small sizes into one corner. On a **log-log plot**, both axes use a logarithmic scale, so each power of 10 gets the same space. A bonus: if the time grows like n^k, the curve is a **straight line of slope k**. A constant-time operation gives a flat line; a linear one, a line of slope 1; O(log n) gives a curve that rises very slowly.
>
> Excel, Google Sheets, and Python's matplotlib (`plt.loglog`) can all produce log-log plots.

- Use **tables** for exact values and **plots** for trends.
- Plot both solutions (and the baseline, if any) **on the same axes**.
- Every plot needs a title, labeled axes **with units**, and a legend.

### Outcome of Stage 4 → Benchmark code and results

Due at the end of **Week 13**:

- The benchmark harness, in your code, with instructions in the README to rerun it.
- The raw results (CSV files) of all experiments, and the plots.

---

## 7. Stage 5 — Trade-off Analysis and Recommendation

### What you do

1. Analyze your results.
2. Recommend one design.
3. Write the report and prepare the presentation.

> **Concept — Trade-off analysis**
>
> In engineering, there is rarely a design that is best at everything. A **trade-off** is when improving one quality makes another worse: faster lookups but more memory, fast exact search but no range queries, simpler code but worse worst case. A **trade-off analysis** makes these explicit, using evidence, so that a decision can be made and defended.

### 7.1 Analyzing each result

For each important result, write three things:

1. **Observation:** what you measured, with numbers. *"At n = 10⁶, `lookup` takes 48 ns with the hash table and 410 ns with the Red-Black tree: 8.5× faster."*
2. **Comparison:** does it match your hypothesis and the theoretical complexity? *"This confirms Hypothesis 1. The tree's time grows like log n, as predicted (slope ≈ 0 on the log-log plot, slowly rising)."*
3. **Explanation:** why? Use your instrumentation counters and what you know about the structures and the machine. *"The tree visits about 21 nodes per search (measured average depth), each in a different place in memory."*

> **Concept — Why measurements differ from Big-O**
>
> Big-O describes how cost **grows** as n grows, for large n, and ignores constant factors. Real timings also depend on:
>
> - **Constant factors:** an O(1) operation that computes an expensive hash can be slower than an O(log n) one for realistic n.
> - **Memory locality:** the processor reads memory through **caches**, which are fast for data stored close together (an array) and slow for data scattered across memory (linked nodes). This is often why array-based structures beat linked ones in practice.
> - **Amortized costs:** a resize is expensive, but rare; it shows up as occasional spikes.
> - **The JVM:** JIT compilation and garbage collection (Section 6.3).
>
> When your results disagree with theory, these are good places to start looking.

### 7.2 Recommendation

Give a **design recommendation**: which solution would you deploy for this application, and under what conditions would you choose the other one instead? Check your recommendation against each non-functional requirement from Stage 1, in a table:

| Requirement | Target | Solution 1 | Solution 2 |
|---|---|---|---|
| `lookup`, full dataset, average | ≤ 1 µs | 0.05 µs ✓ | 0.41 µs ✓ |
| `namesStartingWith`, 1-letter prefix | ≤ 10 ms | 85 ms ✗ | 2.1 ms ✓ |
| … | … | … | … |

### 7.3 Threats to validity

> **Concept — Threats to validity**
>
> Every experiment has limits. Stating them honestly is part of good engineering: it tells the reader how far your conclusions can be trusted. Ask yourself: *what could make my results misleading?* Typical answers: measurement noise; garbage collection; a single machine; a single dataset; the way the workload was generated; differences in how carefully each solution was optimized; a library class compared with your own code.
>
> *Reference:* Wohlin et al. (Section 6), Chapter 8, "Validity evaluation".

### 7.4 The report (10–15 pages, PDF, excluding appendices)

| Section | Content | Suggested length |
|---|---|---|
| 1. Introduction | The application, why it matters, your two solutions, your main finding in 2–3 sentences | 0.5 page |
| 2. Requirements | Functional and non-functional requirements (Stage 1) | 0.5–1 page |
| 3. Background *(only if needed)* | Introduction to each non-covered structure (4.2) | 1.5–3 pages each |
| 4. Design | Both solutions: source, diagrams, operations, implementation details and expected performance (4.3, 4.4), hypotheses | 3–5 pages |
| 5. Implementation and testing | Key decisions (e.g. resizing, deletion, reading the dataset), adapters, testing strategy and the tests you added (Stage 3) | 1–2 pages |
| 6. Experimental methodology | Dataset preparation, sizes, workloads, machine, measurement procedure (Stage 4) | 1 page |
| 7. Results | Tables and plots, each with its observations | 2–4 pages |
| 8. Trade-off analysis | Comparisons and explanations; hypotheses confirmed or refuted (7.1) | 1–2 pages |
| 9. Recommendation and threats to validity | 7.2 and 7.3 | 0.5–1 page |
| 10. Conclusion | What you learned; what you would do next | 0.5 page |
| References | Datasets, textbooks, documentation, any other source | — |
| Appendix | Extra plots and tables; individual contribution statement | — |

### 7.5 The recorded presentation (12 minutes, including the demo)

Record a video of your presentation, with your slides and a **live demo**.

1. The application and its requirements (1–2 slides)
2. *(If applicable)* Teach the non-covered structure (2–3 slides)
3. The two designs, with diagrams (2 slides)
4. Methodology, briefly (1 slide)
5. Key results: your **3–4 most important plots** (3–4 slides)
6. Recommendation (1 slide)
7. **Live demo** (about 3 minutes): run your application on the real dataset with both solutions, on screen. For example, type prefixes and show the completions, run a range query, or replay an hour of requests through the cache and show the hit rate.

Requirements:

- **Every team member** presents a part, and is heard (camera optional).
- At most **12 minutes**. Format: MP4. A screen recording with voice is enough: e.g. Zoom or Teams (record a meeting and share your screen), OBS Studio (free), or PowerPoint's *Record* feature.
- Check the sound and the readability of your plots before submitting.

### Outcome of Stage 5 → Report (end of Week 14) and recorded presentation (end of Week 15)

---

## 8. Submission

| When | What |
|---|---|
| End of Week 10 | **Proposal** (PDF, 2–4 pages) |
| End of Week 12 | **Code**: `DatasetReader`, both solutions, extended tests (`.zip`, or a link to your repository if your instructor allows it); progress note |
| End of Week 13 | **Benchmark code and results**: harness, raw CSV results, plots |
| End of Week 14 | **Report** (PDF) and **final code** (`.zip`) |
| End of Week 15 | **Recorded presentation** (MP4) and **slides** (PDF) |

The final code `.zip` contains:

- all source files (the starter project with your additions);
- the raw benchmark results (CSV);
- a `README.md` with: how to download the dataset, how to run the tests, how to run the demo, and how to reproduce every table and plot of the report.

Do **not** include the dataset itself.

---

## 9. Timeline

| Week | Work | Submit at the end of the week |
|---|---|---|
| 10 | Stages 1 and 2: application, team, requirements, data structure choice, design, hypotheses | **Proposal** |
| 11 | Stage 3: read the dataset, implement the solutions, extend the tests (feedback on the proposal early in the week) | — |
| 12 | Stage 3 completed | **Code and tests**, progress note |
| 13 | Stage 4: benchmark harness, all experiments, plots | **Benchmark code and results** |
| 14 | Stage 5: analysis, recommendation, report | **Report** and **final code** |
| 15 | Stage 5: recorded presentation with demo | **Video** and **slides** |

The intermediate submissions are graded lightly, but they are where you get **feedback** in time to fix problems. Use them.

---

## 10. Grading Rubric

| Component | Criteria | Points |
|---|---|---|
| Proposal and checkpoints | Proposal, Week 10 (5); code and tests, Week 12 (2.5); benchmark results, Week 13 (2.5) | 10 |
| Requirements and design | Measurable, justified requirements; clear designs; correct expected complexity; meaningful hypotheses | 10 |
| Implementation and testing | Correct `DatasetReader` and solutions; sources cited; tests extended (tests from the dataset, edge cases, random comparison); instrumentation where possible; code quality; README | 20 |
| Experimental evaluation | Sizes, workloads, warm-up, repetitions, fairness, reproducibility; clear tables and plots | 15 |
| Analysis and recommendation | Observations explained using theory and counters; hypotheses discussed; requirements checked; justified recommendation; threats to validity | 20 |
| Report quality | Structure, clarity, diagrams, terminology, references; quality of the implementation descriptions (Section 4.3) and of the Background section | 10 |
| Recorded presentation | Clarity, timing, choice of plots, live demo, every member presents | 15 |
| **Total** | | **100** |

---

## 11. Advice

- **Start small.** Get everything working on 1,000 records (e.g. the small version of the dataset) before scaling up.
- **Verify before you measure.** A fast but wrong structure gives meaningless timings.
- **Increase the heap if needed:** run with e.g. `java -Xmx4g …`, and report the setting.
- **Automate.** If producing a plot takes more than one command, you will not redo it when you fix a bug.
- **Surprises are good.** If your measurements contradict the textbook, do not hide it: investigate it.
- **Cite everything** you did not create: datasets, references, documentation, and any code you adapted.

---

## 12. References

- Sommerville, I. *Software Engineering*, 10th ed. Pearson, 2015. Chapter 4.
- Nielsen, J. *Response Times: The 3 Important Limits*. Nielsen Norman Group. https://www.nngroup.com/articles/response-times-3-important-limits/
- Chacon, S. and Straub, B. *Pro Git*, 2nd ed. https://git-scm.com/book/en/v2
- Gamma, E., Helm, R., Johnson, R., Vlissides, J. *Design Patterns: Elements of Reusable Object-Oriented Software*. Addison-Wesley, 1994. And: https://refactoring.guru/design-patterns/adapter
- JUnit User Guide. https://docs.junit.org/ — and Baeldung, *A Guide to JUnit 5*, https://www.baeldung.com/junit-5
- JMH — Java Microbenchmark Harness, and its samples. https://github.com/openjdk/jmh — and Jenkov, J., *JMH*, https://jenkov.com/java-performance/jmh.html
- Wohlin, C. et al. *Experimentation in Software Engineering*. Springer, 2012.
- OpenJDK source code. https://github.com/openjdk/jdk
