(ns flexvec.core

  "An implementation of the confluently persistent vector data
  structure introduced in Bagwell, Rompf, \"RRB-Trees: Efficient
  Immutable Vectors\", EPFL-REPORT-169879, September, 2011.

  RRB-Trees build upon Clojure's PersistentVectors, adding logarithmic
  time concatenation and slicing.

  The main API entry points are flexvec.core/catvec, performing vector
  concatenation, and flexvec.core/subvec, which produces a new vector
  containing the appropriate subrange of the input vector (in contrast
  to clojure.core/subvec, which returns a view on the input vector).

  flexvec's vectors can store objects or unboxed primitives. The
  implementation allows for seamless interoperability with
  clojure.lang.PersistentVector, clojure.core.Vec (more commonly known
  as gvec) and clojure.lang.APersistentVector$SubVector instances:
  flexvec.core/catvec and flexvec.core/subvec convert their inputs to
  flexvec.rrbt.Vector instances whenever necessary (this is a very
  fast constant time operation for PersistentVector and gvec; for
  SubVector it is O(log n), where n is the size of the underlying
  vector).

  flexvec.core also exports its own versions of vector, vector-of and
  vec which always produce flexvec.rrbt.Vector instances. Note that
  vector-of accepts :object as one of the possible type arguments, in
  addition to keywords naming primitive types."

  {:author "Michał Marczyk"}

  (:refer-clojure :exclude [vector vector-of vec subvec])
  (:require [flexvec.protocols :refer [slicev splicev]]
            [flexvec.nodes :refer [ams object-am object-nm primitive-nm
                                   empty-pv-node empty-gvec-node]]
            flexvec.rrbt
            flexvec.interop)
  (:import (flexvec.rrbt Vector)
           (flexvec.nodes NodeManager)
           (clojure.core ArrayManager)))

(defn catvec
  "Concatenates the given vectors in logarithmic time."
  ([]
     [])
  ([v1]
     v1)
  ([v1 v2]
     (splicev v1 v2))
  ([v1 v2 v3]
     (splicev (splicev v1 v2) v3))
  ([v1 v2 v3 v4]
     (splicev (splicev v1 v2) (splicev v3 v4)))
  ([v1 v2 v3 v4 & vn]
     (splicev (splicev (splicev v1 v2) (splicev v3 v4))
              (apply catvec vn))))

(defn subvec
  "Returns a new vector containing the elements of the given vector v
  lying between the start (inclusive) and end (exclusive) indices in
  logarithmic time. end defaults to end of vector."
  ([v start]
     (slicev v start (count v)))
  ([v start end]
     (slicev v start end)))

(defmacro ^:private gen-vector-method [& params]
  (let [arr (with-meta (gensym "arr__") {:tag 'objects})]
    `(let [~arr (object-array ~(count params))]
       ~@(map-indexed (fn [i param]
                        `(aset ~arr ~i ~param))
                      params)
       (Vector. ^NodeManager object-nm ^ArrayManager object-am
                ~(count params) 5 empty-pv-node ~arr nil
                ~(if params -1 1)
                ~(if params -1 1)))))

(defn vector
  "Creates a new vector containing the args."
  ([]
     (gen-vector-method))
  ([x1]
     (gen-vector-method x1))
  ([x1 x2]
     (gen-vector-method x1 x2))
  ([x1 x2 x3]
     (gen-vector-method x1 x2 x3))
  ([x1 x2 x3 x4]
     (gen-vector-method x1 x2 x3 x4))
  ([x1 x2 x3 x4 & xn]
     (loop [v  (vector x1 x2 x3 x4)
            xn xn]
       (if xn
         (recur (.cons ^clojure.lang.IPersistentCollection v (first xn))
                (next xn))
         v))))

(defn vec [coll]
  (apply vector coll))

(defmacro ^:private gen-vector-of-method [t & params]
  (let [am  (gensym "am__")
        nm  (gensym "nm__")
        arr (gensym "arr__")]
    `(let [~am ^ArrayManager (ams ~t)
           ~nm ^NodeManager (if (identical? ~t :object) object-nm primitive-nm)
           ~arr (.array ~am ~(count params))]
       ~@(map-indexed (fn [i param]
                        `(.aset ~am ~arr ~i ~param))
                      params)
       (Vector. ~nm ~am ~(count params) 5
                (if (identical? ~t :object) empty-pv-node empty-gvec-node)
                ~arr nil
                ~(if params -1 1)
                ~(if params -1 1)))))

(defn vector-of
  "Creates a new vector capable of storing homogenous items of type t,
  which should be one of :object, :int, :long, :float, :double, :byte,
  :short, :char, :boolean. Primitives are stored unboxed.

  Optionally takes one or more elements to populate the vector."
  ([t]
     (gen-vector-of-method t))
  ([t x1]
     (gen-vector-of-method t x1))
  ([t x1 x2]
     (gen-vector-of-method t x1 x2))
  ([t x1 x2 x3]
     (gen-vector-of-method t x1 x2 x3))
  ([t x1 x2 x3 x4]
     (gen-vector-of-method t x1 x2 x3 x4))
  ([t x1 x2 x3 x4 & xn]
     (loop [v  (vector-of t x1 x2 x3 x4)
            xn xn]
       (if xn
         (recur (.cons ^clojure.lang.IPersistentCollection v (first xn))
                (next xn))
         v))))
