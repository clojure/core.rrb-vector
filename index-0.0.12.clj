{:namespaces
 ({:source-url
   "https://github.com/clojure/core.rrb-vector/blob/a869c9dcc48be3894ee14ba23a7b4e32a7658a54/src/main/clojure/clojure/core/rrb_vector.clj",
   :wiki-url
   "http://clojure.github.com/core.rrb-vector/clojure.core.rrb-vector-api.html",
   :name "clojure.core.rrb-vector",
   :author "Michał Marczyk",
   :doc
   "An implementation of the confluently persistent vector data\nstructure introduced in Bagwell, Rompf, \"RRB-Trees: Efficient\nImmutable Vectors\", EPFL-REPORT-169879, September, 2011.\n\nRRB-Trees build upon Clojure's PersistentVectors, adding logarithmic\ntime concatenation and slicing.\n\nThe main API entry points are clojure.core.rrb-vector/catvec,\nperforming vector concatenation, and clojure.core.rrb-vector/subvec,\nwhich produces a new vector containing the appropriate subrange of\nthe input vector (in contrast to clojure.core/subvec, which returns\na view on the input vector).\n\ncore.rrb-vector's vectors can store objects or unboxed primitives.\nThe implementation allows for seamless interoperability with\nclojure.lang.PersistentVector, clojure.core.Vec (more commonly known\nas gvec) and clojure.lang.APersistentVector$SubVector instances:\nclojure.core.rrb-vector/catvec and clojure.core.rrb-vector/subvec\nconvert their inputs to clojure.core.rrb-vector.rrbt.Vector\ninstances whenever necessary (this is a very fast constant time\noperation for PersistentVector and gvec; for SubVector it is O(log\nn), where n is the size of the underlying vector).\n\nclojure.core.rrb-vector also exports its own versions of vector and\nvector-of and vec which always produce\nclojure.core.rrb-vector.rrbt.Vector instances. Note that vector-of\naccepts :object as one of the possible type arguments, in addition\nto keywords naming primitive types."}),
 :vars ()}
