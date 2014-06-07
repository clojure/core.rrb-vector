{:namespaces
 ({:source-url
   "https://github.com/clojure/core.rrb-vector/blob/a869c9dcc48be3894ee14ba23a7b4e32a7658a54/src/main/clojure/clojure/core/rrb_vector.clj",
   :wiki-url
   "http://clojure.github.com/core.rrb-vector/clojure.core.rrb-vector-api.html",
   :name "clojure.core.rrb-vector",
   :author "Michał Marczyk",
   :doc
   "An implementation of the confluently persistent vector data\nstructure introduced in Bagwell, Rompf, \"RRB-Trees: Efficient\nImmutable Vectors\", EPFL-REPORT-169879, September, 2011.\n\nRRB-Trees build upon Clojure's PersistentVectors, adding logarithmic\ntime concatenation and slicing.\n\nThe main API entry points are clojure.core.rrb-vector/catvec,\nperforming vector concatenation, and clojure.core.rrb-vector/subvec,\nwhich produces a new vector containing the appropriate subrange of\nthe input vector (in contrast to clojure.core/subvec, which returns\na view on the input vector).\n\ncore.rrb-vector's vectors can store objects or unboxed primitives.\nThe implementation allows for seamless interoperability with\nclojure.lang.PersistentVector, clojure.core.Vec (more commonly known\nas gvec) and clojure.lang.APersistentVector$SubVector instances:\nclojure.core.rrb-vector/catvec and clojure.core.rrb-vector/subvec\nconvert their inputs to clojure.core.rrb-vector.rrbt.Vector\ninstances whenever necessary (this is a very fast constant time\noperation for PersistentVector and gvec; for SubVector it is O(log\nn), where n is the size of the underlying vector).\n\nclojure.core.rrb-vector also exports its own versions of vector and\nvector-of and vec which always produce\nclojure.core.rrb-vector.rrbt.Vector instances. Note that vector-of\naccepts :object as one of the possible type arguments, in addition\nto keywords naming primitive types."}
  {:source-url
   "https://github.com/clojure/core.rrb-vector/blob/863c72b7c056db7b99317d53feacf791a0e2b45b/src/main/clojure/clojure/core/rrb_vector/rrbt.clj",
   :wiki-url
   "http://clojure.github.com/core.rrb-vector/clojure.core.rrb-vector.rrbt-api.html",
   :name "clojure.core.rrb-vector.rrbt",
   :doc nil}),
 :vars
 ({:arglists
   ([] [v1] [v1 v2] [v1 v2 v3] [v1 v2 v3 v4] [v1 v2 v3 v4 & vn]),
   :name "catvec",
   :namespace "clojure.core.rrb-vector",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/a869c9dcc48be3894ee14ba23a7b4e32a7658a54/src/main/clojure/clojure/core/rrb_vector.clj#L45",
   :raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/a869c9dcc48be3894ee14ba23a7b4e32a7658a54/src/main/clojure/clojure/core/rrb_vector.clj",
   :wiki-url
   "http://clojure.github.com/core.rrb-vector//clojure.core.rrb-vector-api.html#clojure.core.rrb-vector/catvec",
   :doc "Concatenates the given vectors in logarithmic time.",
   :var-type "function",
   :line 45,
   :file "src/main/clojure/clojure/core/rrb_vector.clj"}
  {:arglists ([v start] [v start end]),
   :name "subvec",
   :namespace "clojure.core.rrb-vector",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/a869c9dcc48be3894ee14ba23a7b4e32a7658a54/src/main/clojure/clojure/core/rrb_vector.clj#L61",
   :raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/a869c9dcc48be3894ee14ba23a7b4e32a7658a54/src/main/clojure/clojure/core/rrb_vector.clj",
   :wiki-url
   "http://clojure.github.com/core.rrb-vector//clojure.core.rrb-vector-api.html#clojure.core.rrb-vector/subvec",
   :doc
   "Returns a new vector containing the elements of the given vector v\nlying between the start (inclusive) and end (exclusive) indices in\nlogarithmic time. end defaults to end of vector. The resulting\nvector shares structure with the original, but does not hold on to\nany elements of the original vector lying outside the given index\nrange.",
   :var-type "function",
   :line 61,
   :file "src/main/clojure/clojure/core/rrb_vector.clj"}
  {:arglists ([coll]),
   :name "vec",
   :namespace "clojure.core.rrb-vector",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/a869c9dcc48be3894ee14ba23a7b4e32a7658a54/src/main/clojure/clojure/core/rrb_vector.clj#L104",
   :raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/a869c9dcc48be3894ee14ba23a7b4e32a7658a54/src/main/clojure/clojure/core/rrb_vector.clj",
   :wiki-url
   "http://clojure.github.com/core.rrb-vector//clojure.core.rrb-vector-api.html#clojure.core.rrb-vector/vec",
   :doc
   "Returns a vector containing the contents of coll.\n\nIf coll is a vector, returns an RRB vector using the internal tree\nof coll.",
   :var-type "function",
   :line 104,
   :file "src/main/clojure/clojure/core/rrb_vector.clj"}
  {:arglists
   ([] [x1] [x1 x2] [x1 x2 x3] [x1 x2 x3 x4] [x1 x2 x3 x4 & xn]),
   :name "vector",
   :namespace "clojure.core.rrb-vector",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/a869c9dcc48be3894ee14ba23a7b4e32a7658a54/src/main/clojure/clojure/core/rrb_vector.clj#L84",
   :raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/a869c9dcc48be3894ee14ba23a7b4e32a7658a54/src/main/clojure/clojure/core/rrb_vector.clj",
   :wiki-url
   "http://clojure.github.com/core.rrb-vector//clojure.core.rrb-vector-api.html#clojure.core.rrb-vector/vector",
   :doc "Creates a new vector containing the args.",
   :var-type "function",
   :line 84,
   :file "src/main/clojure/clojure/core/rrb_vector.clj"}
  {:arglists
   ([t]
    [t x1]
    [t x1 x2]
    [t x1 x2 x3]
    [t x1 x2 x3 x4]
    [t x1 x2 x3 x4 & xn]),
   :name "vector-of",
   :namespace "clojure.core.rrb-vector",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/a869c9dcc48be3894ee14ba23a7b4e32a7658a54/src/main/clojure/clojure/core/rrb_vector.clj#L130",
   :raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/a869c9dcc48be3894ee14ba23a7b4e32a7658a54/src/main/clojure/clojure/core/rrb_vector.clj",
   :wiki-url
   "http://clojure.github.com/core.rrb-vector//clojure.core.rrb-vector-api.html#clojure.core.rrb-vector/vector-of",
   :doc
   "Creates a new vector capable of storing homogenous items of type t,\nwhich should be one of :object, :int, :long, :float, :double, :byte,\n:short, :char, :boolean. Primitives are stored unboxed.\n\nOptionally takes one or more elements to populate the vector.",
   :var-type "function",
   :line 130,
   :file "src/main/clojure/clojure/core/rrb_vector.clj"}
  {:arglists ([nm am objects? cnt shift root tail tidx]),
   :name "->Transient",
   :namespace "clojure.core.rrb-vector.rrbt",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/863c72b7c056db7b99317d53feacf791a0e2b45b/src/main/clojure/clojure/core/rrb_vector/rrbt.clj#L1471",
   :raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/863c72b7c056db7b99317d53feacf791a0e2b45b/src/main/clojure/clojure/core/rrb_vector/rrbt.clj",
   :wiki-url
   "http://clojure.github.com/core.rrb-vector//clojure.core.rrb-vector-api.html#clojure.core.rrb-vector.rrbt/->Transient",
   :doc
   "Positional factory function for class clojure.core.rrb_vector.rrbt.Transient.",
   :var-type "function",
   :line 1471,
   :file "src/main/clojure/clojure/core/rrb_vector/rrbt.clj"}
  {:arglists ([am vec anode i offset _hash _hasheq]),
   :name "->VecSeq",
   :namespace "clojure.core.rrb-vector.rrbt",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/863c72b7c056db7b99317d53feacf791a0e2b45b/src/main/clojure/clojure/core/rrb_vector/rrbt.clj#L69",
   :raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/863c72b7c056db7b99317d53feacf791a0e2b45b/src/main/clojure/clojure/core/rrb_vector/rrbt.clj",
   :wiki-url
   "http://clojure.github.com/core.rrb-vector//clojure.core.rrb-vector-api.html#clojure.core.rrb-vector.rrbt/->VecSeq",
   :doc
   "Positional factory function for class clojure.core.rrb_vector.rrbt.VecSeq.",
   :var-type "function",
   :line 69,
   :file "src/main/clojure/clojure/core/rrb_vector/rrbt.clj"}
  {:arglists ([nm am cnt shift root tail _meta _hash _hasheq]),
   :name "->Vector",
   :namespace "clojure.core.rrb-vector.rrbt",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/863c72b7c056db7b99317d53feacf791a0e2b45b/src/main/clojure/clojure/core/rrb_vector/rrbt.clj#L338",
   :raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/863c72b7c056db7b99317d53feacf791a0e2b45b/src/main/clojure/clojure/core/rrb_vector/rrbt.clj",
   :wiki-url
   "http://clojure.github.com/core.rrb-vector//clojure.core.rrb-vector-api.html#clojure.core.rrb-vector.rrbt/->Vector",
   :doc
   "Positional factory function for class clojure.core.rrb_vector.rrbt.Vector.",
   :var-type "function",
   :line 338,
   :file "src/main/clojure/clojure/core/rrb_vector/rrbt.clj"}
  {:file nil,
   :raw-source-url nil,
   :source-url nil,
   :wiki-url
   "http://clojure.github.com/core.rrb-vector//clojure.core.rrb-vector-api.html#clojure.core.rrb-vector.rrbt/Transient",
   :namespace "clojure.core.rrb-vector.rrbt",
   :var-type "type",
   :name "Transient"}
  {:file nil,
   :raw-source-url nil,
   :source-url nil,
   :wiki-url
   "http://clojure.github.com/core.rrb-vector//clojure.core.rrb-vector-api.html#clojure.core.rrb-vector.rrbt/VecSeq",
   :namespace "clojure.core.rrb-vector.rrbt",
   :var-type "type",
   :name "VecSeq"}
  {:file nil,
   :raw-source-url nil,
   :source-url nil,
   :wiki-url
   "http://clojure.github.com/core.rrb-vector//clojure.core.rrb-vector-api.html#clojure.core.rrb-vector.rrbt/Vector",
   :namespace "clojure.core.rrb-vector.rrbt",
   :var-type "type",
   :name "Vector"})}
