# core.rrb-vector benchmarks

See the section "How the benchmarks were run" below for how the
results were created.


## Benchmark results


## How the benchmarks were run

To run benchmarks from the Bifurcan project, with small modifications
that add core.rrb-vector to the list of libraries that are measured,
follow these steps.

```bash
$ git clone https://github.com/jafingerhut/bifurcan
$ cd bifurcan
$ git checkout 8f61824e4ab0e6a9d8f1aecd8cfe19ea9dfc0493
$ ./benchmarks/run-vectors-only.sh
```

To copy the data and images produced as a result of the above, to
where I copied them in this repository:

```bash
$ DST=/path/to/my/clone/of/core.rrb-vector/doc/benchmarks
$ mkdir $DST/images $DST/data
$ cp -p benchmarks/images/list*.png benchmarks/images/concat*.png $DST/images
$ cp -p benchmarks/data/benchmarks.edn benchmarks/data/concat.csv benchmarks/data/list*.csv $DST/data
```

The benchmark results given below were measured on a system with these
properties:

* MacBook Pro model 11,2, 2.5 GHz Intel Core i7 with peak clock speed 3.6 GHz
* macOS 10.14.6
* AdoptOpenJDK 11.0.4, 64-bit server build 11.0.4+11
* Leiningen 2.9.1
* See the bifurcan project at the commit mentioned above for its
  Leiningen project.clj file for the versions of the vector/list
  libraries that were measured.  For core.rrb-vector, which I believe
  is the only measured library that is written in Clojure, that
  project.clj file currently specifies Clojure version 1.8.0

