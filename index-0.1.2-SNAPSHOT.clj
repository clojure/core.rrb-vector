{:namespaces
 ({:doc
   "An implementation of the confluently persistent vector data\nstructure introduced in Bagwell, Rompf, \"RRB-Trees: Efficient\nImmutable Vectors\", EPFL-REPORT-169879, September, 2011.\n\nRRB-Trees build upon Clojure's PersistentVectors, adding logarithmic\ntime concatenation and slicing.\n\nThe main API entry points are clojure.core.rrb-vector/catvec,\nperforming vector concatenation, and clojure.core.rrb-vector/subvec,\nwhich produces a new vector containing the appropriate subrange of\nthe input vector (in contrast to clojure.core/subvec, which returns\na view on the input vector).\n\ncore.rrb-vector's vectors can store objects or unboxed primitives.\nThe implementation allows for seamless interoperability with\nclojure.lang.PersistentVector, clojure.core.Vec (more commonly known\nas gvec) and clojure.lang.APersistentVector$SubVector instances:\nclojure.core.rrb-vector/catvec and clojure.core.rrb-vector/subvec\nconvert their inputs to clojure.core.rrb-vector.rrbt.Vector\ninstances whenever necessary (this is a very fast constant time\noperation for PersistentVector and gvec; for SubVector it is O(log\nn), where n is the size of the underlying vector).\n\nclojure.core.rrb-vector also exports its own versions of vector and\nvector-of and vec which always produce\nclojure.core.rrb-vector.rrbt.Vector instances. Note that vector-of\naccepts :object as one of the possible type arguments, in addition\nto keywords naming primitive types.",
   :author "Michał Marczyk",
   :name "clojure.core.rrb-vector",
   :wiki-url "https://clojure.github.io/core.rrb-vector/index.html",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/1cfe5a755bdad3c169a2e4538d04d4db1764d9a2/src/main/clojure/clojure/core/rrb_vector.clj"}
  {:doc nil,
   :name "clojure.core.rrb-vector.debug",
   :wiki-url
   "https://clojure.github.io/core.rrb-vector/index.html#clojure.core.rrb-vector.debug",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj"}
  {:doc nil,
   :name "clojure.core.rrb-vector.rrbt",
   :wiki-url
   "https://clojure.github.io/core.rrb-vector/index.html#clojure.core.rrb-vector.rrbt",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/1cfe5a755bdad3c169a2e4538d04d4db1764d9a2/src/main/clojure/clojure/core/rrb_vector/rrbt.clj"}),
 :vars
 ({:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/1cfe5a755bdad3c169a2e4538d04d4db1764d9a2/src/main/clojure/clojure/core/rrb_vector.clj",
   :name "catvec",
   :file "src/main/clojure/clojure/core/rrb_vector.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/1cfe5a755bdad3c169a2e4538d04d4db1764d9a2/src/main/clojure/clojure/core/rrb_vector.clj#L49",
   :line 49,
   :var-type "function",
   :arglists
   ([] [v1] [v1 v2] [v1 v2 v3] [v1 v2 v3 v4] [v1 v2 v3 v4 & vn]),
   :doc "Concatenates the given vectors in logarithmic time.",
   :namespace "clojure.core.rrb-vector",
   :wiki-url
   "https://clojure.github.io/core.rrb-vector//index.html#clojure.core.rrb-vector/catvec"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/1cfe5a755bdad3c169a2e4538d04d4db1764d9a2/src/main/clojure/clojure/core/rrb_vector.clj",
   :name "subvec",
   :file "src/main/clojure/clojure/core/rrb_vector.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/1cfe5a755bdad3c169a2e4538d04d4db1764d9a2/src/main/clojure/clojure/core/rrb_vector.clj#L65",
   :line 65,
   :var-type "function",
   :arglists ([v start] [v start end]),
   :doc
   "Returns a new vector containing the elements of the given vector v\nlying between the start (inclusive) and end (exclusive) indices in\nlogarithmic time. end defaults to end of vector. The resulting\nvector shares structure with the original, but does not hold on to\nany elements of the original vector lying outside the given index\nrange.",
   :namespace "clojure.core.rrb-vector",
   :wiki-url
   "https://clojure.github.io/core.rrb-vector//index.html#clojure.core.rrb-vector/subvec"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/1cfe5a755bdad3c169a2e4538d04d4db1764d9a2/src/main/clojure/clojure/core/rrb_vector.clj",
   :name "vec",
   :file "src/main/clojure/clojure/core/rrb_vector.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/1cfe5a755bdad3c169a2e4538d04d4db1764d9a2/src/main/clojure/clojure/core/rrb_vector.clj#L106",
   :line 106,
   :var-type "function",
   :arglists ([coll]),
   :doc
   "Returns a vector containing the contents of coll.\n\nIf coll is a vector, returns an RRB vector using the internal tree\nof coll.",
   :namespace "clojure.core.rrb-vector",
   :wiki-url
   "https://clojure.github.io/core.rrb-vector//index.html#clojure.core.rrb-vector/vec"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/1cfe5a755bdad3c169a2e4538d04d4db1764d9a2/src/main/clojure/clojure/core/rrb_vector.clj",
   :name "vector",
   :file "src/main/clojure/clojure/core/rrb_vector.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/1cfe5a755bdad3c169a2e4538d04d4db1764d9a2/src/main/clojure/clojure/core/rrb_vector.clj#L86",
   :line 86,
   :var-type "function",
   :arglists
   ([] [x1] [x1 x2] [x1 x2 x3] [x1 x2 x3 x4] [x1 x2 x3 x4 & xn]),
   :doc "Creates a new vector containing the args.",
   :namespace "clojure.core.rrb-vector",
   :wiki-url
   "https://clojure.github.io/core.rrb-vector//index.html#clojure.core.rrb-vector/vector"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/1cfe5a755bdad3c169a2e4538d04d4db1764d9a2/src/main/clojure/clojure/core/rrb_vector.clj",
   :name "vector-of",
   :file "src/main/clojure/clojure/core/rrb_vector.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/1cfe5a755bdad3c169a2e4538d04d4db1764d9a2/src/main/clojure/clojure/core/rrb_vector.clj#L130",
   :line 130,
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
   "https://clojure.github.io/core.rrb-vector//index.html#clojure.core.rrb-vector/vector-of"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :name "check-catvec",
   :file "src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj#L1303",
   :line 1303,
   :var-type "function",
   :arglists ([extra-checks? & counts]),
   :doc
   "Perform a sequence of calls to catvec or checking-catvec on one or\nmore core.rrb-vector vectors.  Return true if Clojure's built-in\nconcat function give the same results, otherwise false.  Intended\nfor use in tests of this library.",
   :namespace "clojure.core.rrb-vector.debug",
   :wiki-url
   "https://clojure.github.io/core.rrb-vector//index.html#clojure.core.rrb-vector.debug/check-catvec"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :name "check-subvec",
   :file "src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj#L1282",
   :line 1282,
   :var-type "function",
   :arglists ([extra-checks? init & starts-and-ends]),
   :doc
   "Perform a sequence of calls to subvec an a core.rrb-vector vector,\nas well as a normal Clojure vector, returning true if they give the\nsame results, otherwise false.  Intended for use in tests of this\nlibrary.",
   :namespace "clojure.core.rrb-vector.debug",
   :wiki-url
   "https://clojure.github.io/core.rrb-vector//index.html#clojure.core.rrb-vector.debug/check-subvec"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :name "checking-catvec",
   :file "src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj#L1187",
   :line 1187,
   :var-type "function",
   :arglists ([& args]),
   :doc
   "checking-catvec is similar to checking-pop, with the\ndifferences summarized below.  See checking-pop documentation for\ndetails.\n\nNote that (get @d/debug-otps :catvec) is used to control tracing,\nvalidating, and return value sanity checks for checking-catvec as a\nwhole.  This includes controlling those options for the function\nchecking-splice-rrbts, which is used to concatenate pairs of vectors\nwhen you call checking-catvec with 3 or more vectors.  This takes a\nbit longer to do the checking on every concatenation, but catches\nproblems closer to the time they are introduced.\n\n    opts map: (get @d/debug-opts :catvec)\n    function called if (:validating opts) is logical true:\n        validating-catvec",
   :namespace "clojure.core.rrb-vector.debug",
   :wiki-url
   "https://clojure.github.io/core.rrb-vector//index.html#clojure.core.rrb-vector.debug/checking-catvec"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :name "checking-catvec-impl",
   :file "src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj#L1144",
   :line 1144,
   :var-type "function",
   :arglists
   ([] [v1] [v1 v2] [v1 v2 v3] [v1 v2 v3 v4] [v1 v2 v3 v4 & vn]),
   :doc
   "checking-catvec-impl is identical to catvec, except that it calls\nchecking-splicev instead of splicev, for configurable additional\nchecking on each call to checking-splicev.",
   :namespace "clojure.core.rrb-vector.debug",
   :wiki-url
   "https://clojure.github.io/core.rrb-vector//index.html#clojure.core.rrb-vector.debug/checking-catvec-impl"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :name "checking-pop",
   :file "src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj#L886",
   :line 886,
   :var-type "function",
   :arglists ([coll]),
   :doc
   "These two namespace aliases will be used later in this\ndocumentation:\n\n    (require '[clojure.core.rrb-vector.debug :as d])\n    (require '[clojure.core.rrb-vector.debug-platform-dependent :as pd])\n\nchecking-pop passes its argument to clojure.core/pop, and if it\nreturns, it returns whatever clojure.core/pop does.  If checking-pop\ndetects any problems, it will record information about the problems\nfound in one or both of the global atoms 'd/failure-data' and\n'd/warning-data', and optionally throw an exception.\n\nIf coll is not a vector type according to pd/is-vector?, then\nchecking-pop simply behaves exactly like clojure.core/pop, with no\nadditional checks performed.  All of checking-pop's extra checks are\nspecific to vectors.\n\nIf coll is a vector, then checking-pop looks up the key :pop in a\nglobal atom 'd/debug-opts'.  The result of that lookup is a map we\nwill call 'opts' below.\n\n    opts map: (get @d/debug-opts :pop)\n    function called if (:validating opts) is logical true:\n        validating-pop\n\nIf (:trace opts) is true, then a debug trace message is printed to\n*out*.\n\nIf (:validate opts) is true, then validating-pop is called, using\nclojure.core/pop to do the real work, but validating-pop will check\nwhether the return value looks correct relative to the input\nparameter value, i.e. it is equal to a sequence of values containing\nall but the last element of the input coll's sequence of values.\nSee validating-pop documentation for additional details.  This step\nrecords details of problems found in the atoms d/failure-data.\n\n(:return-value-checks opts) should be a sequence of functions that\neach take the vector returned from calling clojure.core/pop, and\nreturn data about any errors or warnings they find in the internals\nof the vector data structure.  Errors or warnings are appended to\natoms d/failure-data and/or d/warning-data.\n\nIf either the validate or return value checks steps find an error,\nthey throw an exception if (:continue-on-error opts) is logical\nfalse.\n\nIf the return value checks step finds no error, but does find a\nwarning, it throws an exception if (:continue-on-warning opts) is\nlogical false.",
   :namespace "clojure.core.rrb-vector.debug",
   :wiki-url
   "https://clojure.github.io/core.rrb-vector//index.html#clojure.core.rrb-vector.debug/checking-pop"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :name "checking-pop!",
   :file "src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj#L973",
   :line 973,
   :var-type "function",
   :arglists ([coll]),
   :doc
   "checking-pop! is similar to checking-pop, with the differences\nsummarized below.  See checking-pop documentation for details.\n\n    opts map: (get @d/debug-opts :pop!)\n    function called if (:validating opts) is logical true:\n        validating-pop!",
   :namespace "clojure.core.rrb-vector.debug",
   :wiki-url
   "https://clojure.github.io/core.rrb-vector//index.html#clojure.core.rrb-vector.debug/checking-pop!"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :name "checking-slicev",
   :file "src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj#L1244",
   :line 1244,
   :var-type "function",
   :arglists ([& args]),
   :doc
   "checking-slicev is similar to checking-pop, with the differences\nsummarized below.  See checking-pop documentation for details.\n\nUnlike checking-pop, it seems unlikely that a user of\ncore.rrb-vector would want to call this function directly.  See\nchecking-subvec.  checking-slicev is part of the implementation of\nchecking-subvec.\n\n    opts map: (get @d/debug-opts :subvec)  ;; _not_ :slicev\n    function called if (:validating opts) is logical true:\n        validating-slicev",
   :namespace "clojure.core.rrb-vector.debug",
   :wiki-url
   "https://clojure.github.io/core.rrb-vector//index.html#clojure.core.rrb-vector.debug/checking-slicev"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :name "checking-splice-rrbts",
   :file "src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj#L1096",
   :line 1096,
   :var-type "function",
   :arglists ([& args]),
   :doc
   "checking-splice-rrbts is similar to checking-pop, with the\ndifferences summarized below.  See checking-pop documentation for\ndetails.\n\nUnlike checking-pop, it seems unlikely that a user of\ncore.rrb-vector would want to call this function directly.  See\nchecking-catvec.  checking-splice-rrbts is part of the\nimplementation of checking-catvec.\n\n    opts map: (get @d/debug-opts :catvec)  ;; _not_ :splice-rrbts\n    function called if (:validating opts) is logical true:\n        validating-splice-rrbts",
   :namespace "clojure.core.rrb-vector.debug",
   :wiki-url
   "https://clojure.github.io/core.rrb-vector//index.html#clojure.core.rrb-vector.debug/checking-splice-rrbts"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :name "checking-splice-rrbts-main",
   :file "src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj#L1068",
   :line 1068,
   :var-type "function",
   :arglists ([& args]),
   :doc
   "checking-splice-rrbts-main is similar to checking-pop, with the\ndifferences summarized below.  See checking-pop documentation for\ndetails.\n\nUnlike checking-pop, it seems unlikely that a user of\ncore.rrb-vector would want to call this function directly.  See\nchecking-catvec.  checking-splice-rrbts-main is part of the\nimplementation of checking-catvec.\n\n    opts map: (get @d/debug-opts :catvec)  ;; _not_ :splice-rrbts-main\n    function called if (:validating opts) is logical true:\n        validating-splice-rrbts-main",
   :namespace "clojure.core.rrb-vector.debug",
   :wiki-url
   "https://clojure.github.io/core.rrb-vector//index.html#clojure.core.rrb-vector.debug/checking-splice-rrbts-main"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :name "checking-splicev",
   :file "src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj#L1131",
   :line 1131,
   :var-type "function",
   :arglists ([v1 v2]),
   :doc
   "checking-splicev is identical to splicev, except that it calls\nchecking-splice-rrbts instead of splice-rrbts, for configurable\nadditional checking on each call to checking-splice-rrbts.\n\nIt is more likely that a core.rrb-vector library user will want to\ncall checking-catvec rather than this one.  checking-splicev is part\nof the implementation of checking-catvec.",
   :namespace "clojure.core.rrb-vector.debug",
   :wiki-url
   "https://clojure.github.io/core.rrb-vector//index.html#clojure.core.rrb-vector.debug/checking-splicev"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :name "checking-subvec",
   :file "src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj#L1270",
   :line 1270,
   :var-type "function",
   :arglists ([v start] [v start end]),
   :doc
   "checking-subvec is similar to checking-pop, with the differences\nsummarized below.  See checking-pop documentation for details.\n\n    opts map: (get @d/debug-opts :subvec)\n    function called if (:validating opts) is logical true:\n        validating-slicev",
   :namespace "clojure.core.rrb-vector.debug",
   :wiki-url
   "https://clojure.github.io/core.rrb-vector//index.html#clojure.core.rrb-vector.debug/checking-subvec"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :name "checking-transient",
   :file "src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj#L1017",
   :line 1017,
   :var-type "function",
   :arglists ([coll]),
   :doc
   "checking-transient is similar to checking-pop, with the differences\nsummarized below.  See checking-pop documentation for details.\n\n    opts map: (get @d/debug-opts :transient)\n    function called if (:validating opts) is logical true:\n        validating-transient",
   :namespace "clojure.core.rrb-vector.debug",
   :wiki-url
   "https://clojure.github.io/core.rrb-vector//index.html#clojure.core.rrb-vector.debug/checking-transient"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :name "filter-indexes",
   :file "src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj#L84",
   :line 84,
   :var-type "function",
   :arglists ([pred coll]),
   :doc
   "Return a sequence of all indexes of elements e of coll for\nwhich (pred e) returns logical true.  0 is the index of the first\nelement.",
   :namespace "clojure.core.rrb-vector.debug",
   :wiki-url
   "https://clojure.github.io/core.rrb-vector//index.html#clojure.core.rrb-vector.debug/filter-indexes"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :name "first-diff",
   :file "src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj#L191",
   :line 191,
   :var-type "function",
   :arglists ([xs ys]),
   :doc
   "Compare two sequences to see if they have = elements in the same\norder, and both sequences have the same number of elements.  If all\nof those conditions are true, and no exceptions occur while calling\nseq, first, and next on the seqs of xs and ys, then return -1.\n\nIf two elements at the same index in each sequence are found not =\nto each other, or the sequences differ in their number of elements,\nreturn the index, 0 or larger, at which the first difference occurs.\n\nIf an exception occurs while calling seq, first, or next, throw an\nexception that contains the index at which this exception occurred.",
   :namespace "clojure.core.rrb-vector.debug",
   :wiki-url
   "https://clojure.github.io/core.rrb-vector//index.html#clojure.core.rrb-vector.debug/first-diff"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :name "generative-check-catvec",
   :file "src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj#L1343",
   :line 1343,
   :var-type "function",
   :arglists ([extra-checks? iterations max-vcnt min-cnt max-cnt]),
   :doc
   "Perform many calls to check-catvec with randomly generated inputs.\nIntended for use in tests of this library.  Returns true if all\ntests pass, otherwise throws an exception containing data about the\ninputs that caused the failing test.",
   :namespace "clojure.core.rrb-vector.debug",
   :wiki-url
   "https://clojure.github.io/core.rrb-vector//index.html#clojure.core.rrb-vector.debug/generative-check-catvec"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :name "generative-check-subvec",
   :file "src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj#L1316",
   :line 1316,
   :var-type "function",
   :arglists ([extra-checks? iterations max-init-cnt slices]),
   :doc
   "Perform many calls to check-subvec with randomly generated inputs.\nIntended for use in tests of this library.  Returns true if all\ntests pass, otherwise throws an exception containing data about the\ninputs that caused the failing test.",
   :namespace "clojure.core.rrb-vector.debug",
   :wiki-url
   "https://clojure.github.io/core.rrb-vector//index.html#clojure.core.rrb-vector.debug/generative-check-subvec"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :name "objects-in-slot-32-of-obj-arrays",
   :file "src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj#L372",
   :line 372,
   :var-type "function",
   :arglists ([v]),
   :doc
   "Function to look for errors of the form where a node's node.array\nobject, which is often an array of 32 or 33 java.lang.Object's, has\nan element at index 32 that is not nil, and refers to an object that\nis of any type _except_ an array of ints.  There appears to be some\nsituation in which this can occur, but it seems to almost certainly\nbe a bug if that happens, and we should be able to detect it\nwhenever it occurs.",
   :namespace "clojure.core.rrb-vector.debug",
   :wiki-url
   "https://clojure.github.io/core.rrb-vector//index.html#clojure.core.rrb-vector.debug/objects-in-slot-32-of-obj-arrays"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :name "sanity-check-vector-internals",
   :file "src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj#L759",
   :line 759,
   :var-type "function",
   :arglists ([err-desc-str ret args opts]),
   :doc
   "This function is called by all of the checking-* variants of\nfunctions in the debug namespace.  It calls all of the functions\nin (:return-value-checks opts) in the order given, passing each of\nthose functions a return value 'ret'.  Each function performs sanity\nchecks on the 'ret' data structure used to represent the vector.\n\nThose functions should return a map with key :error having a logical\ntrue value if any errors were found, or a key :warning having a\nlogical true value if any warnings were found, otherwise both of\nthose values must be logical false in the returned map (or no such\nkey is present in the returned map at all).\n\nThree examples of such functions are included in core.rrb-vector's\ndebug namespace.\n\n* edit-nodes-errors\n* basic-node-errors\n* ranges-errors\n\nThey each look for different problems in the vector data structure\ninternals.  They were developed as separate functions in case there\nwas ever a significant performance advantage to configuring only\nsome of them to be called, not all of them, for long tests.\n\nIf any errors are found, this function calls record-failure-data, to\nrecord the details in a global atom.  It prints a message to *out*,\nand if (:continue-on-error opts) is logical false, it throws a data\nconveying exception using ex-info containing the same message, and\nthe same error details map passed to record-failure-data.\n\nIf no exception is thrown due to an error, then repeat the same\nchecks for a warning message, recording details via calling\nrecord-warning-data, and throwing an exception\nif (:continue-on-warning opts) is logical false.",
   :namespace "clojure.core.rrb-vector.debug",
   :wiki-url
   "https://clojure.github.io/core.rrb-vector//index.html#clojure.core.rrb-vector.debug/sanity-check-vector-internals"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :name "set-debug-opts!",
   :file "src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj#L718",
   :line 718,
   :var-type "function",
   :arglists ([opts]),
   :doc
   "set-debug-opts! modified the debug-opts atom of the core.rrb-vector\nlibrary, which configures what kinds of extra checks are performed\nwhen calling the checking-* versions of functions defined in the\nlibrary's debug namespace.\n\nExample call:\n\n  (require '[clojure.core.rrb-vector.debug :as d])\n  (d/set-debug-opts! d/full-debug-opts)\n\nThis call enables as thorough of extra verification checks as is\nsupported by existing code, when you call any of the checking-*\nvariants of the functions in this namespace, e.g. checking-catvec,\nchecking-subvec.\n\nIt will also slow down your code to do so.  checking-* functions\nreturn the same values as their non checking-* original functions\nthey are based upon, so you can write application code that mixes\ncalls to both, calling the checking-* versions only occasionally, if\nyou have a long sequence of operations that you want to look for\nbugs within core.rrb-vector's implementation of.",
   :namespace "clojure.core.rrb-vector.debug",
   :wiki-url
   "https://clojure.github.io/core.rrb-vector//index.html#clojure.core.rrb-vector.debug/set-debug-opts!"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :name "validating-catvec",
   :file "src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj#L1162",
   :line 1162,
   :var-type "function",
   :arglists ([err-desc-str & vs]),
   :doc
   "validating-catvec behaves similarly to validating-pop, but note\nthat it does not allow you to pass in a function f on which to\nconcatenate its arguments.  It hardcodes d/checking-catvec-impl for\nthat purpose.  See validating-pop for more details.\n\n    opts map: (get @d/debug-opts :catvec)\n\nIf no exception is thrown, the return value is (apply\nchecking-catvec-impl vs).",
   :namespace "clojure.core.rrb-vector.debug",
   :wiki-url
   "https://clojure.github.io/core.rrb-vector//index.html#clojure.core.rrb-vector.debug/validating-catvec"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :name "validating-pop",
   :file "src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj#L820",
   :line 820,
   :var-type "function",
   :arglists ([f err-desc-str coll]),
   :doc
   "validating-pop is not really designed to be called from user\nprograms.  checking-pop can do everything that validating-pop can,\nand more.  See its documentation.\n\nA typical way of calling validating-pop is:\n\n    (require '[clojure.core.rrb-vector.debug :as d])\n    (d/validating-pop clojure.core/pop \"pop\" coll)\n\nMost of the validating-* functions behave similarly.  This one\ncontains the most complete documentation, and the others refer to\nthis one.  They all differ in the function that they are intended to\nvalidate, and a few other details, which will be collected in one\nplace here for function validating-pop so one can quickly see the\ndifferences between validating-pop and the other validating-*\nfunctions.\n\n    good example f: clojure.core/pop\n    opts map: (get @d/debug-opts :pop)\n\nThe first argument can be any function f.  f is expected to take\narguments and return a value equal to what clojure.core/pop would,\ngiven the argument coll.\n\nvalidating-pop will first make a copy of the seq of items in coll,\nas a safety precaution, because some kinds of incorrect\nimplementations of pop could mutate their input argument.  That\nwould be a bug, of course, but aiding a developer in detecting bugs\nis the reason validating-pop exists.  It uses the function\ncopying-seq to do this, which takes at least linear time in the size\nof coll.\n\nIt will then calculate a sequence that is = to the expected return\nvalue, e.g. for pop, all items in coll except the last one.\n\nThen validating-pop will call (f coll), then call copying-seq on the\nreturn value.\n\nIf the expected and returned sequences are not =, then a map\ncontaining details about the arguments and actual return value is\ncreated and passed to d/record-failure-data, which appends the map\nto the end of a vector that is the value of an atom named\nd/failure-data.  An exception is thrown if (:continue-on-error opts)\nis logical false, with ex-data equal to this same map of error data.\n\nIf the expected and actual sequences are the same, no state is\nmodified and no exception is thrown.\n\nIf validating-pop does not throw an exception, the return value is\n(f coll).",
   :namespace "clojure.core.rrb-vector.debug",
   :wiki-url
   "https://clojure.github.io/core.rrb-vector//index.html#clojure.core.rrb-vector.debug/validating-pop"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :name "validating-pop!",
   :file "src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj#L950",
   :line 950,
   :var-type "function",
   :arglists ([f err-desc-str coll]),
   :doc
   "validating-pop! behaves the same as validating-pop, with the\ndifferences described here.  See validating-pop for details.\n\n    good example f: clojure.core/pop!\n    opts map: (get @d/debug-opts :pop!)\n\nIf no exception is thrown, the return value is (f coll).",
   :namespace "clojure.core.rrb-vector.debug",
   :wiki-url
   "https://clojure.github.io/core.rrb-vector//index.html#clojure.core.rrb-vector.debug/validating-pop!"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :name "validating-slicev",
   :file "src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj#L1219",
   :line 1219,
   :var-type "function",
   :arglists ([err-desc-str coll start] [err-desc-str coll start end]),
   :doc
   "validating-slicev behaves similarly to validating-pop, but note\nthat it does not allow you to pass in a function f to call.  It\nhardcodes slicev for that purpose.  See validating-pop for more\ndetails.\n\n    opts map: (get @d/debug-opts :subvec)  ;; _not_ :slicev",
   :namespace "clojure.core.rrb-vector.debug",
   :wiki-url
   "https://clojure.github.io/core.rrb-vector//index.html#clojure.core.rrb-vector.debug/validating-slicev"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :name "validating-splice-rrbts-main",
   :file "src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj#L1039",
   :line 1039,
   :var-type "function",
   :arglists ([err-desc-str nm am v1 v2]),
   :doc
   "validating-splice-rrbts-main behaves the same as validating-pop, with\nthe differences described here.  See validating-pop for details.\n\n    good example f: clojure.core.rrb-vector.rrbt/splice-rrbts-main\n    opts map: (get @d/debug-opts :catvec)  ;; _not_ :splice-rrbts-main\n\nGiven that splice-rrbts-main is an internal implementation detail of\nthe core.rrb-vector library, it is expected that it is more likely\nyou would call validating-catvec instead of this function.\n\nIf no exception is thrown, the return value is (f v1 v2).",
   :namespace "clojure.core.rrb-vector.debug",
   :wiki-url
   "https://clojure.github.io/core.rrb-vector//index.html#clojure.core.rrb-vector.debug/validating-splice-rrbts-main"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :name "validating-transient",
   :file "src/main/clojure/clojure/core/rrb_vector/debug.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/8c3bdc03f4d4c73326ac0146310bf472cda4d035/src/main/clojure/clojure/core/rrb_vector/debug.clj#L994",
   :line 994,
   :var-type "function",
   :arglists ([f err-desc-str coll]),
   :doc
   "validating-transient behaves the same as validating-pop, with the\ndifferences described here.  See validating-pop for details.\n\n    good example f: clojure.core/transient\n    opts map: (get @d/debug-opts :transient)\n\nIf no exception is thrown, the return value is (f coll).",
   :namespace "clojure.core.rrb-vector.debug",
   :wiki-url
   "https://clojure.github.io/core.rrb-vector//index.html#clojure.core.rrb-vector.debug/validating-transient"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/1cfe5a755bdad3c169a2e4538d04d4db1764d9a2/src/main/clojure/clojure/core/rrb_vector/rrbt.clj",
   :name "->Transient",
   :file "src/main/clojure/clojure/core/rrb_vector/rrbt.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/1cfe5a755bdad3c169a2e4538d04d4db1764d9a2/src/main/clojure/clojure/core/rrb_vector/rrbt.clj#L1842",
   :line 1842,
   :var-type "function",
   :arglists ([nm am objects? cnt shift root tail tidx]),
   :doc
   "Positional factory function for class clojure.core.rrb_vector.rrbt.Transient.",
   :namespace "clojure.core.rrb-vector.rrbt",
   :wiki-url
   "https://clojure.github.io/core.rrb-vector//index.html#clojure.core.rrb-vector.rrbt/->Transient"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/1cfe5a755bdad3c169a2e4538d04d4db1764d9a2/src/main/clojure/clojure/core/rrb_vector/rrbt.clj",
   :name "->VecSeq",
   :file "src/main/clojure/clojure/core/rrb_vector/rrbt.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/1cfe5a755bdad3c169a2e4538d04d4db1764d9a2/src/main/clojure/clojure/core/rrb_vector/rrbt.clj#L77",
   :line 77,
   :var-type "function",
   :arglists ([am vec anode i offset _meta _hash _hasheq]),
   :doc
   "Positional factory function for class clojure.core.rrb_vector.rrbt.VecSeq.",
   :namespace "clojure.core.rrb-vector.rrbt",
   :wiki-url
   "https://clojure.github.io/core.rrb-vector//index.html#clojure.core.rrb-vector.rrbt/->VecSeq"}
  {:raw-source-url
   "https://github.com/clojure/core.rrb-vector/raw/1cfe5a755bdad3c169a2e4538d04d4db1764d9a2/src/main/clojure/clojure/core/rrb_vector/rrbt.clj",
   :name "->Vector",
   :file "src/main/clojure/clojure/core/rrb_vector/rrbt.clj",
   :source-url
   "https://github.com/clojure/core.rrb-vector/blob/1cfe5a755bdad3c169a2e4538d04d4db1764d9a2/src/main/clojure/clojure/core/rrb_vector/rrbt.clj#L473",
   :line 473,
   :var-type "function",
   :arglists ([nm am cnt shift root tail _meta _hash _hasheq]),
   :doc
   "Positional factory function for class clojure.core.rrb_vector.rrbt.Vector.",
   :namespace "clojure.core.rrb-vector.rrbt",
   :wiki-url
   "https://clojure.github.io/core.rrb-vector//index.html#clojure.core.rrb-vector.rrbt/->Vector"}
  {:name "Transient",
   :var-type "type",
   :namespace "clojure.core.rrb-vector.rrbt",
   :arglists nil,
   :wiki-url
   "https://clojure.github.io/core.rrb-vector//index.html#clojure.core.rrb-vector.rrbt/Transient",
   :source-url nil,
   :raw-source-url nil,
   :file nil}
  {:name "VecSeq",
   :var-type "type",
   :namespace "clojure.core.rrb-vector.rrbt",
   :arglists nil,
   :wiki-url
   "https://clojure.github.io/core.rrb-vector//index.html#clojure.core.rrb-vector.rrbt/VecSeq",
   :source-url nil,
   :raw-source-url nil,
   :file nil}
  {:name "Vector",
   :var-type "type",
   :namespace "clojure.core.rrb-vector.rrbt",
   :arglists nil,
   :wiki-url
   "https://clojure.github.io/core.rrb-vector//index.html#clojure.core.rrb-vector.rrbt/Vector",
   :source-url nil,
   :raw-source-url nil,
   :file nil})}
