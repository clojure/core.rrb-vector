(ns clojure.core.rrb-vector

  "An implementation of the confluently persistent vector data
  structure introduced in Bagwell, Rompf, \"RRB-Trees: Efficient
  Immutable Vectors\", EPFL-REPORT-169879, September, 2011.

  RRB-Trees build upon Clojure's PersistentVectors, adding logarithmic
  time concatenation and slicing.

  The main API entry points are clojure.core.rrb-vector/catvec,
  performing vector concatenation, and clojure.core.rrb-vector/subvec,
  which produces a new vector containing the appropriate subrange of
  the input vector (in contrast to clojure.core/subvec, which returns
  a view on the input vector).

  core.rrb-vector's vectors can store objects or unboxed primitives.
  The implementation allows for seamless interoperability with
  clojure.lang.PersistentVector, clojure.core.Vec (more commonly known
  as gvec) and clojure.lang.APersistentVector$SubVector instances:
  clojure.core.rrb-vector/catvec and clojure.core.rrb-vector/subvec
  convert their inputs to clojure.core.rrb-vector.rrbt.Vector
  instances whenever necessary (this is a very fast constant time
  operation for PersistentVector and gvec; for SubVector it is O(log
  n), where n is the size of the underlying vector).

  clojure.core.rrb-vector also exports its own versions of vector and
  vector-of and vec which always produce
  clojure.core.rrb-vector.rrbt.Vector instances. Note that vector-of
  accepts :object as one of the possible type arguments, in addition
  to keywords naming primitive types."

  {:author "Michał Marczyk"}

  (:refer-clojure :exclude [vector vector-of vec subvec])
  (:require [clojure.core.rrb-vector.parameters :as p]
            [clojure.core.rrb-vector.protocols :refer [slicev splicev]]
            [clojure.core.rrb-vector.nodes
             :refer [ams object-am object-nm primitive-nm
                     empty-pv-node empty-gvec-node]]
            [clojure.core.rrb-vector.rrbt :refer [as-rrbt]]
            clojure.core.rrb-vector.interop)
  (:import (clojure.core.rrb_vector.rrbt Vector)
           (clojure.core.rrb_vector.nodes NodeManager)
           (clojure.core ArrayManager)))

(set! *warn-on-reflection* true)
(set! *unchecked-math* true) ;; :warn-on-boxed

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
  logarithmic time. end defaults to end of vector. The resulting
  vector shares structure with the original, but does not hold on to
  any elements of the original vector lying outside the given index
  range."
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
                ~(count params) 5 empty-pv-node ~arr nil 0 0))))

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
     (loop [v  (transient (vector x1 x2 x3 x4))
            xn xn]
       (if xn
         (recur (.conj ^clojure.lang.ITransientCollection v (first xn))
                (next xn))
         (persistent! v)))))

(defn vec
  "Returns a vector containing the contents of coll.

  If coll is a vector, returns an RRB vector using the internal tree
  of coll."
  [coll]
  (if (vector? coll)
    (as-rrbt coll)
    (apply vector coll)))

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
                ~arr nil 0 0))))

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
     (loop [v  (transient (vector-of t x1 x2 x3 x4))
            xn xn]
       (if xn
         (recur (.conj ^clojure.lang.ITransientCollection v (first xn))
                (next xn))
         (persistent! v)))))
