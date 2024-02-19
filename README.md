# core.rrb-vector

Why would anyone want to use this library?  The two primary answers
are:

+ You want faster concatenation of vectors, which core.rrb-vector's
  `catvec` function provides for both Clojure and ClojureScript.
+ You use vectors of Java primitive types like long, double, etc.,
  returned by Clojure's `vector-of` function, e.g. to reduce memory
  usage to about 1/3 of the memory required by vectors of arbitrary
  objects, and
  + You want the speed enabled by using the transient versions of such
    vectors.  Clojure does not implement transients for primitive
    vectors created via `vector-of` -- core.rrb-vector does.

Vectors are one of the most commonly used data structures within
Clojure.  Likely you already know that creating a vector equal to `v`
plus a new element `e` appended to the end using the expression `(conj
v e)` has a run time that is "effectively constant", i.e. it takes
O(log N) time in the size N of `v`, where the base of the logarithm is
32, so it is a constant at most 4 for all vector sizes up to a
million, and at most 7 for all vector sizes that Clojure supports.

The fastest way to concatenate two vectors `v1` and `v2` into a single
new vector is using an expression like `(into v1 v2)`.  This is
implemented by repeatedly appending a single element from the second
vector to the first, so it takes linear time in the size of `v2`
(multiplied by the effectively constant time mentioned above).

Aside: There might be another expression that has a better _constant
factor_ for its run time than `(into v1 v2)` does, and is thus faster.
However, any other such expression will still take at least linear
time in the size of the second vector.

The core.rrb-vector library uses a tree structure similar to the one
that Clojure uses internally for vectors, but generalizes it in such a
way that producing a new tree that represents the concatenation of two
input vectors using the `catvec` function can be done in O(log N)
time, where N is the size of the result.

You can give `catvec` vectors created in all of the ways you already
normally do, and while it will return a new type of object, this new
type behaves in all of the ways you expect a Clojure vector to behave.
This new type of vector is indistinguishable from a normal Clojure
vector unless you examine the value of `(type v)` or `(class v)`.  In
particular, `(vector? v)` is true for this new type, you can call all
of the usual sequence-based functions on it to examine or process its
elements, you can call `conj` on it, `nth`, etc.

Thus if you have a program where frequently concatenating large
vectors to produce new vectors is useful, core.rrb-vector may help you
write a much faster program in a more natural way.

This library is an implementation of the confluently persistent vector
data structure introduced in the paper "RRB-Trees: Efficient Immutable
Vectors", EPFL-REPORT-169879, September, 2011, by Phil Bagwell and
Tiark Rompf.

RRB-Trees build upon Clojure's internal `PersistentVector` class used
to implement its built in vectors, adding logarithmic time
concatenation and slicing (i.e. create sub-vectors from input
vectors).  ClojureScript is supported with the same API, except for
the absence of the `vector-of` function.

The main functions provided are `clojure.core.rrb-vector/catvec`,
performing vector concatenation, and `clojure.core.rrb-vector/subvec`,
which produces a new vector containing the appropriate subrange of the
input vector (in contrast to `clojure.core/subvec`, which returns a
view on the input vector).

Like Clojure vectors, core.rrb-vector vectors can store arbitrary
values, or using `vector-of` you can create vectors restricted to one
primitive type, e.g. long, double, etc.  The core.rrb-vector
implementation provides seamless interoperability with the built in
Clojure vectors of class `clojure.lang.PersistentVector`,
`clojure.core.Vec` (vectors of primitive values) and
`clojure.lang.APersistentVector$SubVector` instances:
`clojure.core.rrb-vector/catvec` and `clojure.core.rrb-vector/subvec`
convert their inputs to `clojure.core.rrb_vector.rrbt.Vector`
instances whenever necessary (this is a very fast constant time
operation for PersistentVector and primitive vectors; for SubVector it
is O(log N), where N is the size of the underlying vector).

`clojure.core.rrb-vector` also provides its own versions of `vector`,
`vector-of`, and `vec` that always produce
`clojure.core.rrb_vector.rrbt.Vector` instances.  Note that
`vector-of` accepts `:object` as one of the possible type arguments,
in addition to keywords naming primitive types.


## Usage

