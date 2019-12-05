# Other implementations and descriptions of RRB trees

Note that most implementations have an associated paper.  If they have
an author in common, then typically the paper or talk describes the
implementation they published.

Implementations:

* TBD: Tiark Rompf and Phil Bagwell's original implementation code?
  * If it is available somewhere, likely it is instrumented for
    experimentation on multiple variations of the algorithms, and
    counting significant events that they were trying to optimize.
    Thus more of a research implementation than one intended for use
    in production.
* [`scala-rrb-vector`](https://github.com/nicolasstucki/scala-rrb-vector)
  Scala library by Nicolas Stucki
  * As of Oct 2019, this library has a few unfixed bugs that were
    reported as Github issues in 2017.  I have not examined them to
    see how easy they might be to fix.
* [`bifurcan`](https://github.com/lacuna/bifurcan) Java library by
  Zach Tellman, Java class `io.lacuna.bifurcan.List`
* [`Paguro`](https://github.com/GlenKPeterson/Paguro) Java library by
  Glen Peterson, Java class `org.organicdesign.fp.collections.RrbTree`.
  * From some comments in the code, it appears that perhaps this
    implementation is more precisely a data structure based upon
    B-trees, because those comments imply that nodes in the tree can
    have a number of children varying between some branching factor B,
    and be as low as B/2.
* [`c-rrb`](https://github.com/hypirion/c-rrb) C library by Jean
  Niklas L'orange
* [`Array`](https://github.com/xash/Array) RRB trees implemented in
  JavaScript for use in the Elm programming language.
* [`immer`](https://sinusoid.es/immer) C++ library by Juan Pedro
  Bolivar Puente
* [`Vector`](https://docs.rs/im/12.3.3/im/vector/enum.Vector.html)
  Rust implementation of RRB trees by Bodil Stokke
  * [Source code](https://docs.rs/crate/im/12.3.3/source/)
* [`RRBVector`](https://github.com/rmunn/FSharpx.Collections/blob/rrb-vector/src/FSharpx.Collections.Experimental/RRBVector.fs)
  F# library by Robin Munn.
  * According to discussion on [this Github
    issue](https://github.com/fsprojects/FSharpx.Collections/issues/72)
    it appears to have known bugs the author would still like to find
    and fix as of 2019-Oct-10.


Implementation of immutable vectors that are not RRB trees:

* [`Clojure`](https://github.com/clojure/clojure) collections library,
  Java class `clojure.lang.PersistentVector` implemented in Java
  * Also class `clojure.core.Vector` implemented in Clojure, with
    memory/time optimizations achieved by restricting vector elements
    to all be the same type of Java primitive, e.g. all `long` vector
    elements, or all `double`.
* [`Scala`](https://github.com/scala/scala) collection library, Java
  class `scala.collection.immutable.Vector`
  * In source file `src/library/scala/collection/immutable/Vector.scala`
  * As far as I can tell, as of 2019-Oct-10, it appears that this
    class does _not_ use RRB trees, and thus implements concatenation
    of vectors in linear time in the length of the second vector.
    [This Github
    issue](https://github.com/nicolasstucki/scala-rrb-vector/issues/9)
    from April 2019 implies that Scala has not yet had an RRB tree
    implementation incorporated into its standard library.


Published papers and theses:

* Phil Bagwell, Tiark Rompf, "RRB-Trees: Efficient Immutable Vectors",
  EPFL-REPORT-169879, September, 2011
  [[PDF]](http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.592.5377&rep=rep1&type=pdf)
  [[SemanticScholar
  page]](https://www.semanticscholar.org/paper/RRB-Trees-%3A-Efficient-Immutable-Vectors-Phil-Tiark-Bagwell-Rompf/30c8c562f6421ab6b00d0b7faebd897c407de69c)
  * Phil Bagwell talk
    [[video]](https://www.youtube.com/watch?v=K2NYwP90bNs), "Striving
    to Make Things Simple and Fast", January 2013, given at Clojure
    conj conference
* Jean Niklas L'orange, "Improving RRB-Tree Performance through
  Transience", MAster Thesis, 2014,
  [[PDF]](https://hypirion.com/thesis.pdf)
* "RRB Vector: A Practical General Purpose Immutable Sequence",
  Nicolas Stucki, Tiark, Rompf, Vlad Ureche, Phil Bagwell, Proc. of
  the 20th ACM SIGPLAN International Conference on Functional
  Programming, 2015 [[ACM digital library
  link]](http://dx.doi.org/10.1145/2784731.2784739)
  [[PDF]](https://github.com/nicolasstucki/scala-rrb-vector/blob/master/documents/RRB%20Vector%20-%20A%20Practical%20General%20Purpose%20Immutable%20Sequence.pdf)
* Nicolas Stucki, "Turning Relaxed Radix Balanced Vector from Theory
  into Practice for Scala Collections", Master Thesis, 2015
  [[PDF]](https://github.com/nicolasstucki/scala-rrb-vector/blob/master/documents/Master%20Thesis%20-%20Nicolas%20Stucki%20-%20Turning%20Relaxed%20Radix%20Balanced%20Vector%20from%20Theory%20into%20Practice%20for%20Scala%20Collections.pdf?raw=true)
* Juan Pedro Bolivar Puente, "Persistence for the Masses: RRB-Vectors
  in a Systems Language", Proc. ACM Program. Lang. 1, ICFP, Article 16
  (September 2017), https://doi.org/10.1145/3110260
  [[PDF]](https://public.sinusoid.es/misc/immer/immer-icfp17.pdf)
  * Juan's talk [[video]](https://www.youtube.com/watch?v=sPhpelUfu8Q)
    "Postmodern immutable data structures" given at CppCon 2017
* Bodil Stokke talk
  [[video]](https://www.youtube.com/watch?v=cUx2b_FO8EQ) "Meetings With
  Remarkable Trees" given at ClojuTRE 2018


Related things:

* Jean Niklas L'orange's series of articles on Clojure's persistent
  vector data structure and how it works inside.  These are good
  tutorial style articles.  I have not found any similar articles like
  these on RRB trees.
  * ["Understanding Clojure's Persistent Vectors, Part
    1"](https://hypirion.com/musings/understanding-persistent-vector-pt-1),
    September 2013
  * ["Understanding Clojure's Persistent Vectors, Part
    2](https://hypirion.com/musings/understanding-persistent-vector-pt-2),
    October 2013
  * ["Understanding Clojure's Persistent Vectors, Part
    3](https://hypirion.com/musings/understanding-persistent-vector-pt-3)
    April 2014
  * ["Understanding Clojure's
    Transients"](https://hypirion.com/musings/understanding-clojure-transients),
    October 2014
  * ["Persistent Vector
    Performance"](https://hypirion.com/musings/persistent-vector-performance),
    January 2015
  * ["Persistent Vector Performance
    Summarised"](https://hypirion.com/musings/persistent-vector-performance-summarised),
    February 2015
* StackOverflow
  [question](https://stackoverflow.com/questions/14007153/what-invariant-do-rrb-trees-maintain)
  "What invariant do RRB-trees maintain?"


Not RRB trees, but somewhat related ideas:

* ["Theory and practice of chunked
  sequences"](http://www.andrew.cmu.edu/user/mrainey//chunkedseq/chunkedseq.html)
  web page has links to papers, talks, and Github repository
  containing C++ of their ideas.
