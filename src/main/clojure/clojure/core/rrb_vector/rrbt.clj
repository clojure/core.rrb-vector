(ns clojure.core.rrb-vector.rrbt
  (:refer-clojure :exclude [assert ->VecSeq])
  (:require [clojure.core.rrb-vector.parameters :as p]
            [clojure.core.rrb-vector.protocols
             :refer [PSliceableVector slicev
                     PSpliceableVector splicev
                     PTransientDebugAccess]]
            [clojure.core.rrb-vector.nodes
             :refer [ranges overflow? last-range regular-ranges
                     first-child last-child remove-leftmost-child
                     replace-leftmost-child replace-rightmost-child
                     fold-tail new-path index-of-nil
                     object-am object-nm primitive-nm]]
            [clojure.core.rrb-vector.transients :refer [transient-helper]]
            [clojure.core.rrb-vector.fork-join :as fj]
            [clojure.core.protocols :refer [IKVReduce]]
            [clojure.core.reducers :as r :refer [CollFold coll-fold]])
  (:import (clojure.core ArrayManager Vec ArrayChunk)
           (clojure.lang RT Util Box PersistentVector
                         APersistentVector$SubVector)
           (clojure.core.rrb_vector.nodes NodeManager)
           (java.util.concurrent.atomic AtomicReference)))

(set! *warn-on-reflection* true)
(set! *unchecked-math* true) ;; :warn-on-boxed

(def ^:const rrbt-concat-threshold 33)
(def ^:const max-extra-search-steps 2)

(def ^:const elide-assertions? true)
(def ^:const elide-debug-printouts? true)