core.rrb-vector exports one public namespace:

    (require '[clojure.core.rrb-vector :as fv])

Note that the ClojureScript version uses the same namespace name (it
*does not* use the alternative `cljs.*` prefix!). This is because the
API is precisely the same (except `clojure.core.rrb-vector/vector-of`
only makes sense on the JVM and is therefore not available in
ClojureScript).

The docstring attached to the namespace provides an overview of the
available functionality (as found at the top of this README):

    (doc clojure.core.rrb-vector)

The new functionality is accessible through two functions:
`clojure.core.rrb-vector/subvec`, which provides logarithmic-time
non-view slicing (in contrast to `clojure.core/subvec`, which is a
constant-time operation producing view vectors that prevent the
underlying vector from becoming eligible for garbage collection), and
`clojure.core.rrb-vector/catvec`, which provides logarithmic-time
concatenation. Crucially, these can be applied to regular
Clojure(Script) vectors.

    (doc fv/subvec)
    (doc fv/catvec)

    ;; apply catvec and subvec to regular Clojure(Script) vectors
    (fv/catvec (vec (range 1234)) (vec (range 8765)))
    (fv/subvec (vec (range 1024)) 123 456)

Additionally, several functions for constructing RRB vectors are
provided. There is rarely any reason to use them, since, as mentioned
above, the interesting functions exported by core.rrb-vector work with
regular vectors. Note that `clojure.core.rrb-vector/vec`, in contrast
to `clojure.core/vec`, reuses the internal tree of its input if it
already is a vector (of any type) and does not alias short arrays.
When passed a non-vector argument, it returns an RRB vector.

    (doc fv/vector)
    (doc fv/vector-of)
    (doc fv/vec)

The debug namespace bundled with core.rrb-vector provides several
utilities used by the test suite, as well as a function for
visualizing the internal structure of vectors that works with regular
Clojure(Script) vectors and RRB vectors.

    ;; for peeking under the hood
    (require '[clojure.core.rrb-vector.debug :as dv])
    (dv/dbg-vec (fv/catvec (vec (range 1234)) (vec (range 8765))))


## Releases and dependency information

core.rrb-vector requires Clojure >= 1.5.0. View vectors created by
`clojure.core/subvec` are correctly handled for Clojure >= 1.6.0. The
ClojureScript version is regularly tested against the most recent
ClojureScript release.

core.rrb-vector releases are available from Maven Central. Development
snapshots are available from the Sonatype OSS repository.

* [Released versions](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.clojure%22%20AND%20a%3A%22core.rrb-vector%22)
* [Development snapshots](https://oss.sonatype.org/index.html#nexus-search;gav~org.clojure~core.rrb-vector~~~)
* [Change log](CHANGES.md) of changes made in this library.
* Some [benchmark results](doc/benchmarks/benchmarks.md) comparing the run time of core.rrb-vector's JVM implementation against other vector/list implementations on the JVM.

Follow the first link above to discover the current release number.

[CLI/`deps.edn`](https://clojure.org/reference/deps_and_cli) dependency information:
```clojure
org.clojure/core.rrb-vector {:mvn/version "${version}"}
```

[Leiningen](http://leiningen.org/) dependency information:

    [org.clojure/core.rrb-vector "${version}"]

[Maven](http://maven.apache.org/) dependency information:

    <dependency>
      <groupId>org.clojure</groupId>
      <artifactId>core.rrb-vector</artifactId>
      <version>${version}</version>
    </dependency>

[Gradle](http://www.gradle.org/) dependency information:

    compile "org.clojure:core.rrb-vector:${version}"


## TODO

 1. more tests;

 2. performance: general perf tuning, more efficient `catvec`
    implementation (to replace current seq-ops-based impl).


## Developer information

core.rrb-vector is being developed as a Clojure Contrib project, see
the
[What is Clojure Contrib](https://clojure.org/dev/contrib_libs)
page for details. Patches will only be accepted from developers who
have signed the Clojure Contributor Agreement.

* [GitHub project](https://github.com/clojure/core.rrb-vector)
* [Bug Tracker](https://clojure.atlassian.net/browse/CRRBV)
* [Continuous Integration](https://github.com/clojure/core.rrb-vector/actions/workflows/test.yml)


### Useful Maven commands

To run Clojure and ClojureScript tests:
```bash
$ mvn -DCLOJURE_VERSION=1.10.1 -Dclojure.version=1.10.1 clean test
```

Clojure versions as old as 1.5.1 can be tested with such a command,
but the ClojureScript tests only work when using Clojure 1.8.0 or
later.

To run tests and, if successful, create a JAR file in the targets
directory:
```bash
$ mvn -DCLOJURE_VERSION=1.10.1 -Dclojure.version=1.10.1 clean package
```

Prerequisites: Only Java and Maven need to be installed.  Maven will
download whatever versions of Clojure are needed for the command you
use.  Both Clojure and ClojureScript tests are run with the commands
given here.  They use the Nashorn JavaScript run time environment
included with Java -- no other JavaScript run time is needed.


### Useful clj CLI commands

To run relatively short Clojure tests, but no ClojureScript tests:
```bash
$ ./script/jdo test
```

To run relatively short ClojureScript tests, but no Clojure tests:
```bash
$ ./script/sdo test
```

Warning: Currently the command above for running ClojureScript tests
does _not_ show warnings from the ClojureScript compiler.  I have seen
some ClojureScript compiler warnings appear when running the Maven
command above, and the Leiningen command given below for running
ClojureScript tests, that unfortunately do not appear using
`./script/sdo test`.  Suggestions welcome on how to make that command
also show similar warnings.

Replace `test` in the commands above with one of the following for
other useful things:

* `sock` (or no argument at all) - start a REPL, and listen for a
  socket REPL connection on TCP port 50505
* `long` - run a longer set of tests
* `coll` - run generative tests from
  [`collection-check`](https://github.com/ztellman/collection-check)
  library
* `east` - run Eastwood lint tool (clj version only, not cljs)


### Useful Leiningen commands

To run Clojure tests, but no ClojureScript tests:
```bash
$ lein with-profile +1.10 test
```
You can test with Clojure versions 1.5 through 1.10 by specifying that
version number after the `+`.

Prerequisites: Only Java and Leiningen.  Leiningen will download
whatever versions of Clojure and other libraries are needed.

To run ClojureScript tests with Node.js and SpiderMonkey JavaScript
runtimes, but no Clojure tests:
```bash
$ lein with-profile +cljs cljsbuild test
```
Add `node` or `spidermonkey` as a separate argument after `test` to
restrict the JavaScript runtime used to only the one you specify.  You
may need to adjust the command names in the `:test-commands` section
of the `project.clj` file if the command for running those JavaScript
runtimes have a different name on your system than what is used there.

Prerequisites: Java, Leiningen, and either or both of Node.js and
SpiderMonkey JavaScript run time environments.

To run normal Clojure tests, plus the
[`collection-check`](https://github.com/ztellman/collection-check)
tests, but no ClojureScript tests:
```bash
$ lein with-profile +coll,+1.7 test
```
The `collection-check` tests require Clojure 1.7.0 or later, I believe
because collection-check and/or its dependencies require that.

There is no existing command configured to run `collection-check`
tests with ClojureScript.

To start a REPL from Leiningen with Clojure versions 1.6.0 and older,
you must use Leiningen 2.8.0 (likely some other versions work, too).


### Installing other software you will need

For all of the development commands you must have Java installed.
This includes the ClojureScript compile and test commands, since the
ClojureScript compiler is at least partially written in the Java
version of Clojure.


#### Java

Install one or more of the pre-built binaries from
[AdoptOpenJDK](https://adoptopenjdk.net), or several other providers
of Java binaries.

Additional methods:
* Ubuntu 18.04 Linux: `sudo apt-get install default-jre`


#### Maven

For any `mvn` command you must install
[Maven](https://maven.apache.org).

* Ubuntu 18.04 Linux: `sudo apt-get install maven`
* macOS
  * plus Homebrew: `brew install maven`
  * plus MacPorts: `sudo port install maven3`, then either use the
    command `mvn3`, or to use `mvn` also run the command `sudo port
    select --set maven maven3`.


#### Leiningen

An install script and instructions are available on the
[Leiningen](https://leiningen.org) site.


#### Node.js JavaScript run time environment

Installation instructions for many different versions of Node.js are
available on the [Node.js web site](https://nodejs.org).  You can also
install it using the commands below.

* Ubuntu 18.04 Linux: `sudo apt-get install nodejs`
* macOS
  * plus Homebrew: `brew install node`
  * plus MacPorts: `sudo port install nodejs10`.  You can see other
    versions available via the command `port list | grep nodejs`.


#### SpiderMonkey JavaScript run time environment

Installation instructions for many different versions of SpiderMonkey
are available on the [SpiderMonkey web
site](https://developer.mozilla.org/en-US/docs/Mozilla/Projects/SpiderMonkey).
You may also install it using the commands below.

* Ubuntu 18.04 Linux: `sudo apt-get install libmozjs-52-dev`
* macOS
  * plus Homebrew: As of 2019-Sep-24, `brew install spidermonkey`
    installs version 1.8.5 of SpiderMonkey, which according to the
    Wikipedia page on SpiderMonkey was first released in 2011, with at
    least one release per year after that.  The ClojureScript tests
    fail to run using this version of SpiderMonkey.  It seems worth
    avoiding this version of SpiderMonkey for the purposes of testing
    `core.rrb-vector`.
  * plus MacPorts: `sudo port install mozjs52`


## Clojure(Script) code reuse

core.rrb-vector's vectors support the same basic functionality regular
Clojure's vectors do (with the omissions listed above). Where
possible, this is achieved by reusing code from Clojure's gvec and
ClojureScript's PersistentVector implementations. The Clojure(Script)
source files containing the relevant code carry the following
copyright notice:

    Copyright (c) Rich Hickey. All rights reserved.
    The use and distribution terms for this software are covered by the
    Eclipse Public License 1.0 (https://opensource.org/license/epl-1-0/)
    which can be found in the file epl-v10.html at the root of this distribution.
    By using this software in any fashion, you are agreeing to be bound by
      the terms of this license.
    You must not remove this notice, or any other, from this software.


## Licence

Copyright © Michał Marczyk, Andy Fingerhut, Rich Hickey and contributors

Distributed under the
[Eclipse Public License 1.0](https://opensource.org/license/epl-1-0/),
the same as Clojure. The licence text can be found in the
`epl-v10.html` file at the root of this distribution.
