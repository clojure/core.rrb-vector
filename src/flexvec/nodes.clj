(ns flexvec.nodes
  (:import (clojure.core VecNode ArrayManager)
           (clojure.lang PersistentVector PersistentVector$Node)
           (java.util.concurrent.atomic AtomicReference)))

;;; array managers

(defmacro mk-am [t]
  (#'clojure.core/mk-am &env &form t))

(definline object [x] x)

(def ams
  (assoc @#'clojure.core/ams :object (mk-am object)))

(def object-am
  (ams :object))

;;; empty nodes

(let [empty-node-field (.getDeclaredField PersistentVector "EMPTY_NODE")]
  (.setAccessible empty-node-field true)
  (def empty-pv-node (.get empty-node-field nil)))

(def empty-gvec-node clojure.core/EMPTY-NODE)

;;; node managers

(let [root-field (doto (.getDeclaredField PersistentVector "root")
                   (.setAccessible true))]
  (defn ^clojure.lang.PersistentVector$Node pv-root [^PersistentVector v]
    (.get root-field v)))

(let [tail-field (doto (.getDeclaredField PersistentVector "tail")
                   (.setAccessible true))]
  (defn pv-tail [^PersistentVector v]
    (.get tail-field v)))

(let [shift-field (doto (.getDeclaredField PersistentVector "shift")
                    (.setAccessible true))]
  (defn pv-shift [^PersistentVector v]
    (.get shift-field v)))

(let [array-field (doto (.getDeclaredField PersistentVector$Node "array")
                    (.setAccessible true))]
  (defn pv-node-array [^PersistentVector$Node node]
    (.get array-field node)))

(let [edit-field (doto (.getDeclaredField PersistentVector$Node "edit")
                   (.setAccessible true))]
  (defn ^AtomicReference pv-node-edit [^PersistentVector$Node node]
    (.get edit-field node)))

(let [node-ctor (.getDeclaredConstructor
                 PersistentVector$Node
                 (into-array Class [AtomicReference (class (object-array 0))]))]
  (.setAccessible node-ctor true)
  (defn make-pv-node [^AtomicReference edit ^objects arr]
    (.newInstance node-ctor (object-array (list edit arr)))))

(definterface NodeManager
  (node [^java.util.concurrent.atomic.AtomicReference edit arr])
  (empty [])
  (array [node])
  (^java.util.concurrent.atomic.AtomicReference edit [node])
  (^boolean regular [node])
  (clone [^clojure.core.ArrayManager am ^int shift node]))

(def object-nm
  (reify NodeManager
    (node [_ edit arr]
      (make-pv-node edit arr))
    (empty [_]
      empty-pv-node)
    (array [_ node]
      (pv-node-array node))
    (edit [_ node]
      (pv-node-edit node))
    (regular [_ node]
      (not (== (alength ^objects (pv-node-array node)) (int 33))))
    (clone [_ am shift node]
      (make-pv-node (pv-node-edit node)
                    (aclone ^objects (pv-node-array node))))))

(def primitive-nm
  (reify NodeManager
    (node [_ edit arr]
      (VecNode. edit arr))
    (empty [_]
      empty-gvec-node)
    (array [_ node]
      (.-arr ^VecNode node))
    (edit [_ node]
      (.-edit ^VecNode node))
    (regular [_ node]
      (not (== (alength ^objects (.-arr ^VecNode node)) (int 33))))
    (clone [_ am shift node]
      (if (zero? shift)
        (VecNode. (.-edit ^VecNode node)
                  (.aclone am (.-arr ^VecNode node)))
        (VecNode. (.-edit ^VecNode node)
                  (aclone ^objects (.-arr ^VecNode node)))))))

;;; ranges

(defmacro ranges [nm node]
  `(ints (aget ~(with-meta `(.array ~nm ~node) {:tag 'objects}) 32)))

(defn last-range [^NodeManager nm node]
  (let [rngs (ranges nm node)
        i    (unchecked-dec-int (aget rngs 32))]
    (aget rngs i)))

(defn regular-ranges [shift cnt]
  (let [step (bit-shift-left (int 1) (int shift))
        rngs (int-array 33)]
    (loop [i (int 0) r step]
      (if (< r cnt)
        (do (aset rngs i r)
            (recur (unchecked-inc-int i) (unchecked-add-int r step)))
        (do (aset rngs i (int cnt))
            (aset rngs 32 (unchecked-inc-int i))
            rngs)))))

;;; root overflow

(defn overflow? [^NodeManager nm root shift cnt]
  (if (.regular nm root)
    (> (bit-shift-right (unchecked-inc-int (int cnt)) (int 5))
       (bit-shift-left (int 1) (int shift)))
    (let [rngs (ranges nm root)
          slc  (aget rngs 32)]
      (and (== slc (int 32))
           (or (== (int shift) (int 5))
               (recur nm
                      (aget ^objects (.array nm root) (unchecked-dec-int slc))
                      (unchecked-subtract-int (int shift) (int 5))
                      (unchecked-subtract-int (aget rngs 31) (aget rngs 30))))))))

;;; find nil / 0

(defn index-of-0 ^long [arr]
  (let [arr (ints arr)]
    (loop [l 0 h 31]
      (if (>= l (unchecked-dec h))
        (if (zero? (aget arr l))
          l
          (if (zero? (aget arr h))
            h
            32))
        (let [mid (unchecked-add l (bit-shift-right (unchecked-subtract h l) 1))]
          (if (zero? (aget arr mid))
            (recur l mid)
            (recur (unchecked-inc-int mid) h)))))))

(defn index-of-nil ^long [arr]
  (loop [l 0 h 31]
    (if (>= l (unchecked-dec h))
      (if (nil? (aget ^objects arr l))
        l
        (if (nil? (aget ^objects arr h))
          h
          32))
      (let [mid (unchecked-add l (bit-shift-right (unchecked-subtract h l) 1))]
        (if (nil? (aget ^objects arr mid))
          (recur l mid)
          (recur (unchecked-inc-int mid) h))))))

;;; children

(defn first-child [^NodeManager nm node]
  (aget ^objects (.array nm node) 0))

(defn last-child [^NodeManager nm node]
  (let [arr (.array nm node)]
    (if (.regular nm node)
      (aget ^objects arr (dec (index-of-nil arr)))
      (aget ^objects arr (unchecked-dec-int (aget (ranges nm node) 32))))))

(defn remove-leftmost-child [^NodeManager nm shift parent]
  (let [arr (.array nm parent)]
    (if (nil? (aget ^objects arr 1))
      nil
      (let [regular? (.regular nm parent)
            new-arr  (object-array (if regular? 32 33))]
        (System/arraycopy arr 1 new-arr 0 31)
        (if-not regular?
          (let [rngs     (ranges nm parent)
                new-rngs (aclone rngs)]
            (System/arraycopy rngs 1 new-rngs 0 (dec (aget rngs 32)))
            (aset new-rngs 32 (dec (aget rngs 32)))
            (aset ^objects new-arr 32 new-rngs)))
        (.node nm (.edit nm parent) new-arr)))))

(defn replace-leftmost-child [^NodeManager nm shift parent pcnt child d]
  (if (.regular nm parent)
    (let [step (bit-shift-left 1 shift)
          rng0 (- step d)
          ncnt (- pcnt d)
          li   (bit-and (bit-shift-right shift (dec pcnt)) 0x1f)]
      (let [arr      (.array nm parent)
            new-arr  (object-array 33)
            new-rngs (int-array 33)]
        (aset ^objects new-arr 0 child)
        (System/arraycopy arr 1 new-arr 1 li)
        (aset ^objects new-arr 32 new-rngs)
        (aset new-rngs 0 (int rng0))
        (aset new-rngs li (int ncnt))
        (aset new-rngs 32 (int (inc li)))
        (loop [i 1]
          (when (<= i li)
            (aset new-rngs i (+ (aget new-rngs (dec i)) step))
            (recur (inc i))))
        (.node nm nil new-arr)))
    (let [new-arr  (aclone ^objects (.array nm parent))
          rngs     (ranges nm parent)
          new-rngs (aclone rngs)
          li       (dec (index-of-0 new-rngs))]
      (aset ^objects new-arr 32 new-rngs)
      (aset ^objects new-arr 0 child)
      (loop [i 0]
        (when (<= i li)
          (aset new-rngs i (- (aget rngs i) (int d)))
          (recur (inc i))))
      (.node nm nil new-arr))))

(defn replace-rightmost-child [^NodeManager nm shift parent child d]
  (if (.regular nm parent)
    (let [arr (.array nm parent)
          i   (unchecked-dec (index-of-nil arr))]
      (if (.regular nm child)
        (let [new-arr (aclone ^objects arr)]
          (aset ^objects new-arr i child)
          (.node nm nil new-arr))
        (let [arr     (.array nm parent)
              new-arr (object-array 33)
              step    (bit-shift-left 1 shift)
              rngs    (int-array 33)]
          (aset rngs 32 (inc i))
          (aset ^objects new-arr 32 rngs)
          (System/arraycopy arr 0 new-arr 0 i)
          (aset ^objects new-arr i child)
          (loop [j 0 r step]
            (when (<= j i)
              (aset rngs j r)
              (recur (inc j) (+ r step))))
          (aset rngs i (int (last-range nm child)))
          (.node nm nil arr))))
    (let [rngs     (ranges nm parent)
          new-rngs (aclone rngs)
          i        (dec (aget rngs 32))
          new-arr  (aclone ^objects (.array nm parent))]
      (aset ^objects new-arr i child)
      (aset ^objects new-arr 32 new-rngs)
      (aset new-rngs i (int (+ (aget rngs i) d)))
      (.node nm nil new-arr))))

;;; fold-tail

(defn new-path [^NodeManager nm ^ArrayManager am shift node]
  (let [reg? (== 32 (.alength am (.array nm node)))
        len  (if reg? 32 33)
        arr  (object-array len)
        rngs (if-not reg?
               (doto (int-array 33)
                 (aset 0 (.alength am (.array nm node)))
                 (aset 32 1)))
        ret  (.node nm nil arr)]
    (loop [arr arr shift shift]
      (if (== shift 5)
        (do (if-not reg?
              (aset arr 32 rngs))
            (aset arr 0 node))
        (let [a (object-array len)
              e (.node nm nil a)]
          (aset arr 0 e)
          (if-not reg?
            (aset arr 32 rngs))
          (recur a (- shift 5)))))
    ret))

(defn fold-tail [^NodeManager nm ^ArrayManager am node shift cnt tail]
  (let [tlen     (.alength am tail)
        reg?     (and (.regular nm node) (== tlen 32))
        arr      (.array nm node)
        li       (index-of-nil arr)
        new-arr  (object-array (if reg? 32 33))
        rngs     (if-not (.regular nm node) (ranges nm node))
        cret     (if (== shift 5)
                   (.node nm nil tail)
                   (fold-tail nm am
                              (aget ^objects arr (dec li))
                              (- shift 5)
                              (if (.regular nm node)
                                (mod cnt (bit-shift-left 1 shift))
                                (let [li (aget rngs 32)]
                                  (if (pos? li)
                                    (unchecked-subtract-int
                                     (aget rngs (unchecked-dec-int li))
                                     (aget rngs (unchecked-subtract-int
                                                 li (int 2))))
                                    (aget rngs (unchecked-dec-int li)))))
                              tail))
        new-rngs (ints (if-not reg?
                         (if rngs
                           (aclone rngs)
                           (regular-ranges shift cnt))))]
    (when-not (and (== shift 5) (== li 32))
      (System/arraycopy arr 0 new-arr 0 li)
      (when-not reg?
        (if (or (nil? cret) (== shift 5))
          (do (aset new-rngs li
                    (+ (if (pos? li)
                         (aget new-rngs (dec li))
                         (int 0))
                       tlen))
              (aset new-rngs 32 (inc li)))
          (do (when (pos? li)
                (aset new-rngs (dec li)
                      (+ (aget new-rngs (dec li)) tlen)))
              (aset new-rngs 32 li))))
      (if-not reg?
        (aset new-arr 32 new-rngs))
      (if (nil? cret)
        (aset new-arr li
              (new-path nm am
                        (unchecked-subtract-int shift 5)
                        (.node nm nil tail)))
        (aset new-arr (if (== shift 5) li (dec li)) cret))
      (.node nm nil new-arr))))
