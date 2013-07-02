(ns cljs.core.rrb-vector

  "An implementation of the confluently persistent vector data
  structure introduced in Bagwell, Rompf, \"RRB-Trees: Efficient
  Immutable Vectors\", EPFL-REPORT-169879, September, 2011.

  RRB-Trees build upon Clojure's PersistentVectors, adding logarithmic
  time concatenation and slicing.

  The main API entry points are cljs.core.rrb-vector/catvec,
  performing vector concatenation, and cljs.core.rrb-vector/subvec,
  which produces a new vector containing the appropriate subrange of
  the input vector (in contrast to cljs.core/subvec, which returns
  a view on the input vector).

  core.rrb-vector's vectors can store objects or unboxed primitives.
  The implementation allows for seamless interoperability with
  clojure.lang.PersistentVector, cljs.core.Vec (more commonly known
  as gvec) and clojure.lang.APersistentVector$SubVector instances:
  cljs.core.rrb-vector/catvec and cljs.core.rrb-vector/subvec
  convert their inputs to cljs.core.rrb-vector.rrbt.Vector
  instances whenever necessary (this is a very fast constant time
  operation for PersistentVector and gvec; for SubVector it is O(log
  n), where n is the size of the underlying vector).

  cljs.core.rrb-vector also exports its own versions of vector and
  vector-of and vec which always produce
  cljs.core.rrb-vector.rrbt.Vector instances. Note that vector-of
  accepts :object as one of the possible type arguments, in addition
  to keywords naming primitive types."

  {:author "Michał Marczyk"}

  (:refer-clojure :exclude [vector vec subvec])
  (:require [cljs.core.rrb-vector.protocols :refer [-slicev -splicev]]
            cljs.core.rrb-vector.rrbt
            cljs.core.rrb-vector.interop)
  (:require-macros [cljs.core.rrb-vector.macros :refer [gen-vector-method]]))

(defn catvec
  "Concatenates the given vectors in logarithmic time."
  ([]
     [])
  ([v1]
     v1)
  ([v1 v2]
     (-splicev v1 v2))
  ([v1 v2 v3]
     (-splicev (-splicev v1 v2) v3))
  ([v1 v2 v3 v4]
     (-splicev (-splicev v1 v2) (-splicev v3 v4)))
  ([v1 v2 v3 v4 & vn]
     (-splicev (-splicev (-splicev v1 v2) (-splicev v3 v4))
              (apply catvec vn))))

(defn subvec
  "Returns a new vector containing the elements of the given vector v
  lying between the start (inclusive) and end (exclusive) indices in
  logarithmic time. end defaults to end of vector. The resulting
  vector shares structure with the original, but does not hold on to
  any elements of the original vector lying outside the given index
  range."
  ([v start]
     (-slicev v start (count v)))
  ([v start end]
     (-slicev v start end)))

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
         (recur (-conj ^not-native v (first xn))
                (next xn))
         v))))

(defn vec
  "Returns a new vector containing the contents of coll."
  [coll]
  (apply vector coll))
