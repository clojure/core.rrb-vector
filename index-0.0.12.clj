{:namespaces
 ({:doc
   "An implementation of the confluently persistent vector data\nstructure introduced in Bagwell, Rompf, \"RRB-Trees: Efficient\nImmutable Vectors\", EPFL-REPORT-169879, September, 2011.\n\nRRB-Trees build upon Clojure's PersistentVectors, adding logarithmic\ntime concatenation and slicing.\n\nThe main API entry points are clojure.core.rrb-vector/catvec,\nperforming vector concatenation, and clojure.core.rrb-vector/subvec,\nwhich produces a new vector containing the appropriate subrange of\nthe input vector (in contrast to clojure.core/subvec, which returns\na view on the input vector).\n\ncore.rrb-vector's vectors can store objects or unboxed primitives.\nThe implementation allows for seamless interoperability with\nclojure.lang.PersistentVector, clojure.core.Vec (more commonly known\nas gvec) and clojure.lang.APersistentVector$SubVector instances:\nclojure.core.rrb-vector/catvec and clojure.core.rrb-vector/subvec\nconvert their inputs to clojure.core.rrb-vector.rrbt.Vector\ninstances whenever necessary (this is a very fast constant time\noperation for PersistentVector and gvec; for SubVector it is O(log\nn), where n is the size of the underlying vector).\n\nclojure.core.rrb-vector also exports its own versions of vector and\nvector-of and vec which always produce\nclojure.core.rrb-vector.rrbt.Vector instances. Note that vector-of\naccepts :object as one of the possible type arguments, in addition\nto keywords naming primitive types.",
   :author "Michał Marczyk",
   :name "clojure.core.rrb-vector",
   :wiki-url
   "http://clojure.github.com/core.rrb-vector/clojure.core.rrb-vector-api.html",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/4e8584914f7e71c2733412de10ff81558aabeb95/src/main/clojure/clojure/core/rrb_vector.clj"}
  {:doc nil,
   :name "clojure.core.rrb-vector.rrbt",
   :wiki-url
   "http://clojure.github.com/core.rrb-vector/clojure.core.rrb-vector.rrbt-api.html",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/4e8584914f7e71c2733412de10ff81558aabeb95/src/main/clojure/clojure/core/rrb_vector/rrbt.clj"}),
 :vars
 ({:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/4e8584914f7e71c2733412de10ff81558aabeb95/src/main/clojure/clojure/core/rrb_vector.clj",
   :name "catvec",
   :file "src/main/clojure/clojure/core/rrb_vector.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/4e8584914f7e71c2733412de10ff81558aabeb95/src/main/clojure/clojure/core/rrb_vector.clj#L48",
   :line 48,
   :var-type "function",
   :arglists
   ([] [v1] [v1 v2] [v1 v2 v3] [v1 v2 v3 v4] [v1 v2 v3 v4 & vn]),
   :doc "Concatenates the given vectors in logarithmic time.",
   :namespace "clojure.core.rrb-vector",
   :wiki-url
   "http://clojure.github.com/core.rrb-vector//clojure.core.rrb-vector-api.html#clojure.core.rrb-vector/catvec"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/4e8584914f7e71c2733412de10ff81558aabeb95/src/main/clojure/clojure/core/rrb_vector.clj",
   :name "subvec",
   :file "src/main/clojure/clojure/core/rrb_vector.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/4e8584914f7e71c2733412de10ff81558aabeb95/src/main/clojure/clojure/core/rrb_vector.clj#L64",
   :line 64,
   :var-type "function",
   :arglists ([v start] [v start end]),
   :doc
   "Returns a new vector containing the elements of the given vector v\nlying between the start (inclusive) and end (exclusive) indices in\nlogarithmic time. end defaults to end of vector. The resulting\nvector shares structure with the original, but does not hold on to\nany elements of the original vector lying outside the given index\nrange.",
   :namespace "clojure.core.rrb-vector",
   :wiki-url
   "http://clojure.github.com/core.rrb-vector//clojure.core.rrb-vector-api.html#clojure.core.rrb-vector/subvec"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/4e8584914f7e71c2733412de10ff81558aabeb95/src/main/clojure/clojure/core/rrb_vector.clj",
   :name "vec",
   :file "src/main/clojure/clojure/core/rrb_vector.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/4e8584914f7e71c2733412de10ff81558aabeb95/src/main/clojure/clojure/core/rrb_vector.clj#L107",
   :line 107,
   :var-type "function",
   :arglists ([coll]),
   :doc
   "Returns a vector containing the contents of coll.\n\nIf coll is a vector, returns an RRB vector using the internal tree\nof coll.",
   :namespace "clojure.core.rrb-vector",
   :wiki-url
   "http://clojure.github.com/core.rrb-vector//clojure.core.rrb-vector-api.html#clojure.core.rrb-vector/vec"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/4e8584914f7e71c2733412de10ff81558aabeb95/src/main/clojure/clojure/core/rrb_vector.clj",
   :name "vector",
   :file "src/main/clojure/clojure/core/rrb_vector.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/4e8584914f7e71c2733412de10ff81558aabeb95/src/main/clojure/clojure/core/rrb_vector.clj#L87",
   :line 87,
   :var-type "function",
   :arglists
   ([] [x1] [x1 x2] [x1 x2 x3] [x1 x2 x3 x4] [x1 x2 x3 x4 & xn]),
   :doc "Creates a new vector containing the args.",
   :namespace "clojure.core.rrb-vector",
   :wiki-url
   "http://clojure.github.com/core.rrb-vector//clojure.core.rrb-vector-api.html#clojure.core.rrb-vector/vector"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/4e8584914f7e71c2733412de10ff81558aabeb95/src/main/clojure/clojure/core/rrb_vector.clj",
   :name "vector-of",
   :file "src/main/clojure/clojure/core/rrb_vector.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/4e8584914f7e71c2733412de10ff81558aabeb95/src/main/clojure/clojure/core/rrb_vector.clj#L133",
   :line 133,
   :var-type "function",
   :arglists
   ([t]
    [t x1]
    [t x1 x2]
    [t x1 x2 x3]
    [t x1 x2 x3 x4]
    [t x1 x2 x3 x4 & xn]),
   :doc
   "Creates a new vector capable of storing homogenous items of type t,\nwhich should be one of :object, :int, :long, :float, :double, :byte,\n:short, :char, :boolean. Primitives are stored unboxed.\n\nOptionally takes one or more elements to populate the vector.",
   :namespace "clojure.core.rrb-vector",
   :wiki-url
   "http://clojure.github.com/core.rrb-vector//clojure.core.rrb-vector-api.html#clojure.core.rrb-vector/vector-of"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/4e8584914f7e71c2733412de10ff81558aabeb95/src/main/clojure/clojure/core/rrb_vector/rrbt.clj",
   :name "->Transient",
   :file "src/main/clojure/clojure/core/rrb_vector/rrbt.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/4e8584914f7e71c2733412de10ff81558aabeb95/src/main/clojure/clojure/core/rrb_vector/rrbt.clj#L1613",
   :line 1613,
   :var-type "function",
   :arglists ([nm am objects? cnt shift root tail tidx]),
   :doc
   "Positional factory function for class clojure.core.rrb_vector.rrbt.Transient.",
   :namespace "clojure.core.rrb-vector.rrbt",
   :wiki-url
   "http://clojure.github.com/core.rrb-vector//clojure.core.rrb-vector-api.html#clojure.core.rrb-vector.rrbt/->Transient"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/4e8584914f7e71c2733412de10ff81558aabeb95/src/main/clojure/clojure/core/rrb_vector/rrbt.clj",
   :name "->VecSeq",
   :file "src/main/clojure/clojure/core/rrb_vector/rrbt.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/4e8584914f7e71c2733412de10ff81558aabeb95/src/main/clojure/clojure/core/rrb_vector/rrbt.clj#L75",
   :line 75,
   :var-type "function",
   :arglists ([am vec anode i offset _meta _hash _hasheq]),
   :doc
   "Positional factory function for class clojure.core.rrb_vector.rrbt.VecSeq.",
   :namespace "clojure.core.rrb-vector.rrbt",
   :wiki-url
   "http://clojure.github.com/core.rrb-vector//clojure.core.rrb-vector-api.html#clojure.core.rrb-vector.rrbt/->VecSeq"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/4e8584914f7e71c2733412de10ff81558aabeb95/src/main/clojure/clojure/core/rrb_vector/rrbt.clj",
   :name "->Vector",
   :file "src/main/clojure/clojure/core/rrb_vector/rrbt.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/4e8584914f7e71c2733412de10ff81558aabeb95/src/main/clojure/clojure/core/rrb_vector/rrbt.clj#L465",
   :line 465,
   :var-type "function",
   :arglists ([nm am cnt shift root tail _meta _hash _hasheq]),
   :doc
   "Positional factory function for class clojure.core.rrb_vector.rrbt.Vector.",
   :namespace "clojure.core.rrb-vector.rrbt",
   :wiki-url
   "http://clojure.github.com/core.rrb-vector//clojure.core.rrb-vector-api.html#clojure.core.rrb-vector.rrbt/->Vector"}
  {:name "Transient",
   :var-type "type",
   :namespace "clojure.core.rrb-vector.rrbt",
   :arglists nil,
   :wiki-url
   "http://clojure.github.com/core.rrb-vector//clojure.core.rrb-vector-api.html#clojure.core.rrb-vector.rrbt/Transient",
   :source-url nil,
   :raw-source-url nil,
   :file nil}
  {:name "VecSeq",
   :var-type "type",
   :namespace "clojure.core.rrb-vector.rrbt",
   :arglists nil,
   :wiki-url
   "http://clojure.github.com/core.rrb-vector//clojure.core.rrb-vector-api.html#clojure.core.rrb-vector.rrbt/VecSeq",
   :source-url nil,
   :raw-source-url nil,
   :file nil}
  {:name "Vector",
   :var-type "type",
   :namespace "clojure.core.rrb-vector.rrbt",
   :arglists nil,
   :wiki-url
   "http://clojure.github.com/core.rrb-vector//clojure.core.rrb-vector-api.html#clojure.core.rrb-vector.rrbt/Vector",
   :source-url nil,
   :raw-source-url nil,
   :file nil})}
