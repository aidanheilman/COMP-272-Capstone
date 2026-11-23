On MacOs, to rerun the Benchmark harness, use the command
java -cp target/test-classes:target/classes autocomplete.benchmarking.BenchmarkFINAL 

This will output the results to the terminal.
To route the results to overwrite the csv file, use:
java -cp target/test-classes:target/classes autocomplete.benchmarking.BenchmarkFINAL > data.csv

Note: The benchmark harness is dependent on the source code, count_1w.txt downloaded in the data folder,
as well as a lexicgraphically sorted version of count_1w.txt. 