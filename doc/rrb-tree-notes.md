# Other implementations and descriptions of RRB trees

Implementations:

* [`scala-rrb-vector`](https://github.com/nicolasstucki/scala-rrb-vector)
  by Nicolas Stucki
* [`bifurcan`](https://github.com/lacuna/bifurcan) library by Zach
  Tellman, Java class `io.lacuna.bifurcan.List`
* [`Paguro`](https://github.com/GlenKPeterson/Paguro) library by Glen
  Peterson, Java class `org.organicdesign.fp.collections.RrbTree`
* [`Scala`](https://github.com/scala/scala) collection library, Java
  class `scala.collection.immutable.Vector`
  * In source file `src/library/scala/collection/immutable/Vector.scala`
  * As far as I can tell, as of 2019-Oct-10, it appears that this
    class does _not_ use RRB trees, and thus implements concatenation
    of vectors in linear time in the length of the second vector.
  * commit 1c67c5b849d57a3fae89c4156f4a15defceccbf4 2009-Oct-10 was
    first to make them immutable vectors.  About 10 to 20 follow up
    commits later in the same month to fix bugs and enhance things
* [`c-rrb`](https://github.com/hypirion/c-rrb) RRB trees implemented
  in C
* [`Array`](https://github.com/xash/Array) RRB trees implemented in
  JavaScript for use in the Elm programming language.
* Tiark Rompf and Phil Bagwell's original implementation code?  Did
  this become what was used in Scala?  Was there an earlier version
  used in first research paper results?

Published papers and theses:

* Phil Bagwell, Tiark Rompf, "RRB-Trees: Efficient Immutable Vectors",
  EPFL-REPORT-169879, September, 2011
  [PDF](http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.592.5377&rep=rep1&type=pdf)
  [SemanticScholar
  page](https://www.semanticscholar.org/paper/RRB-Trees-%3A-Efficient-Immutable-Vectors-Phil-Tiark-Bagwell-Rompf/30c8c562f6421ab6b00d0b7faebd897c407de69c)
  * Phil Bagwell talk
    [video](https://www.youtube.com/watch?v=K2NYwP90bNs), "Striving to
    Make Things Simple and Fast", January 2013, given at Clojure conj
    conference
* Jean Niklas L'orange, "Improving RRB-Tree Performance through
  Transience", MAster Thesis, 2014,
  [PDF](https://hypirion.com/thesis.pdf)
* "RRB Vector: A Practical General Purpose Immutable Sequence",
  Nicolas Stucki, Tiark, Rompf, Vlad Ureche, Phil Bagwell, Proc. of
  the 20th ACM SIGPLAN International Conference on Functional
  Programming, 2015 [ACM digital library
  link](http://dx.doi.org/10.1145/2784731.2784739)
  [PDF](https://github.com/nicolasstucki/scala-rrb-vector/blob/master/documents/RRB%20Vector%20-%20A%20Practical%20General%20Purpose%20Immutable%20Sequence.pdf)
* Nicolas Stucki, "Turning Relaxed Radix Balanced Vector from Theory
  into Practice for Scala Collections", Master Thesis, 2015
  [PFD](https://github.com/nicolasstucki/scala-rrb-vector/blob/master/documents/Master%20Thesis%20-%20Nicolas%20Stucki%20-%20Turning%20Relaxed%20Radix%20Balanced%20Vector%20from%20Theory%20into%20Practice%20for%20Scala%20Collections.pdf?raw=true)
* Juan Pedro Bolivar Puente, "Persistence for the Masses: RRB-Vectors
  in a Systems Language", Proc. ACM Program. Lang. 1, ICFP, Article 16
  (September 2017), https://doi.org/10.1145/3110260
  [PDF](https://public.sinusoid.es/misc/immer/immer-icfp17.pdf)
  * Juan's talk [video](https://www.youtube.com/watch?v=sPhpelUfu8Q)
    "Postmodern immutable data structures" at CppCon 2017

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