(defmacro assert [& args]
  (if-not elide-assertions?
    (apply #'clojure.core/assert &form &env args)))

(defmacro dbg [& args]
  (if-not elide-debug-printouts?
    `(prn ~@args)))

(defmacro dbg- [& args])

(defn throw-unsupported []
  (throw (UnsupportedOperationException.)))

(defmacro compile-if [test then else]
  (if (eval test)
    then
    else))

(defmacro ^:private caching-hash [coll hash-fn hash-key]
  `(let [h# ~hash-key]
     (if-not (== h# 0)
       h#
       (let [h# (~hash-fn ~coll)]
         (set! ~hash-key (int h#))
         h#))))

(defn ^:private hash-gvec-seq [xs]
  (let [cnt (count xs)]
    (loop [h (int 1) xs (seq xs)]
      (if xs
        (let [x (first xs)]
          (recur (unchecked-add-int (unchecked-multiply-int 31 h)
                                    (clojure.lang.Util/hash x))
                 (next xs)))
        h))))

(definterface IVecImpl
  (^int tailoff [])
  (arrayFor [^int i])
  (pushTail [^int shift ^int cnt parent tailnode])
  (popTail [^int shift ^int cnt node])
  (newPath [^java.util.concurrent.atomic.AtomicReference edit ^int shift node])
  (doAssoc [^int shift node ^int i val]))

(deftype VecSeq [^ArrayManager am ^IVecImpl vec anode ^int i ^int offset
                 ^clojure.lang.IPersistentMap _meta
                 ^:unsynchronized-mutable ^int _hash
                 ^:unsynchronized-mutable ^int _hasheq]
  clojure.core.protocols/InternalReduce
  (internal-reduce
   [_ f val]
   (loop [result val
          aidx i
          off offset]
     (if (< aidx (count vec))
       (let [node (.arrayFor vec aidx)
             alen (.alength am node)
             result (loop [result result
                           node-idx off]
                      (if (< node-idx alen)
                        (let [result (f result (.aget am node node-idx))]
                          (if (reduced? result)
                            result
                            (recur result (inc node-idx))))
                        result))]
         (if (reduced? result)
           @result
           (recur result (+ aidx alen) 0)))
       result)))

  Object
  (toString [this]
    (pr-str this))

  (hashCode [this]
    (caching-hash this hash-gvec-seq _hash))

  (equals [this that]
    (cond
      (identical? this that) true
      (not (or (sequential? that) (instance? java.util.List that))) false
      :else
      (loop [xs this ys (seq that)]
        (if xs
          (if ys
            (if (clojure.lang.Util/equals (first xs) (first ys))
              (recur (next xs) (next ys))
              false)
            false)
          (nil? ys)))))

  clojure.lang.IHashEq
  (hasheq [this]
    (let [h _hasheq]
      (if (== h 0)
        (compile-if (resolve 'clojure.core/hash-ordered-coll)
          (let [h (hash-ordered-coll this)]
            (do (set! _hasheq (int h))
                h))
          (loop [h (int 1) xs (seq this)]
            (if xs
              (recur (unchecked-add-int (unchecked-multiply-int (int 31) h)
                                        (Util/hasheq (first xs)))
                     (next xs))
              (do (set! _hasheq (int h))
                  h))))
        h)))

  clojure.lang.IMeta
  (meta [this]
    _meta)

  clojure.lang.IObj
  (withMeta [this m]
    (VecSeq. am vec anode i offset m _hash _hasheq))

  clojure.lang.Counted
  (count [this]
    (unchecked-subtract-int
      (unchecked-subtract-int (count vec) i)
      offset))

  clojure.lang.ISeq
  (first [_] (.aget am anode offset))
  (next [this]
    (if (< (inc offset) (.alength am anode))
      (VecSeq. am vec anode i (inc offset) nil 0 0)
      (.chunkedNext this)))
  (more [this]
    (let [s (.next this)]
      (or s (clojure.lang.PersistentList/EMPTY))))
  (cons [this o]
    (clojure.lang.Cons. o this))
  (equiv [this o]
    (cond
     (identical? this o) true
     (or (instance? clojure.lang.Sequential o) (instance? java.util.List o))
     (loop [me this
            you (seq o)]
       (if (nil? me)
         (nil? you)
         (and (clojure.lang.Util/equiv (first me) (first you))
              (recur (next me) (next you)))))
     :else false))
  (empty [_]
    clojure.lang.PersistentList/EMPTY)

  clojure.lang.Seqable
  (seq [this] this)

  clojure.lang.IChunkedSeq
  (chunkedFirst [_] (ArrayChunk. am anode offset (.alength am anode)))
  (chunkedNext [_]
   (let [nexti (+ i (.alength am anode))]
     (when (< nexti (count vec))
       (VecSeq. am vec (.arrayFor vec nexti) nexti 0 nil 0 0))))
  (chunkedMore [this]
    (let [s (.chunkedNext this)]
      (or s (clojure.lang.PersistentList/EMPTY))))

  java.lang.Iterable
  (iterator [this]
    (let [xs (clojure.lang.Box. (seq this))]
      (reify java.util.Iterator
        (next [this]
          (locking xs
            (if-let [v (.-val xs)]
              (let [x (first v)]
                (set! (.-val xs) (next v))
                x)
              (throw
                (java.util.NoSuchElementException.
                  "no more elements in VecSeq iterator")))))
        (hasNext [this]
          (locking xs
            (not (nil? (.-val xs)))))
        (remove [this]
          (throw-unsupported)))))

  java.io.Serializable

  java.util.Collection
  (contains [this o]
    (boolean (some #(= % o) this)))

  (containsAll [this c]
    (every? #(.contains this %) c))

  (isEmpty [this]
    (zero? (count this)))

  (toArray [this]
    (into-array Object this))

  (^"[Ljava.lang.Object;" toArray [this ^"[Ljava.lang.Object;" arr]
    (let [cnt (count this)]
      (if (>= (count arr) cnt)
        (do (dotimes [i cnt]
              (aset arr i (nth vec i)))
            arr)
        (into-array Object this))))

  (size [this]
    (count this))

  (add [_ o]             (throw-unsupported))
  (addAll [_ c]          (throw-unsupported))
  (clear [_]             (throw-unsupported))
  (^boolean remove [_ o] (throw-unsupported))
  (removeAll [_ c]       (throw-unsupported))
  (retainAll [_ c]       (throw-unsupported))

  java.util.List
  (get [this i]
    (nth this i))

  (indexOf [this o]
    (loop [xs (seq this) i 0]
      (if xs
        (let [x (first xs)]
          (if (= o x)
            i
            (recur (next xs) (unchecked-inc-int i))))
        -1)))

  (lastIndexOf [this o]
    (loop [xs (rseq vec)
           l  (unchecked-dec-int (- (count vec) i))]
      (cond
        (neg? l) -1
        (= o (first xs)) l
        :else (recur (next xs) (unchecked-dec-int l)))))

  (listIterator [this]
    (.listIterator this 0))

  (listIterator [this n]
    (let [n (java.util.concurrent.atomic.AtomicInteger. n)]
      (reify java.util.ListIterator
        (hasNext [_] (< (.get n) (count this)))
        (hasPrevious [_] (pos? n))
        (next [_]
          (try
            (nth vec (unchecked-add-int i
                       (unchecked-add-int offset
                         (unchecked-dec-int (.incrementAndGet n)))))
            (catch IndexOutOfBoundsException e
              (throw (java.util.NoSuchElementException.
                       "no more elements in VecSeq list iterator")))))
        (nextIndex [_] (.get n))
        (previous [_] (nth vec (unchecked-add i
                                 (unchecked-add offset
                                   (.decrementAndGet n)))))
        (previousIndex [_] (unchecked-dec-int (.get n)))
        (add [_ e]  (throw-unsupported))
        (remove [_] (throw-unsupported))
        (set [_ e]  (throw-unsupported)))))

  (subList [this a z]
    (seq (slicev vec
           (unchecked-add (unchecked-add i offset) a)
           (unchecked-add (unchecked-add i offset) z))))

  (add [_ i o]               (throw-unsupported))
  (addAll [_ i c]            (throw-unsupported))
  (^Object remove [_ ^int i] (throw-unsupported))
  (set [_ i e]               (throw-unsupported)))

(defprotocol AsRRBT
  (as-rrbt [v]))

(defn slice-right [^NodeManager nm ^ArrayManager am node shift end]
  (let [shift (int shift)
        end   (int end)]
    (if (zero? shift)
      ;; potentially return a short node, although it would be better to
      ;; make sure a regular leaf is always left at the right, with any
      ;; items over the final 32 moved into tail (and then potentially
      ;; back into the tree should the tail become too long...)
      (let [arr     (.array nm node)
            new-arr (.array am end)]
        (System/arraycopy arr 0 new-arr 0 end)
        (.node nm nil new-arr))
      (let [regular?  (.regular nm node)
            rngs      (if-not regular? (ranges nm node))
            i         (bit-and (bit-shift-right (unchecked-dec-int end) shift)
                               (int 0x1f))
            i         (if regular?
                        i
                        (loop [j i]
                          (if (<= end (aget rngs j))
                            j
                            (recur (unchecked-inc-int j)))))
            child-end (if regular?
                        (let [ce (unchecked-remainder-int
                                  end (bit-shift-left (int 1) shift))]
                          (if (zero? ce) (bit-shift-left (int 1) shift) ce))
                        (if (pos? i)
                          (unchecked-subtract-int
                           end (aget rngs (unchecked-dec-int i)))
                          end))
            arr       (.array nm node)
            new-child (slice-right nm am (aget ^objects arr i)
                                   (unchecked-subtract-int shift (int 5))
                                   child-end)
            regular-child? (if (== shift (int 5))
                             (== (int 32) (.alength am (.array nm new-child)))
                             (.regular nm new-child))
            new-arr   (object-array (if (and regular? regular-child?) 32 33))
            new-child-rng  (if regular-child?
                             (let [m (mod child-end (bit-shift-left 1 shift))]
                               (if (zero? m) (bit-shift-left 1 shift) m))
                             (if (== shift (int 5))
                               (.alength am (.array nm new-child))
                               (last-range nm new-child)))]
        (System/arraycopy arr 0 new-arr 0 i)
        (aset ^objects new-arr i new-child)
        (if-not (and regular? regular-child?)
          (let [new-rngs (int-array 33)
                step     (bit-shift-left (int 1) shift)]
            (if regular?
              (dotimes [j i]
                (aset new-rngs j (unchecked-multiply-int (inc j) step)))
              (dotimes [j i]
                (aset new-rngs j (aget rngs j))))
            (aset new-rngs i (unchecked-add-int
                              (if (pos? i)
                                (aget new-rngs (unchecked-dec-int i))
                                (int 0))
                              new-child-rng))
            (aset new-rngs 32 (unchecked-inc-int i))
            (aset new-arr 32 new-rngs)))
        (.node nm nil new-arr)))))

(defn slice-left [^NodeManager nm ^ArrayManager am node shift start end]
  (let [shift (int shift)
        start (int start)
        end   (int end)]
    (if (zero? shift)
      ;; potentially return a short node
      (let [arr     (.array nm node)
            new-len (unchecked-subtract-int (.alength am arr) start)
            new-arr (.array am new-len)]
        (System/arraycopy arr start new-arr 0 new-len)
        (.node nm nil new-arr))
      (let [regular? (.regular nm node)
            arr      (.array nm node)
            rngs     (if-not regular? (ranges nm node))
            i        (bit-and (bit-shift-right start shift) (int 0x1f))
            i        (if regular?
                       i
                       (loop [j i]
                         (if (< start (aget rngs j))
                           j
                           (recur (unchecked-inc-int j)))))
            len      (if regular?
                       (loop [i i]
                         (if (or (== i (int 32))
                                 (nil? (aget ^objects arr i)))
                           i
                           (recur (unchecked-inc-int i))))
                       (aget rngs 32))
            child-start (if (pos? i)
                          (unchecked-subtract-int
                           start (if regular?
                                   (unchecked-multiply-int
                                    i (bit-shift-left (int 1) shift))
                                   (aget rngs (unchecked-dec-int i))))
                          start)
            child-end   (int (if regular?
                               (min (bit-shift-left (int 1) shift)
                                    (if (pos? i)
                                      (unchecked-subtract-int
                                       end (unchecked-multiply-int
                                            i (bit-shift-left (int 1) shift)))
                                      end))
                               (let [capped-end (min (aget rngs i) end)]
                                 (if (pos? i)
                                   (unchecked-subtract-int
                                    capped-end
                                    (aget rngs (unchecked-dec-int i)))
                                   capped-end))))
            new-child   (slice-left nm am
                                    (aget ^objects arr i)
                                    (unchecked-subtract-int shift (int 5))
                                    child-start
                                    child-end)
            new-len     (unchecked-subtract-int len i)
            new-len     (if (nil? new-child) (unchecked-dec-int new-len) new-len)]
        (cond
          (zero? new-len)
          nil

          regular?
          (let [new-arr (object-array 33)
                rngs    (int-array 33)
                rng0    (if (or (nil? new-child)
                                (== shift (int 5))
                                (.regular nm new-child))
                          (unchecked-subtract-int
                           (bit-shift-left (int 1) shift)
                           (bit-and (bit-shift-right
                                     start (unchecked-subtract-int shift (int 5)))
                                    (int 0x1f)))
                          (int (last-range nm new-child)))
                step    (bit-shift-left (int 1) shift)]
            (loop [j (int 0)
                   r rng0]
              (when (< j new-len)
                (aset rngs j r)
                (recur (unchecked-inc-int j) (unchecked-add-int r step))))
            (when (> new-len 1)
              (aset rngs (dec new-len) (- end start)))
            (aset rngs 32 new-len)
            (System/arraycopy arr (if (nil? new-child) (unchecked-inc-int i) i)
                              new-arr 0
                              new-len)
            (if-not (nil? new-child)
              (aset new-arr 0 new-child))
            (aset new-arr 32 rngs)
            (.node nm (.edit nm node) new-arr))

          :else
          (let [new-arr  (object-array 33)
                new-rngs (int-array 33)]
            (loop [j (int 0) i i]
              (when (< j new-len)
                (aset new-rngs j (unchecked-subtract-int (aget rngs i) start))
                (recur (unchecked-inc-int j) (unchecked-inc-int i))))
            (aset new-rngs 32 new-len)
            (System/arraycopy arr (if (nil? new-child) (unchecked-inc-int i) i)
                              new-arr 0
                              new-len)
            (if-not (nil? new-child)
              (aset new-arr 0 new-child))
            (aset new-arr 32 new-rngs)
            (.node nm (.edit nm node) new-arr)))))))

(declare splice-rrbts ->Transient)

(deftype Vector [^NodeManager nm ^ArrayManager am ^int cnt ^int shift root tail
                 ^clojure.lang.IPersistentMap _meta
                 ^:unsynchronized-mutable ^int _hash
                 ^:unsynchronized-mutable ^int _hasheq]
  Object
  (equals [this that]
    (cond
      (identical? this that) true

      (or (instance? clojure.lang.IPersistentVector that)
          (instance? java.util.RandomAccess that))
      (and (== cnt (count that))
           (loop [i (int 0)]
             (cond
               (== i cnt) true
               (.equals (.nth this i) (nth that i)) (recur (unchecked-inc-int i))
               :else false)))

      (or (instance? clojure.lang.Sequential that)
          (instance? java.util.List that))
      (.equals (seq this) (seq that))

      :else false))

  (hashCode [this]
    (let [h _hash]
      (if (== h 0)
        (loop [h (int 1) i (int 0)]
          (if (== i cnt)
            (do (set! _hash (int h))
                h)
            (let [val (.nth this i)]
              (recur (unchecked-add-int (unchecked-multiply-int (int 31) h)
                                        (Util/hash val))
                     (unchecked-inc-int i)))))
        h)))

  (toString [this]
    (pr-str this))

  clojure.lang.IHashEq
  (hasheq [this]
    (let [h _hasheq]
      (if (== h 0)
        (compile-if (resolve 'clojure.core/hash-ordered-coll)
          (let [h (hash-ordered-coll this)]
            (do (set! _hasheq (int h))
                h))
          (loop [h (int 1) xs (seq this)]
            (if xs
              (recur (unchecked-add-int (unchecked-multiply-int (int 31) h)
                                        (Util/hasheq (first xs)))
                     (next xs))
              (do (set! _hasheq (int h))
                  h))))
        h)))

  clojure.lang.Counted
  (count [_] cnt)

  clojure.lang.IMeta
  (meta [_] _meta)

  clojure.lang.IObj
  (withMeta [_ m]
    (Vector. nm am cnt shift root tail m _hash _hasheq))

  clojure.lang.Indexed
  (nth [this i]
    (if (and (<= (int 0) i) (< i cnt))
      (let [tail-off (unchecked-subtract-int cnt (.alength am tail))]
        (if (<= tail-off i)
          (.aget am tail (unchecked-subtract-int i tail-off))
          (loop [i i node root shift shift]
            (if (zero? shift)
              (let [arr (.array nm node)]
                (.aget am arr (bit-and (bit-shift-right i shift) (int 0x1f))))
              (if (.regular nm node)
                (let [arr (.array nm node)
                      idx (bit-and (bit-shift-right i shift) (int 0x1f))]
                  (loop [i     i
                         node  (aget ^objects arr idx)
                         shift (unchecked-subtract-int shift (int 5))]
                    (let [arr (.array nm node)
                          idx (bit-and (bit-shift-right i shift) (int 0x1f))]
                      (if (zero? shift)
                        (.aget am arr idx)
                        (recur i
                               (aget ^objects arr idx)
                               (unchecked-subtract-int shift (int 5)))))))
                (let [arr  (.array nm node)
                      rngs (ranges nm node)
                      idx  (loop [j (bit-and (bit-shift-right i shift) (int 0x1f))]
                             (if (< i (aget rngs j))
                               j
                               (recur (unchecked-inc-int j))))
                      i    (if (zero? idx)
                             (int i)
                             (unchecked-subtract-int
                              (int i) (aget rngs (unchecked-dec-int idx))))]
                  (recur i
                         (aget ^objects arr idx)
                         (unchecked-subtract-int shift (int 5)))))))))
      (throw (IndexOutOfBoundsException.))))

  (nth [this i not-found]
    (if (and (>= i (int 0)) (< i cnt))
      (.nth this i)
      not-found))

  clojure.lang.IPersistentCollection
  (cons [this val]
    (if (< (.alength am tail) (int 32))
      (let [tail-len (.alength am tail)
            new-tail (.array am (unchecked-inc-int tail-len))]
        (System/arraycopy tail 0 new-tail 0 tail-len)
        (.aset am new-tail tail-len val)
        (Vector. nm am (unchecked-inc-int cnt) shift root new-tail _meta 0 0))
      (let [tail-node (.node nm (.edit nm root) tail)
            new-tail  (let [new-arr (.array am 1)]
                        (.aset am new-arr 0 val)
                        new-arr)]
        (if (overflow? nm root shift cnt)
          (if (.regular nm root)
            (let [new-arr  (object-array 32)
                  new-root (.node nm (.edit nm root) new-arr)]
              (doto new-arr
                (aset (int 0) root)
                (aset (int 1) (.newPath this (.edit nm root) shift tail-node)))
              (Vector. nm am
                       (unchecked-inc-int cnt)
                       (unchecked-add-int shift (int 5))
                       new-root new-tail _meta 0 0))
            (let [new-arr  (object-array 33)
                  new-rngs (ints (int-array 33))
                  new-root (.node nm (.edit nm root) new-arr)
                  root-total-range (aget (ranges nm root) (int 31))]
              (doto new-arr
                (aset (int 0)  root)
                (aset (int 1)  (.newPath this (.edit nm root) shift tail-node))
                (aset (int 32) new-rngs))
              (doto new-rngs
                (aset (int 0)  root-total-range)
                (aset (int 1)  (unchecked-add-int root-total-range (int 32)))
                (aset (int 32) (int 2)))
              (Vector. nm am
                       (unchecked-inc-int cnt)
                       (unchecked-add-int shift (int 5))
                       new-root new-tail _meta 0 0)))
          (Vector. nm am (unchecked-inc-int cnt) shift
                   (.pushTail this shift cnt root tail-node)
                   new-tail _meta 0 0)))))

  (empty [_]
    (Vector. nm am 0 5 (.empty nm) (.array am 0) _meta 0 0))

  (equiv [this that]
    (cond
      (or (instance? clojure.lang.IPersistentVector that)
          (instance? java.util.RandomAccess that))
      (and (== cnt (count that))
           (loop [i (int 0)]
             (cond
               (== i cnt) true
               (= (.nth this i) (nth that i)) (recur (unchecked-inc-int i))
               :else false)))

      (or (instance? clojure.lang.Sequential that)
          (instance? java.util.List that))
      (Util/equiv (seq this) (seq that))

      :else false))

  clojure.lang.IPersistentStack
  (peek [this]
    (when (pos? cnt)
      (.nth this (unchecked-dec-int cnt))))

  (pop [this]
    (cond
      (zero? cnt)
      (throw (IllegalStateException. "Can't pop empty vector"))

      (== 1 cnt)
      (Vector. nm am 0 5 (.empty nm) (.array am 0) _meta 0 0)

      (> (.alength am tail) (int 1))
      (let [new-tail (.array am (unchecked-dec-int (.alength am tail)))]
        (System/arraycopy tail 0 new-tail 0 (.alength am new-tail))
        (Vector. nm am (unchecked-dec-int cnt) shift root new-tail _meta 0 0))

      :else
      (let [new-tail (.arrayFor this (unchecked-subtract-int cnt (int 2)))
            root-cnt (.tailoff this)
            new-root (.popTail this shift root-cnt root)]
        (cond
          (nil? new-root)
          (Vector. nm am (unchecked-dec-int cnt) shift (.empty nm) new-tail
                   _meta 0 0)

          (and (> shift (int 5))
               (nil? (aget ^objects (.array nm new-root) 1)))
          (Vector. nm am
                   (unchecked-dec-int cnt)
                   (unchecked-subtract-int shift (int 5))
                   (aget ^objects (.array nm new-root) 0)
                   new-tail _meta 0 0)

          :else
          (Vector. nm am (unchecked-dec-int cnt) shift new-root new-tail
                   _meta 0 0)))))

  clojure.lang.IPersistentVector
  (assocN [this i val]
    (cond
      (and (<= (int 0) i) (< i cnt))
      (let [tail-off (.tailoff this)]
        (if (>= i tail-off)
          (let [new-tail (.array am (.alength am tail))
                idx (unchecked-subtract-int i tail-off)]
            (System/arraycopy tail 0 new-tail 0 (.alength am tail))
            (.aset am new-tail idx val)
            (Vector. nm am cnt shift root new-tail _meta 0 0))
          (Vector. nm am cnt shift (.doAssoc this shift root i val) tail
                   _meta 0 0)))

      (== i cnt) (.cons this val)
      :else (throw (IndexOutOfBoundsException.))))

  (length [this]
    (.count this))

  clojure.lang.Reversible
  (rseq [this]
    (if (pos? cnt)
      (clojure.lang.APersistentVector$RSeq. this (unchecked-dec-int cnt))
      nil))

  clojure.lang.Associative
  (assoc [this k v]
    (if (Util/isInteger k)
      (.assocN this k v)
      (throw (IllegalArgumentException. "Key must be integer"))))

  (containsKey [this k]
    (and (Util/isInteger k)
         (<= (int 0) (int k))
         (< (int k) cnt)))

  (entryAt [this k]
    (if (.containsKey this k)
      (clojure.lang.MapEntry. k (.nth this (int k)))
      nil))

  clojure.lang.ILookup
  (valAt [this k not-found]
    (if (Util/isInteger k)
      (let [i (int k)]
        (if (and (>= i (int 0)) (< i cnt))
          (.nth this i)
          not-found))
      not-found))

  (valAt [this k]
    (.valAt this k nil))

  clojure.lang.IFn
  (invoke [this k]
    (if (Util/isInteger k)
      (let [i (int k)]
        (if (and (>= i (int 0)) (< i cnt))
          (.nth this i)
          (throw (IndexOutOfBoundsException.))))
      (throw (IllegalArgumentException. "Key must be integer"))))

  (applyTo [this args]
    (let [n (RT/boundedLength args 1)]
      (case n
        0 (throw (clojure.lang.ArityException.
                  n (.. this (getClass) (getSimpleName))))
        1 (.invoke this (first args))
        2 (throw (clojure.lang.ArityException.
                  n (.. this (getClass) (getSimpleName)))))))

  clojure.lang.Seqable
  (seq [this]
    (if (zero? cnt)
      nil
      (VecSeq. am this (.arrayFor this 0) 0 0 nil 0 0)))

  clojure.lang.Sequential

  clojure.lang.IEditableCollection
  (asTransient [this]
    (->Transient nm am
                 (identical? am object-am)
                 cnt
                 shift
                 (.editableRoot transient-helper nm am root)
                 (.editableTail transient-helper am tail)
                 (.alength am tail)))

  IVecImpl
  (tailoff [_]
    (unchecked-subtract-int cnt (.alength am tail)))

  (arrayFor [this i]
    (if (and (<= (int 0) i) (< i cnt))
      (if (>= i (.tailoff this))
        tail
        (loop [i (int i) node root shift shift]
          (if (zero? shift)
            (.array nm node)
            (if (.regular nm node)
              (loop [node  (aget ^objects (.array nm node)
                                 (bit-and (bit-shift-right i shift) (int 0x1f)))
                     shift (unchecked-subtract-int shift (int 5))]
                (if (zero? shift)
                  (.array nm node)
                  (recur (aget ^objects (.array nm node)
                               (bit-and (bit-shift-right i shift) (int 0x1f)))
                         (unchecked-subtract-int shift (int 5)))))
              (let [rngs (ranges nm node)
                    j    (loop [j (bit-and (bit-shift-right i shift) (int 0x1f))]
                           (if (< i (aget rngs j))
                             j
                             (recur (unchecked-inc-int j))))
                    i    (if (pos? j)
                           (unchecked-subtract-int
                            i (aget rngs (unchecked-dec-int j)))
                           i)]
                (recur (int i)
                       (aget ^objects (.array nm node) j)
                       (unchecked-subtract-int shift (int 5)))))))) 
      (throw (IndexOutOfBoundsException.))))

  (pushTail [this shift cnt node tail-node]
    (if (.regular nm node)
      (let [arr (aclone ^objects (.array nm node))
            ret (.node nm (.edit nm node) arr)]
        (loop [node ret shift (int shift)]
          (let [arr    (.array nm node)
                subidx (bit-and (bit-shift-right (unchecked-dec-int cnt) shift)
                                (int 0x1f))]
            (if (== shift (int 5))
              (aset ^objects arr subidx tail-node)
              (if-let [child (aget ^objects arr subidx)]
                (let [new-carr  (aclone ^objects (.array nm child))
                      new-child (.node nm (.edit nm root) new-carr)]
                  (aset ^objects arr subidx new-child)
                  (recur new-child (unchecked-subtract-int shift (int 5))))
                (aset ^objects arr subidx
                      (.newPath this (.edit nm root)
                                (unchecked-subtract-int
                                 shift (int 5))
                                tail-node))))))
        ret)
      (let [arr  (aclone ^objects (.array nm node))
            rngs (ranges nm node)
            li   (unchecked-dec-int (aget rngs 32))
            ret  (.node nm (.edit nm node) arr)
            cret (if (== shift (int 5))
                   nil
                   (let [child (aget ^objects arr li)
                         ccnt  (unchecked-add-int
                                (int (if (pos? li)
                                       (unchecked-subtract-int
                                        (aget rngs li)
                                        (aget rngs (unchecked-dec-int li)))
                                       (aget rngs 0)))
                                ;; add 32 elems to account for the new
                                ;; full tail we plan to add to the
                                ;; subtree.
                                (int 32))]
                     ;; See Note 2 in file transients.clj
                     (if-not (overflow? nm child
                                        (unchecked-subtract-int shift (int 5))
                                        ccnt)
                       (.pushTail this
                                  (unchecked-subtract-int shift (int 5))
                                  ccnt
                                  (aget ^objects arr li)
                                  tail-node))))]
        (if cret
          (do (aset ^objects arr li cret)
              (aset rngs li (unchecked-add-int (aget rngs li) (int 32)))
              ret)
          (do (when (>= li 31)
                ;; See Note 1 in file transients.clj
                (let [msg (str "Assigning index " (inc li) " of vector"
                               " object array to become a node, when that"
                               " index should only be used for storing"
                               " range arrays.")
                      data {:shift shift, :cnt cnt, :node node,
                            :tail-node tail-node, :rngs rngs, :li li,
                            :cret cret}]
                  (throw (ex-info msg data))))
              (aset ^objects arr (unchecked-inc-int li)
                    (.newPath this (.edit nm root)
                              (unchecked-subtract-int shift (int 5))
                              tail-node))
              (aset rngs (unchecked-inc-int li)
                    (unchecked-add-int (aget rngs li) (int 32)))
              (aset rngs 32 (unchecked-inc-int (aget rngs 32)))
              ret)))))

  (popTail [this shift cnt node]
    (if (.regular nm node)
      (let [subidx (bit-and
                    (bit-shift-right (unchecked-subtract-int cnt (int 2))
                                     (int shift))
                    (int 0x1f))]
        (cond
          (> (int shift) (int 5))
          (let [new-child (.popTail this
                                    (unchecked-subtract-int (int shift) (int 5))
                                    cnt
                                    (aget ^objects (.array nm node) subidx))]
            (if (and (nil? new-child) (zero? subidx))
              nil
              (let [arr (aclone ^objects (.array nm node))]
                (aset arr subidx new-child)
                (.node nm (.edit nm root) arr))))

          (zero? subidx)
          nil

          :else
          (let [arr (aclone ^objects (.array nm node))]
            (aset arr subidx nil)
            (.node nm (.edit nm root) arr))))
      (let [rngs   (ranges nm node)
            subidx (unchecked-dec-int (aget rngs 32))
            new-rngs (aclone rngs)]
        (cond
          (> (int shift) (int 5))
          (let [child     (aget ^objects (.array nm node) subidx)
                child-cnt (if (zero? subidx)
                            (aget rngs 0)
                            (unchecked-subtract-int
                             (aget rngs subidx)
                             (aget rngs (unchecked-dec-int subidx))))
                new-child (.popTail this
                                    (unchecked-subtract-int (int shift) (int 5))
                                    child-cnt
                                    child)]
            (cond
              (and (nil? new-child) (zero? subidx))
              nil

              (.regular nm child)
              (let [arr (aclone ^objects (.array nm node))]
                (aset new-rngs subidx
                      (unchecked-subtract-int (aget new-rngs subidx) (int 32)))
                (aset arr subidx new-child)
                (aset arr (int 32) new-rngs)
                (if (nil? new-child)
                  (aset new-rngs 32 (unchecked-dec-int (aget new-rngs 32))))
                (.node nm (.edit nm root) arr))

              :else
              (let [rng  (int (last-range nm child))
                    diff (unchecked-subtract-int
                          rng
                          (if new-child
                            (last-range nm new-child)
                            0))
                    arr  (aclone ^objects (.array nm node))]
                (aset new-rngs subidx
                      (unchecked-subtract-int (aget new-rngs subidx) diff))
                (aset arr subidx new-child)
                (aset arr (int 32) new-rngs)
                (if (nil? new-child)
                  (aset new-rngs 32 (unchecked-dec-int (aget new-rngs 32))))
                (.node nm (.edit nm root) arr))))

          (zero? subidx)
          nil

          :else
          (let [arr      (aclone ^objects (.array nm node))
                child    (aget arr subidx)
                new-rngs (aclone rngs)]
            (aset arr subidx nil)
            (aset arr (int 32) new-rngs)
            (aset new-rngs subidx 0)
            (aset new-rngs 32 (unchecked-dec-int (aget new-rngs (int 32))))
            (.node nm (.edit nm root) arr))))))

  (newPath [this ^AtomicReference edit ^int shift node]
    (if (== (.alength am tail) (int 32))
      (let [shift (int shift)]
        (loop [s (int 0) node node]
          (if (== s shift)
            node
            (let [arr (object-array 32)
                  ret (.node nm edit arr)]
              (aset arr 0 node)
              (recur (unchecked-add-int s (int 5)) ret)))))
      (let [shift (int shift)]
        (loop [s (int 0) node node]
          (if (== s shift)
            node
            (let [arr  (object-array 33)
                  rngs (int-array 33)
                  ret  (.node nm edit arr)]
              (aset arr 0 node)
              (aset arr 32 rngs)
              (aset rngs 32 1)
              (aset rngs 0 (.alength am tail))
              (recur (unchecked-add-int s (int 5)) ret)))))))

  (doAssoc [this shift node i val]
    (if (.regular nm node)
      (let [node (.clone nm am shift node)]
        (loop [shift (int shift)
               node  node]
          (if (zero? shift)
            (let [arr (.array nm node)]
              (.aset am arr (bit-and i (int 0x1f)) val))
            (let [arr    (.array nm node)
                  subidx (bit-and (bit-shift-right i shift) (int 0x1f))
                  next-shift (int (unchecked-subtract-int shift (int 5)))
                  child  (.clone nm am next-shift (aget ^objects arr subidx))]
              (aset ^objects arr subidx child)
              (recur next-shift child))))
        node)
      (let [arr    (aclone ^objects (.array nm node))
            rngs   (ranges nm node)
            subidx (bit-and (bit-shift-right i shift) (int 0x1f))
            subidx (loop [subidx subidx]
                     (if (< i (aget rngs subidx))
                       subidx
                       (recur (unchecked-inc-int subidx))))
            i      (if (zero? subidx)
                     i
                     (unchecked-subtract-int
                      i (aget rngs (unchecked-dec-int subidx))))]
        (aset arr subidx
              (.doAssoc this
                        (unchecked-subtract-int (int shift) (int 5))
                        (aget arr subidx)
                        i
                        val))
        (.node nm (.edit nm node) arr))))

  IKVReduce
  (kv-reduce [this f init]
    (loop [i (int 0)
           j (int 0)
           init init
           arr  (.arrayFor this i)
           lim  (unchecked-dec-int (.alength am arr))
           step (unchecked-inc-int lim)]
      (let [init (f init (unchecked-add-int i j) (.aget am arr j))]
        (if (reduced? init)
          @init
          (if (< j lim)
            (recur i (unchecked-inc-int j) init arr lim step)
            (let [i (unchecked-add-int i step)]
              (if (< i cnt)
                (let [arr (.arrayFor this i)
                      len (.alength am arr)
                      lim (unchecked-dec-int len)]
                  (recur i (int 0) init arr lim len))
                init)))))))

  CollFold
  ;; adapted from #'clojure.core.reducers/foldvec
  (coll-fold [this n combinef reducef]
    (let [n (int n)]
      (cond
        (zero? cnt) (combinef)
        (<= cnt n)  (r/reduce reducef (combinef) this)
        :else
        (let [split (quot cnt 2)
              v1 (slicev this 0 split)
              v2 (slicev this split cnt)
              fc (fn [child] #(coll-fold child n combinef reducef))]
          (fj/invoke
           #(let [f1 (fc v1)
                  t2 (fj/task (fc v2))]
              (fj/fork t2)
              (combinef (f1) (fj/join t2))))))))
  
  PSliceableVector
  (slicev [this start end]
    (let [start   (int start)
          end     (int end)
          new-cnt (unchecked-subtract-int end start)]
      (cond
        (or (neg? start) (> end cnt))
        (throw (IndexOutOfBoundsException.))

        (== start end)
        ;; NB. preserves metadata
        (empty this)

        (> start end)
        (throw (IllegalStateException. "start index greater than end index"))

        :else
        (let [tail-off (.tailoff this)]
          (if (>= start tail-off)
            (let [new-tail (.array am new-cnt)]
              (System/arraycopy tail (unchecked-subtract-int start tail-off)
                                new-tail 0
                                new-cnt)
              (Vector. nm am new-cnt (int 5) (.empty nm) new-tail _meta 0 0))
            (let [tail-cut? (> end tail-off)
                  new-root  (if tail-cut?
                              root
                              (slice-right nm am root shift end))
                  new-root  (if (zero? start)
                              new-root
                              (slice-left nm am new-root shift start
                                          (min end tail-off)))
                  new-tail  (if tail-cut?
                              (let [new-len  (unchecked-subtract-int end tail-off)
                                    new-tail (.array am new-len)]
                                (System/arraycopy tail 0 new-tail 0 new-len)
                                new-tail)
                              (.arrayFor (Vector. nm am new-cnt shift new-root
                                                  (.array am 0) nil 0 0)
                                         (unchecked-dec-int new-cnt)))
                  new-root  (if tail-cut?
                              new-root
                              (.popTail (Vector. nm am
                                                 new-cnt
                                                 shift new-root
                                                 (.array am 0) nil 0 0)
                                        shift new-cnt new-root))]
              (if (nil? new-root)
                (Vector. nm am new-cnt 5 (.empty nm) new-tail _meta 0 0)
                (loop [r new-root
                       s (int shift)]
                  (if (and (> s (int 5))
                           (nil? (aget ^objects (.array nm r) 1)))
                    (recur (aget ^objects (.array nm r) 0)
                           (unchecked-subtract-int s (int 5)))
                    (Vector. nm am new-cnt s r new-tail _meta 0 0))))))))))

  PSpliceableVector
  (splicev [this that]
    (splice-rrbts nm am this (as-rrbt that)))

  AsRRBT
  (as-rrbt [this]
    this)

  java.io.Serializable

  java.lang.Comparable
  (compareTo [this that]
    (if (identical? this that)
      0
      (let [^clojure.lang.IPersistentVector v
            (cast clojure.lang.IPersistentVector that)
            vcnt (.count v)]
        (cond
          (< cnt vcnt) -1
          (> cnt vcnt) 1
          :else
          (loop [i (int 0)]
            (if (== i cnt)
              0
              (let [comp (Util/compare (.nth this i) (.nth v i))]
                (if (zero? comp)
                  (recur (unchecked-inc-int i))
                  comp))))))))

  java.lang.Iterable
  (iterator [this]
    (let [i (java.util.concurrent.atomic.AtomicInteger. 0)]
      (reify java.util.Iterator
        (hasNext [_] (< (.get i) cnt))
        (next [_]
          (try
            (.nth this (unchecked-dec-int (.incrementAndGet i)))
            (catch IndexOutOfBoundsException e
              (throw (java.util.NoSuchElementException.
                       "no more elements in RRB vector iterator")))))
        (remove [_] (throw-unsupported)))))

  java.util.Collection
  (contains [this o]
    (boolean (some #(= % o) this)))

  (containsAll [this c]
    (every? #(.contains this %) c))

  (isEmpty [_]
    (zero? cnt))

  (toArray [this]
    (into-array Object this))

  (^"[Ljava.lang.Object;" toArray [this ^"[Ljava.lang.Object;" arr]
    (if (>= (count arr) cnt)
      (do (dotimes [i cnt]
            (aset arr i (.nth this i)))
          arr)
      (into-array Object this)))

  (size [_] cnt)

  (add [_ o]             (throw-unsupported))
  (addAll [_ c]          (throw-unsupported))
  (clear [_]             (throw-unsupported))
  (^boolean remove [_ o] (throw-unsupported))
  (removeAll [_ c]       (throw-unsupported))
  (retainAll [_ c]       (throw-unsupported))

  java.util.RandomAccess
  java.util.List
  (get [this i] (.nth this i))

  (indexOf [this o]
    (loop [i (int 0)]
      (cond
        (== i cnt) -1
        (= o (.nth this i)) i
        :else (recur (unchecked-inc-int i)))))

  (lastIndexOf [this o]
    (loop [i (unchecked-dec-int cnt)]
      (cond
        (neg? i) -1
        (= o (.nth this i)) i
        :else (recur (unchecked-dec-int i)))))

  (listIterator [this]
    (.listIterator this 0))

  (listIterator [this i]
    (let [i (java.util.concurrent.atomic.AtomicInteger. i)]
      (reify java.util.ListIterator
        (hasNext [_] (< (.get i) cnt))
        (hasPrevious [_] (pos? i))
        (next [_]
          (try
            (.nth this (unchecked-dec-int (.incrementAndGet i)))
            (catch IndexOutOfBoundsException e
              (throw (java.util.NoSuchElementException.
                       "no more elements in RRB vector list iterator")))))
        (nextIndex [_] (.get i))
        (previous [_] (.nth this (.decrementAndGet i)))
        (previousIndex [_] (unchecked-dec-int (.get i)))
        (add [_ e]  (throw-unsupported))
        (remove [_] (throw-unsupported))
        (set [_ e]  (throw-unsupported)))))

  (subList [this a z]
    (slicev this a z))

  (add [_ i o]               (throw-unsupported))
  (addAll [_ i c]            (throw-unsupported))
  (^Object remove [_ ^int i] (throw-unsupported))
  (set [_ i e]               (throw-unsupported)))

(extend-protocol AsRRBT
  Vec
  (as-rrbt [^Vec this]
    (Vector. primitive-nm (.-am this)
             (.-cnt this) (.-shift this) (.-root this) (.-tail this)
             (.-_meta this) 0 0))

  PersistentVector
  (as-rrbt [^PersistentVector this]
    (Vector. object-nm object-am
             (count this) (.-shift this) (.-root this) (.-tail this)
             (meta this) 0 0))

  APersistentVector$SubVector
  (as-rrbt [^APersistentVector$SubVector this]
    (let [v     (.-v this)
          start (.-start this)
          end   (.-end this)]
      (slicev (as-rrbt v) start end)))

  java.util.Map$Entry
  (as-rrbt [^java.util.Map$Entry this]
    (as-rrbt [(.getKey this) (.getValue this)])))

(defn shift-from-to [^NodeManager nm node from to]
  (cond
    (== from to)
    node

    (.regular nm node)
    (recur nm
           (.node nm (.edit nm node) (doto (object-array 32) (aset 0 node)))
           (unchecked-add-int (int 5) (int from))
           to)

    :else
    (recur nm
           (.node nm
                  (.edit nm node)
                  (doto (object-array 33)
                    (aset 0 node)
                    (aset 32
                          (ints (doto (int-array 33)
                                  (aset 0  (int (last-range nm node)))
                                  (aset 32 (int 1)))))))
           (unchecked-add-int (int 5) (int from))
           to)))

(defn pair ^"[Ljava.lang.Object;" [x y]
  (doto (object-array 2)
    (aset 0 x)
    (aset 1 y)))

(defn slot-count [^NodeManager nm ^ArrayManager am node shift]
  (let [arr (.array nm node)]
    (if (zero? shift)
      (.alength am arr)
      (if (.regular nm node)
        (index-of-nil arr)
        (let [rngs (ranges nm node)]
          (aget rngs 32))))))

(defn subtree-branch-count [^NodeManager nm ^ArrayManager am node shift]
  ;; NB. positive shifts only
  (let [arr (.array nm node)
        cs  (- shift 5)]
    (if (.regular nm node)
      (loop [i 0 sbc 0]
        (if (== i 32)
          sbc
          (if-let [child (aget ^objects arr i)]
            (recur (inc i) (+ sbc (long (slot-count nm am child cs))))
            sbc)))
      (let [lim (aget (ranges nm node) 32)]
        (loop [i 0 sbc 0]
          (if (== i lim)
            sbc
            (let [child (aget ^objects arr i)]
              (recur (inc i) (+ sbc (long (slot-count nm am child cs)))))))))))

(defn leaf-seq [^NodeManager nm arr]
  (mapcat #(.array nm %) (take (index-of-nil arr) arr)))

(defn rebalance-leaves
  [^NodeManager nm ^ArrayManager am n1 cnt1 n2 cnt2 ^Box transferred-leaves]
  (let [slc1 (slot-count nm am n1 5)
        slc2 (slot-count nm am n2 5)
        a    (+ slc1 slc2)
        sbc1 (subtree-branch-count nm am n1 5)
        sbc2 (subtree-branch-count nm am n2 5)
        p    (+ sbc1 sbc2)
        e    (- a (inc (quot (dec p) 32)))]
    (cond
      (<= e max-extra-search-steps)
      (pair n1 n2)

      (<= (+ sbc1 sbc2) 1024)
      (let [reg?    (zero? (mod p 32))
            new-arr (object-array (if reg? 32 33))
            new-n1  (.node nm nil new-arr)]
        (loop [i  0
               bs (partition-all 32
                                 (concat (leaf-seq nm (.array nm n1))
                                         (leaf-seq nm (.array nm n2))))]
          (when-first [block bs]
            (let [a (.array am (count block))]
              (loop [i 0 xs (seq block)]
                (when xs
                  (.aset am a i (first xs))
                  (recur (inc i) (next xs))))
              (aset new-arr i (.node nm nil a))
              (recur (inc i) (next bs)))))
        (if-not reg?
          (aset new-arr 32 (regular-ranges 5 p)))
        (set! (.-val transferred-leaves) sbc2)
        (pair new-n1 nil))

      :else
      (let [reg?     (zero? (mod p 32))
            new-arr1 (object-array 32)
            new-arr2 (object-array (if reg? 32 33))
            new-n1   (.node nm nil new-arr1)
            new-n2   (.node nm nil new-arr2)]
        (loop [i  0
               bs (partition-all 32
                                 (concat (leaf-seq nm (.array nm n1))
                                         (leaf-seq nm (.array nm n2))))]
          (when-first [block bs]
            (let [a (.array am (count block))]
              (loop [i 0 xs (seq block)]
                (when xs
                  (.aset am a i (first xs))
                  (recur (inc i) (next xs))))
              (if (< i 32)
                (aset new-arr1 i (.node nm nil a))
                (aset new-arr2 (- i 32) (.node nm nil a)))
              (recur (inc i) (next bs)))))
        (if-not reg?
          (aset new-arr2 32 (regular-ranges 5 (- p 1024))))
        (set! (.-val transferred-leaves) (- 1024 sbc1))
        (pair new-n1 new-n2)))))

(defn child-seq [^NodeManager nm node shift cnt]
  (let [arr  (.array nm node)
        rngs (if (.regular nm node)
               (ints (regular-ranges shift cnt))
               (ranges nm node))
        cs   (if rngs (aget rngs 32) (index-of-nil arr))
        cseq (fn cseq [c r]
               (let [arr  (.array nm c)
                     rngs (if (.regular nm c)
                            (ints (regular-ranges (- shift 5) r))
                            (ranges nm c))
                     gcs  (if rngs (aget rngs 32) (index-of-nil arr))]
                 (map list (take gcs arr) (take gcs (map - rngs (cons 0 rngs))))))]
    (mapcat cseq (take cs arr) (take cs (map - rngs (cons 0 rngs))))))

(defn rebalance
  [^NodeManager nm ^ArrayManager am shift n1 cnt1 n2 cnt2 ^Box transferred-leaves]
  (if (nil? n2)
    (pair n1 nil)
    (let [slc1 (slot-count nm am n1 shift)
          slc2 (slot-count nm am n2 shift)
          a    (+ slc1 slc2)
          sbc1 (subtree-branch-count nm am n1 shift)
          sbc2 (subtree-branch-count nm am n2 shift)
          p    (+ sbc1 sbc2)
          e    (- a (inc (quot (dec p) 32)))]
      (cond
        (<= e max-extra-search-steps)
        (pair n1 n2)

        (<= (+ sbc1 sbc2) 1024)
        (let [new-arr  (object-array 33)
              new-rngs (int-array 33)
              new-n1   (.node nm nil new-arr)]
          (loop [i  0
                 bs (partition-all 32
                                   (concat (child-seq nm n1 shift cnt1)
                                           (child-seq nm n2 shift cnt2)))]
            (when-first [block bs]
              (let [a (object-array 33)
                    r (int-array 33)]
                (aset a 32 r)
                (aset r 32 (count block))
                (loop [i 0 o (int 0) gcs (seq block)]
                  (when-first [[gc gcr] gcs]
                    (aset ^objects a i gc)
                    (aset r i (unchecked-add-int o (int gcr)))
                    (recur (inc i) (unchecked-add-int o (int gcr)) (next gcs))))
                (aset ^objects new-arr i (.node nm nil a))
                (aset new-rngs i
                      (+ (aget r (dec (aget r 32)))
                         (if (pos? i) (aget new-rngs (dec i)) (int 0))))
                (aset new-rngs 32 (inc i))
                (recur (inc i) (next bs)))))
          (aset new-arr 32 new-rngs)
          (set! (.-val transferred-leaves) cnt2)
          (pair new-n1 nil))

        :else
        (let [new-arr1  (object-array 33)
              new-arr2  (object-array 33)
              new-rngs1 (int-array 33)
              new-rngs2 (int-array 33)
              new-n1    (.node nm nil new-arr1)
              new-n2    (.node nm nil new-arr2)]
          (loop [i  0
                 bs (partition-all 32
                                   (concat (child-seq nm n1 shift cnt1)
                                           (child-seq nm n2 shift cnt2)))]
            (when-first [block bs]
              (let [a (object-array 33)
                    r (int-array 33)]
                (aset a 32 r)
                (aset r 32 (count block))
                (loop [i 0 o (int 0) gcs (seq block)]
                  (when-first [[gc gcr] gcs]
                    (aset a i gc)
                    (aset r i (unchecked-add-int o (int gcr)))
                    (recur (inc i) (unchecked-add-int o (int gcr)) (next gcs))))
                (if (and (< i 32) (> (+ (* i 32) (count block)) sbc1))
                  (let [tbs (- (+ (* i 32) (count block)) sbc1)
                        li  (dec (aget r 32))
                        d   (if (>= tbs 32)
                              (aget r li)
                              (- (aget r li) (aget r (- li tbs))))]
                    (set! (.-val transferred-leaves)
                          (+ (.-val transferred-leaves) d))))
                (let [new-arr  (if (< i 32) new-arr1 new-arr2)
                      new-rngs (if (< i 32) new-rngs1 new-rngs2)
                      i        (mod i 32)]
                  (aset ^objects new-arr i (.node nm nil a))
                  (aset new-rngs i
                        (+ (aget r (dec (aget r 32)))
                           (if (pos? i) (aget new-rngs (dec i)) (int 0))))
                  (aset new-rngs 32 (int (inc i))))
                (recur (inc i) (next bs)))))
          (aset new-arr1 32 new-rngs1)
          (aset new-arr2 32 new-rngs2)
          (pair new-n1 new-n2))))))

(defn zippath
  [^NodeManager nm ^ArrayManager am shift n1 cnt1 n2 cnt2 ^Box transferred-leaves]
  (if (== shift 5)
    (rebalance-leaves nm am n1 cnt1 n2 cnt2 transferred-leaves)
    (let [c1 (last-child nm n1)
          c2 (first-child nm n2)
          ccnt1 (if (.regular nm n1)
                  (let [m (mod cnt1 (bit-shift-left 1 shift))]
                    (if (zero? m) (bit-shift-left 1 shift) m))
                  (let [rngs (ranges nm n1)
                        i    (dec (aget rngs 32))]
                    (if (zero? i)
                      (aget rngs 0)
                      (- (aget rngs i) (aget rngs (dec i))))))
          ccnt2 (if (.regular nm n2)
                  (let [m (mod cnt2 (bit-shift-left 1 shift))]
                    (if (zero? m) (bit-shift-left 1 shift) m))
                  (aget (ranges nm n2) 0))
          next-transferred-leaves (Box. 0)
          [new-c1 new-c2] (zippath nm am (- shift 5) c1 ccnt1 c2 ccnt2
                                   next-transferred-leaves)
          d (.-val next-transferred-leaves)]
      (set! (.-val transferred-leaves) (+ (.-val transferred-leaves) d))
      (rebalance nm am shift
                 (if (identical? c1 new-c1)
                   n1
                   (replace-rightmost-child nm shift n1 new-c1 d))
                 (+ cnt1 d)
                 (if new-c2
                   (if (identical? c2 new-c2)
                     n2
                     (replace-leftmost-child nm shift n2 cnt2 new-c2 d))
                   (remove-leftmost-child nm shift n2))
                 (- cnt2 d)
                 transferred-leaves))))

(defn squash-nodes [^NodeManager nm shift n1 cnt1 n2 cnt2]
  (let [arr1  (.array nm n1)
        arr2  (.array nm n2)
        li1   (index-of-nil arr1)
        li2   (index-of-nil arr2)
        slots (concat (take li1 arr1) (take li2 arr2))]
    (if (> (count slots) 32)
      (pair n1 n2)
      (let [new-rngs (int-array 33)
            new-arr  (object-array 33)
            rngs1    (take li1 (if (.regular nm n1)
                                 (regular-ranges shift cnt1)
                                 (ranges nm n1)))
            rngs2    (take li2 (if (.regular nm n2)
                                 (regular-ranges shift cnt2)
                                 (ranges nm n2)))
            rngs2    (let [r (last rngs1)]
                       (map #(+ % r) rngs2))
            rngs     (concat rngs1 rngs2)]
        (aset new-arr 32 new-rngs)
        (loop [i 0 cs (seq slots)]
          (when cs
            (aset new-arr i (first cs))
            (recur (inc i) (next cs))))
        (loop [i 0 rngs (seq rngs)]
          (if rngs
            (do (aset new-rngs i (int (first rngs)))
                (recur (inc i) (next rngs)))
            (aset new-rngs 32 i)))
        (pair (.node nm nil new-arr) nil)))))

(def peephole-optimization-config (atom {:debug-fn nil}))
(def peephole-optimization-count (atom 0))

;; TBD: Transducer versions of child-nodes and bounded-grandchildren
;; are included here for when we are willing to rely upon Clojure
;; 1.7.0 as the minimum version supported by the core.rrb-vector
;; library.  They are faster.

#_(defn child-nodes [node ^NodeManager nm]
  (into [] (comp (take-while (complement nil?))
                 (take 32))
        (.array nm node)))

(defn child-nodes [node ^NodeManager nm]
  (->> (.array nm node)
       (take-while (complement nil?))
       (take 32)))

;; (take 33) is just a technique to avoid generating more
;; grandchildren than necessary.  If there are at least 33, we do not
;; care how many there are.
#_(defn bounded-grandchildren [nm children]
  (into [] (comp (map #(child-nodes % nm))
                 cat
                 (take 33))
        children))

(defn bounded-grandchildren [nm children]
  (->> children
       (mapcat #(child-nodes % nm))
       (take 33)))

;; TBD: Do functions like last-non-nil-idx and
;; count-vector-elements-beneath already exist elsewhere in this
;; library?  It seems like they might.

;; A regular tree node is guaranteed to have only 32-way branching at
;; all nodes, except perhaps along the right spine, where it can be
;; partial.  From a regular tree node down, all leaf arrays
;; (containing vector elements directly) are restricted to contain a
;; full 32 vector elements.  This code relies on these invariants to
;; quickly calculate the number of vector elements beneath a regular
;; node in O(log N) time.

(defn last-non-nil-idx [^objects arr]
  (loop [i (int (dec (alength arr)))]
    (if (neg? i)
      i
      (if (nil? (aget arr (int i)))
        (recur (unchecked-dec-int i))
        i))))

(defn count-vector-elements-beneath [node shift ^NodeManager nm]
  (if (.regular nm node)
    (loop [node node
           shift shift
           acc 0]
      (if (zero? shift)
        (if (nil? node)
          acc
          ;; The +32 is for the regular leaf node reached at shift 0
          (+ acc 32))
        (let [^objects arr (.array nm node)
              max-child-idx (int (last-non-nil-idx arr))
              num-elems-in-full-child (int (bit-shift-left 1 shift))]
          (if (< max-child-idx 0)
            acc
            (recur (aget ^objects arr max-child-idx)
                   (unchecked-subtract-int shift (int 5))
                   (unchecked-add-int acc (unchecked-multiply-int
                                           max-child-idx
                                           num-elems-in-full-child)))))))
    ;; irregular case
    (let [rngs (ranges nm node)]
      (aget rngs (dec (aget rngs 32))))))

(defn peephole-optimize-root [^Vector v]
  (let [config @peephole-optimization-config]
    (if (<= (.-shift v) 10)
      ;; Tree depth cannot be reduced if shift <= 5.
      ;; TBD: If shift=10, the grandchildren nodes need to be handled
      ;; by an am array manager for primitive vectors, which I haven't
      ;; written code for yet below, but so far this peephole
      ;; optimizer seems to be working sufficiently well without
      ;; handling that case.
      v
      (let [root (.-root v)
            ^NodeManager nm (.-nm v)
            children (child-nodes root nm)
            grandchildren (bounded-grandchildren nm children)
            num-granchildren-bounded (count grandchildren)
            many-grandchildren? (> num-granchildren-bounded 32)]
        (if many-grandchildren?
          ;; If it is possible to reduce tree depth, it requires going
          ;; deeper than just to the grandchildren, which is beyond
          ;; what this peephole optimizer is intended to do.
          v
          ;; Create a new root node that points directly at the
          ;; grandchildren, since there are few enough of them.
          (let [^objects new-arr  (object-array 33)
                ^ints new-rngs (int-array 33)
                new-root (.node nm (.edit nm root) new-arr)
                shift    (.-shift v)
                grandchild-shift (- shift (* 2 5))]
            (swap! peephole-optimization-count inc)
            (loop [idx 0
                   remaining-gc grandchildren
                   elem-sum (int 0)]
              (if-let [remaining-gc (seq remaining-gc)]
                (let [grandchild (first remaining-gc)
                      num-elems-this-grandchild (count-vector-elements-beneath
                                                 grandchild grandchild-shift nm)
                      next-elem-sum (int (+ elem-sum num-elems-this-grandchild))]
                  (aset new-arr idx grandchild)
                  (aset new-rngs idx next-elem-sum)
                  (recur (inc idx) (rest remaining-gc) next-elem-sum))))
            (aset new-rngs 32 num-granchildren-bounded)
            (aset new-arr 32 new-rngs)
            (let [new-v (Vector. nm (.-am v) (.-cnt v) (- shift 5)
                                 new-root (.-tail v) (.-_meta v) 0 0)]
              (when (:debug-fn config)
                ((:debug-fn config) v new-v))
              new-v)))))))

(def max-vector-elements Integer/MAX_VALUE)

;; Larger shift values than 64 definitely break assumptions all over
;; the RRB vector implementation, e.g. (bit-shift-right 255 65)
;; returns the same result as (bit-shift-right 255 1), I believe
;; because the shift amount argument is effectively modulo'd by 64.
;; Larger shift values than 30 are unlikely to make sense, given that
;; the maximum number of vector elements supported is somewhere near
;; Integer/MAX_VALUE=2^31-1.

(defn shift-too-large? [^Vector v]
  (> (.-shift v) 30))

;; The maximum number of vector elements in a tree, not counting any
;; elements in the tail, with a given shift value is:
;;
;; (bit-shift-left 1 (+ shift 5))
;;
;; It is perfectly normal to have vectors with a root tree node with
;; only 1 non-nil child, so at a fraction 1/32 of maximum capacity.  I
;; do not know the exact minimum fraction that RRB vectors as
;; implemented here should allow, but I suspect it is well over
;; 1/1024.

(defn poor-branching? [^Vector v]
  (let [tail-off (.tailoff v)]
    (if (zero? tail-off)
      false
      (let [shift-amount (unchecked-subtract-int (.-shift v) (int 5))
            max-capacity-divided-by-1024 (bit-shift-left 1 shift-amount)]
        (< tail-off max-capacity-divided-by-1024)))))

;; Note 3:

;; Consider checking the performance of an expression like the one
;; used now against the one below:

;;(into (clojure.lang.LazilyPersistentVector/create v1) v2))

;; If the LazilyPersistentVector/create version is faster, it would be
;; good to create a version of the create method that returns
;; primitive vectors when given a primitive vector, and an Object
;; vector when given an Object vector.  The existing method in Clojure
;; core always returns an Object vector, even when given a primitive
;; vector.

;; TBD: Is there any promise about what metadata catvec returns?
;; Always the same as on the first argument?

(def fallback-config (atom {:debug-fn nil}))
(def fallback-to-slow-splice-count1 (atom 0))
(def fallback-to-slow-splice-count2 (atom 0))

(defn fallback-to-slow-splice-if-needed [^Vector v1 ^Vector v2
                                         ^Vector splice-result]
  (let [config @fallback-config]
    (if (or (shift-too-large? splice-result)
            (poor-branching? splice-result))
      (do
        (dbg (str "splice-rrbts result had shift " (.-shift splice-result)
                  " and " (.tailoff splice-result) " elements not counting"
                  " the tail. Falling back to slower method of concatenation."))
        (if (poor-branching? v1)
          ;; The v1 we started with was not good, either.
          (do
            (swap! fallback-to-slow-splice-count1 inc)
            (dbg (str "splice-rrbts first arg had shift " (.-shift v1)
                      " and " (.tailoff v1) " elements not counting"
                      " the tail.  Building the result from scratch."))
            ;: See Note 3
            (let [new-splice-result (-> (empty v1) (into v1) (into v2))]
              (when (:debug-fn config)
                ((:debug-fn config) splice-result new-splice-result))
              new-splice-result))
          ;; Assume that v1 is balanced enough that we can use into to
          ;; add all elements of v2 to it, without problems.
          ;; TBD: That assumption might be incorrect.  Consider
          ;; checking the result of this, too, and fall back again to
          ;; the true case above?
          (let [new-splice-result (into v1 v2)]
            (swap! fallback-to-slow-splice-count2 inc)
            (when (:debug-fn config)
              ((:debug-fn config) splice-result new-splice-result))
            new-splice-result)))
      ;; else the fast result is good
      splice-result)))

(defn splice-rrbts-main [^NodeManager nm ^ArrayManager am ^Vector v1 ^Vector v2]
  (cond
    (zero? (count v1)) v2
    (> (+ (long (count v1)) (long (count v2))) max-vector-elements)
    (let [c1 (long (count v1)), c2 (long (count v2))]
      (throw (IllegalArgumentException.
              (str "Attempted to concatenate two vectors whose total"
                   " number of elements is " (+ c1 c2) ", which is"
                   " larger than the maximum number of elements "
                   max-vector-elements " supported in a vector "))))
    (< (count v2) rrbt-concat-threshold) (into v1 v2)
    :else
    (let [s1 (.-shift v1)
          s2 (.-shift v2)
          r1 (.-root v1)
          o? (overflow? nm r1 s1 (+ (count v1) (- 32 (.alength am (.-tail v1)))))
          r1 (if o?
               (let [tail      (.-tail v1)
                     tail-node (.node nm nil tail)
                     reg?      (and (.regular nm r1) (== (.alength am tail) 32))
                     arr       (object-array (if reg? 32 33))]
                 (aset arr 0 r1)
                 (aset arr 1 (new-path nm am s1 tail-node))
                 (if-not reg?
                   (let [rngs (int-array 33)]
                     (aset rngs 32 2)
                     (aset rngs 0 (- (count v1) (.alength am tail)))
                     (aset rngs 1 (count v1))
                     (aset arr 32 rngs)))
                 (.node nm nil arr))
               (fold-tail nm am r1 s1 (.tailoff v1) (.-tail v1)))
          s1 (if o? (+ s1 5) s1)
          r2 (.-root v2)
          s  (max s1 s2)
          r1 (shift-from-to nm r1 s1 s)
          r2 (shift-from-to nm r2 s2 s)
          transferred-leaves (Box. 0)
          [n1 n2] (zippath nm am
                           s
                           r1 (count v1)
                           r2 (- (count v2) (.alength am (.-tail v2)))
                           transferred-leaves)
          d (.-val transferred-leaves)
          ncnt1   (+ (count v1) d)
          ncnt2   (- (count v2) (.alength am (.-tail v2)) d)
          [n1 n2] (if (identical? n2 r2)
                    (squash-nodes nm s n1 ncnt1 n2 ncnt2)
                    (object-array (list n1 n2)))
          ncnt1   (if n2
                    (int ncnt1)
                    (unchecked-add-int (int ncnt1) (int ncnt2)))
          ncnt2   (if n2
                    (int ncnt2)
                    (int 0))]
      (if n2
        (let [arr      (object-array 33)
              new-root (.node nm nil arr)]
          (aset arr 0 n1)
          (aset arr 1 n2)
          (aset arr 32 (doto (int-array 33)
                         (aset 0 ncnt1)
                         (aset 1 (+ ncnt1 ncnt2))
                         (aset 32 2)))
          (Vector. nm am (+ (count v1) (count v2)) (+ s 5) new-root (.-tail v2)
                   nil 0 0))
        (loop [r n1
               s (int s)]
          (if (and (> s (int 5))
                   (nil? (aget ^objects (.array nm r) 1)))
            (recur (aget ^objects (.array nm r) 0)
                   (unchecked-subtract-int s (int 5)))
            (Vector. nm am (+ (count v1) (count v2)) s r (.-tail v2)
                     nil 0 0)))))))

(defn splice-rrbts [^NodeManager nm ^ArrayManager am ^Vector v1 ^Vector v2]
  (let [r1 (splice-rrbts-main nm am v1 v2)
        r2 (peephole-optimize-root r1)]
    (fallback-to-slow-splice-if-needed v1 v2 r2)))

(defn array-copy [^ArrayManager am from i to j len]
  (loop [i   (int i)
         j   (int j)
         len (int len)]
    (when (pos? len)
      (.aset am to j (.aget am from i))
      (recur (unchecked-inc-int i)
             (unchecked-inc-int j)
             (unchecked-dec-int len)))))

(deftype Transient [^NodeManager nm ^ArrayManager am
                    ^boolean objects?
                    ^:unsynchronized-mutable ^int cnt
                    ^:unsynchronized-mutable ^int shift
                    ^:unsynchronized-mutable root
                    ^:unsynchronized-mutable tail
                    ^:unsynchronized-mutable ^int tidx]
  clojure.lang.Counted
  (count [this]
    (.ensureEditable transient-helper nm root)
    cnt)

  clojure.lang.Indexed
  (nth [this i]
    (.ensureEditable transient-helper nm root)
    (if (and (<= (int 0) i) (< i cnt))
      (let [tail-off (unchecked-subtract-int cnt (.alength am tail))]
        (if (<= tail-off i)
          (.aget am tail (unchecked-subtract-int i tail-off))
          (loop [i i node root shift shift]
            (if (zero? shift)
              (let [arr (.array nm node)]
                (.aget am arr (bit-and (bit-shift-right i shift) (int 0x1f))))
              (if (.regular nm node)
                (let [arr (.array nm node)
                      idx (bit-and (bit-shift-right i shift) (int 0x1f))]
                  (loop [i     i
                         node  (aget ^objects arr idx)
                         shift (unchecked-subtract-int shift (int 5))]
                    (let [arr (.array nm node)
                          idx (bit-and (bit-shift-right i shift) (int 0x1f))]
                      (if (zero? shift)
                        (.aget am arr idx)
                        (recur i
                               (aget ^objects arr idx)
                               (unchecked-subtract-int shift (int 5)))))))
                (let [arr  (.array nm node)
                      rngs (ranges nm node)
                      idx  (loop [j (bit-and (bit-shift-right i shift) (int 0x1f))]
                             (if (< i (aget rngs j))
                               j
                               (recur (unchecked-inc-int j))))
                      i    (if (zero? idx)
                             (int i)
                             (unchecked-subtract-int
                              (int i) (aget rngs (unchecked-dec-int idx))))]
                  (recur i
                         (aget ^objects arr idx)
                         (unchecked-subtract-int shift (int 5)))))))))
      (throw (IndexOutOfBoundsException.))))

  (nth [this i not-found]
    (.ensureEditable transient-helper nm root)
    (if (and (>= i (int 0)) (< i cnt))
      (.nth this i)
      not-found))

  clojure.lang.ILookup
  (valAt [this k not-found]
    (.ensureEditable transient-helper nm root)
    (if (Util/isInteger k)
      (let [i (int k)]
        (if (and (>= i (int 0)) (< i cnt))
          (.nth this i)
          not-found))
      not-found))

  (valAt [this k]
    (.valAt this k nil))

  clojure.lang.IFn
  (invoke [this k]
    (.ensureEditable transient-helper nm root)
    (if (Util/isInteger k)
      (let [i (int k)]
        (if (and (>= i (int 0)) (< i cnt))
          (.nth this i)
          (throw (IndexOutOfBoundsException.))))
      (throw (IllegalArgumentException. "Key must be integer"))))

  (applyTo [this args]
    (.ensureEditable transient-helper nm root)
    (let [n (RT/boundedLength args 1)]
      (case n
        0 (throw (clojure.lang.ArityException.
                  n (.. this (getClass) (getSimpleName))))
        1 (.invoke this (first args))
        2 (throw (clojure.lang.ArityException.
                  n (.. this (getClass) (getSimpleName)))))))

  clojure.lang.ITransientCollection
  (conj [this val]
    (.ensureEditable transient-helper nm root)
    (if (< tidx 32)
      (do (.aset am tail tidx val)
          (set! cnt  (unchecked-inc-int cnt))
          (set! tidx (unchecked-inc-int tidx))
          this)
      (let [tail-node (.node nm (.edit nm root) tail)
            new-tail  (.array am 32)]
        (.aset am new-tail 0 val)
        (set! tail new-tail)
        (set! tidx (int 1))
        (if (overflow? nm root shift cnt)
          (if (.regular nm root)
            (let [new-arr (object-array 32)]
              (doto new-arr
                (aset 0 root)
                (aset 1 (.newPath transient-helper nm am
                                  tail (.edit nm root) shift tail-node)))
              (set! root  (.node nm (.edit nm root) new-arr))
              (set! shift (unchecked-add-int shift (int 5)))
              (set! cnt   (unchecked-inc-int cnt))
              this)
            (let [new-arr  (object-array 33)
                  new-rngs (int-array 33)
                  new-root (.node nm (.edit nm root) new-arr)
                  root-total-range (aget (ranges nm root) 31)]
              (doto new-arr
                (aset 0  root)
                (aset 1  (.newPath transient-helper nm am
                                   tail (.edit nm root) shift tail-node))
                (aset 32 new-rngs))
              (doto new-rngs
                (aset 0  root-total-range)
                (aset 1  (unchecked-add-int root-total-range (int 32)))
                (aset 32 2))
              (set! root  new-root)
              (set! shift (unchecked-add-int shift (int 5)))
              (set! cnt   (unchecked-inc-int cnt))
              this))
          (let [new-root (.pushTail transient-helper nm am
                                    shift cnt (.edit nm root) root tail-node)]
            (set! root new-root)
            (set! cnt  (unchecked-inc-int cnt))
            this)))))

  (persistent [this]
    (.ensureEditable transient-helper nm root)
    (.set (.edit nm root) nil)
    (let [trimmed-tail (.array am tidx)]
      (array-copy am tail 0 trimmed-tail 0 tidx)
      (Vector. nm am cnt shift root trimmed-tail nil 0 0)))

  clojure.lang.ITransientVector
  (assocN [this i val]
    (.ensureEditable transient-helper nm root)
    (cond
      (and (<= 0 i) (< i cnt))
      (let [tail-off (unchecked-subtract-int cnt tidx)]
        (if (<= tail-off i)
          (.aset am tail (unchecked-subtract-int i tail-off) val)
          (set! root (.doAssoc transient-helper nm am
                               shift (.edit nm root) root i val)))
        this)

      (== i cnt) (.conj this val)

      :else (throw (IndexOutOfBoundsException.))))

  (pop [this]
    (.ensureEditable transient-helper nm root)
    (cond
      (zero? cnt)
      (throw (IllegalStateException. "Can't pop empty vector"))

      (== 1 cnt)
      (do (set! cnt (int 0))
          (set! tidx (int 0))
          (if objects?
            (.aset am tail 0 nil))
          this)

      (> tidx 1)
      (do (set! cnt  (unchecked-dec-int cnt))
          (set! tidx (unchecked-dec-int tidx))
          (if objects?
            (.aset am tail tidx nil))
          this)

      :else
      (let [new-tail-base (.arrayFor this (unchecked-subtract-int cnt (int 2)))
            new-tail      (.editableTail transient-helper am new-tail-base)
            new-tidx      (.alength am new-tail-base)
            new-root      (.popTail transient-helper nm am
                                    shift
                                    cnt
                                    (.edit nm root)
                                    root)]
        (cond
          (nil? new-root)
          (do (set! cnt  (unchecked-dec-int cnt))
              (set! root (.ensureEditable transient-helper nm am
                                          (.edit nm root)
                                          (.empty nm)
                                          5))
              (set! tail new-tail)
              (set! tidx new-tidx)
              this)

          (and (> shift 5)
               (nil? (aget ^objects (.array nm new-root) 1)))
          (do (set! cnt   (unchecked-dec-int cnt))
              (set! shift (unchecked-subtract-int shift (int 5)))
              (set! root  (.ensureEditable
                           transient-helper nm am
                           (.edit nm root)
                           (aget ^objects (.array nm new-root) 0)
                           (unchecked-subtract-int shift (int 5))))
              (set! tail  new-tail)
              (set! tidx  new-tidx)
              this)

          :else
          (do (set! cnt  (unchecked-dec-int cnt))
              (set! root new-root)
              (set! tail new-tail)
              (set! tidx new-tidx)
              this)))))

  clojure.lang.ITransientAssociative
  (assoc [this k v]
    (.assocN this k v))

  ;; temporary kludge
  IVecImpl
  (tailoff [_]
    (unchecked-subtract-int cnt tidx))

  (arrayFor [this i]
    (if (and (<= (int 0) i) (< i cnt))
      (if (>= i (.tailoff this))
        tail
        (loop [i (int i) node root shift shift]
          (if (zero? shift)
            (.array nm node)
            (if (.regular nm node)
              (loop [node  (aget ^objects (.array nm node)
                                 (bit-and (bit-shift-right i shift) (int 0x1f)))
                     shift (unchecked-subtract-int shift (int 5))]
                (if (zero? shift)
                  (.array nm node)
                  (recur (aget ^objects (.array nm node)
                               (bit-and (bit-shift-right i shift) (int 0x1f)))
                         (unchecked-subtract-int shift (int 5)))))
              (let [rngs (ranges nm node)
                    j    (loop [j (bit-and (bit-shift-right i shift) (int 0x1f))]
                           (if (< i (aget rngs j))
                             j
                             (recur (unchecked-inc-int j))))
                    i    (if (pos? j)
                           (unchecked-subtract-int
                            i (aget rngs (unchecked-dec-int j)))
                           i)]
                (recur (int i)
                       (aget ^objects (.array nm node) j)
                       (unchecked-subtract-int shift (int 5)))))))) 
      (throw (IndexOutOfBoundsException.))))

  PTransientDebugAccess
  (debugGetRoot [_] root)
  (debugGetShift [_] shift)
  (debugGetTail [_] tail)
  (debugGetCnt [_] cnt))
