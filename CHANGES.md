# Changes in 0.1.1

## Changes visible to users of the library

Bug fixes:

* Eliminate warning issued by ClojureScript compiler caused by use of a not-yet-defined type name [CRRBV-24](https://clojure.atlassian.net/browse/CRRBV-24)

Minor code cleanup:

* Eliminate redundant require of a namespace
  [commit](https://github.com/clojure/core.rrb-vector/commit/8c3bdc03f4d4c73326ac0146310bf472cda4d035)

Documentation:

* Created this change log.
* Added new introductory text at beginning of README explaining
  briefly why someone might want to use this library.
* Added doc/benchmarks/benchmarks.md document, with link from README,
  showing some benchmark results of this library versus several other
  implementations of vector data structures, some of which are based
  on RRB trees, some of which have only a linear time vector
  concatenation operation.
* Added doc/rrb-tree-notes.md with links to other implementations of
  RRB trees, and papers and theses that have been written about them.


## Changes relevant to those who test and develop the library itself

* Added doc/use-transducers/ directory with README and proposed patch
  for speeding up some of the code by using transducers, which in the
  cases used provide a speedup by avoiding allocating multiple
  intermediate sequences.


# Changes in 0.1.0

## Changes visible to users of the library

Bug fixes:

* Test case added that was failing before the fixes for other bugs listed below, but now works with those fixes, so likely its root cause has also been corrected [CRRBV-12](https://clojure.atlassian.net/browse/CRRBV-12)
* Fixed bug that caused assoc and assoc! to fail when used on vectors of primitives [CRRBV-13](https://clojure.atlassian.net/browse/CRRBV-13)
* Fixed bug where internal tree data structure gets too "tall and skinny", eventually exceeding the limits supported by the library's implementation [CRRBV-14](https://clojure.atlassian.net/browse/CRRBV-14)
* Bug with similar root cause, and the same fix as, CRRBV-14 [CRRBV-17](https://clojure.atlassian.net/browse/CRRBV-17)
* Fix incorrect condition check for when a subtree was full or not [CRRBV-20](https://clojure.atlassian.net/browse/CRRBV-20)
* Fix of several bugs found during implementation and testing of other issues [CRRBV-21](https://clojure.atlassian.net/browse/CRRBV-21)
* Fix off by one bug that caused incorrect results for pop and pop! operations on vectors of certain sizes [CRRBV-22](https://clojure.atlassian.net/browse/CRRBV-22)
* Fix incorrect hash calculation for empty RRB vectors in ClojureScript implementation [CRRBV-25](https://clojure.atlassian.net/browse/CRRBV-25)
* Fix potential data race for multi-threaded Clojure programs using this library where the hash value could be returned incorrectly as -1 instead of the correct value [CRRBV-26](https://clojure.atlassian.net/browse/CRRBV-26)

Enhancements:

* Support bootstrapped ClojureScript [CRRBV-16](https://clojure.atlassian.net/browse/CRRBV-16)

## Changes relevant to those who test and develop the library itself

In order to continue to support Clojure 1.6.0, core.rrb-vector uses
neither `.cljc` files nor transducers, for which Clojure added support
in Clojure 1.7.0.  However, in preparation for a future
core.rrb-vector release that requires Clojure 1.7.0 or later, several
test namespaces have been made nearly identical between their Clojure
and ClojureScript versions, so that they can be replaced with a single
`.cljc` file in the future, with only a few small uses of reader
conditionals.

The implementation of core.rrb-vector still contains similar, but
independent, implementations in Clojure and ClojureScript, and no
attempt has been made to make their implementations so similar that
merging them into a combined .cljc file would be reasonable.
Replacing some or all of the Clojure implementation with Java source
code may help improve the constant factors of the execution time
enough to warrant such a change in a future version of this library.

* Updated Maven pom.xml file, Leiningen project.clj file, and Clojure
  deps.edn files, so that any of them may be used for the purposes of
  developing and testing this library further.  Added examples of
  commands for all of these tools for running this library's tests,
  and commands for Ubuntu Linux and macOS for installing two different
  JavaScript run time engines that can be used to test the
  ClojureScript implementation.
* The ClojureScript tests are now run, using JDK's Nashorn JavaScript
  run time environment, on build.clojure.org, in addition to the
  previous behavior of running the Clojure tests.
* Rearranged tests in the test namespaces extensively, including some
  of their namespace names.  There is now a `test-common` namespace
  that contains most of the tests, which can thus be run on both the
  Clojure and ClojureScript implementations.  There are
  `test-clj-only` and `test-cljs-only` namespaces for tests unique to
  one of the implementations.
* The `clojure.core.rrb-vector.debug/dbg-vec` function has been
  enhanced to support showing internal details of both the built in
  Clojure vectors, persistent and transient, as well as
  core.rrb-vector's data structures.  The `debug` namespace also has
  several `checking-*` functions, e.g. `checking-catvec`,
  `checking-subvec`, etc. that behave the same as their non-checking
  counterparts, but perform significant sanity checking of their
  return values before they return, and while they have some
  configuration options, by default they throw exceptions if they find
  errors in the returned values.  Any such exceptions are likely to be
  due to bugs in core.rrb-vector.  These checking functions are
  significantly slower than the non-checking variants, and only
  intended for testing the core.rrb-vector library.  The details of
  configuring their options are not intended to be stable, and thus
  likely to change in future releases of this library.
