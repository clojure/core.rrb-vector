# core.rrb-vector benchmarks

See the section "How the benchmarks were run" below for how the
results were created.


## Benchmark results

These benchmark results are based upon benchmark code written for the
[`bifurcan`](https://github.com/lacuna/bifurcan) library.  The
[benchmarks published originally
here](https://github.com/lacuna/bifurcan/blob/master/doc/comparison.md)
include comparisons of data structures other than vectors, e.g. hash
maps and sorted sets.

Note that for the data structures we care most about here, those
results call them "lists" rather than "vectors".  I will use "lists"
here to be consistent with the `bifurcan` benchmarks.  The portion of
the `bifurcan` results relevant to lists can be found
[here](https://github.com/lacuna/bifurcan/blob/master/doc/comparison.md#lists).

The time measurements here were made on a different machine than the
`bifurcan` results, so you should not infer anything from the absolute
time measurements here vs. there.  Only the relative time measurements
between the libraries in one set of benchmark results.

![](images/images/list_construct.png)
![](images/images/list_construct_all_but_vavr.png)

![](images/images/list_iterate.png)
![](images/images/list_iterate_all_but_core_rrb_vector.png)

![](images/images/list_lookup.png)

![](images/images/concat.png)
![](images/images/concat_time_all_rrb.png)
![](images/images/concat_time_all_rrb_but_core_rrb_vector.png)


## How the benchmarks were run

To run benchmarks from the Bifurcan project, with small modifications
that add core.rrb-vector to the list of libraries that are measured,
follow these steps.  Note that the version of the `bifurcan.List` data
structure code used in these results has a few proposed bug fixes from
the version of the code in the original `bifurcan` repository, but I
do not believe they affect the performance in any noticeable way.

```bash
$ git clone https://github.com/jafingerhut/bifurcan
$ cd bifurcan
$ git checkout 457fd0346b78392f39e4c0e79f1e43b7847ea93b
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

The benchmark results here were measured on a system with these
properties:

* MacBook Pro model 11,2, 2.5 GHz Intel Core i7 with peak clock speed
  3.6 GHz, 16 GB RAM
* macOS 10.14.6
* AdoptOpenJDK 11.0.4, 64-bit server build 11.0.4+11
* Leiningen 2.9.1
* To see the versions of the list libraries that were measured, look
  in the project.clj file of the bifurcan project at the commit
  mentioned above.  For core.rrb-vector, the only measured library
  that is written in Clojure, that project.clj file currently
  specifies Clojure version 1.8.0.  The other libraries are written in
  Java.
