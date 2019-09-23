(ns clojure.core.rrb-vector.transients
  (:refer-clojure :exclude [new-path])
  (:require [clojure.core.rrb-vector.nodes
             :refer [regular? clone node-ranges last-range overflow?]]
            [clojure.core.rrb-vector.trees :refer [tail-offset new-path]]))

(defn ensure-editable [edit node]
  (if (identical? (.-edit node) edit)
    node
    (let [new-arr (aclone (.-arr node))]
      (if (== 33 (alength new-arr))
        (aset new-arr 32 (aclone (aget new-arr 32))))
      (VectorNode. edit new-arr))))

(defn editable-root [root]
  (let [new-arr (aclone (.-arr root))]
    (if (== 33 (alength new-arr))
      (aset new-arr 32 (aclone (aget new-arr 32))))
    (VectorNode. (js-obj) new-arr)))

(defn editable-tail [tail]
  (let [ret (make-array 32)]
    (array-copy tail 0 ret 0 (alength tail))
    ret))

;; Note 1: This condition check and exception are a little bit closer
;; to the source of the cause for what was issue CRRBV-20, added in
;; case there is still some remaining way to cause this condition to
;; occur.

;; Note 2: In the worst case, when the tree is nearly full, calling
;; overflow? here takes run time O(tree_depth^2) here.  That could be
;; made O(tree_depth).  One way would be to call push-tail! in hopes
;; that it succeeds, but return some distinctive value indicating a
;; failure on the full condition, and create the node via a new-path
;; call at most recent recursive push-tail! call that has an empty
;; slot available.
(defn push-tail! [shift cnt root-edit current-node tail-node]
  (let [ret (ensure-editable root-edit current-node)]
    (if (regular? ret)
      (do (loop [n ret shift shift]
            (let [arr    (.-arr n)
                  subidx (bit-and (bit-shift-right (dec cnt) shift) 0x1f)]
              (if (== shift 5)
                (aset arr subidx tail-node)
                (let [child (aget arr subidx)]
                  (if (nil? child)
                    (aset arr subidx
                          (new-path (.-arr tail-node)
                                    root-edit
                                    (- shift 5)
                                    tail-node))
                    (let [editable-child (ensure-editable root-edit child)]
                      (aset arr subidx editable-child)
                      (recur editable-child (- shift 5))))))))
          ret)
      (let [arr  (.-arr ret)
            rngs (node-ranges ret)
            li   (dec (aget rngs 32))
            cret (if (== shift 5)
                   nil
                   (let [child (ensure-editable root-edit (aget arr li))
                         ccnt  (+ (if (pos? li)
                                    (- (aget rngs li) (aget rngs (dec li)))
                                    (aget rngs 0))
                                  ;; add 32 elems to account for the
                                  ;; new full tail we plan to add to
                                  ;; the subtree.
                                  32)]
                     ;; See Note 2
                     (if-not (overflow? child (- shift 5) ccnt)
                       (push-tail! (- shift 5) ccnt root-edit
                                   child
                                   tail-node))))]
        (if cret
          (do (aset arr  li cret)
              (aset rngs li (+ (aget rngs li) 32))
              ret)
          (do (when (>= li 31)
                ;; See Note 1
                (let [msg (str "Assigning index " (inc li) " of vector"
                               " object array to become a node, when that"
                               " index should only be used for storing"
                               " range arrays.")
                      data {:shift shift, :cnd cnt,
                            :current-node current-node,
                            :tail-node tail-node, :rngs rngs, :li li,
                            :cret cret}]
                  (throw (ex-info msg data))))
              (aset arr (inc li)
                    (new-path (.-arr tail-node)
                              root-edit
                              (- shift 5)
                              tail-node))
              (aset rngs (inc li) (+ (aget rngs li) 32))
              (aset rngs 32 (inc (aget rngs 32)))
              ret))))))

(defn pop-tail! [shift cnt root-edit current-node]
  (let [ret (ensure-editable root-edit current-node)]
    (if (regular? ret)
      (let [subidx (bit-and (bit-shift-right (- cnt 2) shift) 0x1f)]
        (cond
          (> shift 5)
          (let [child (pop-tail! (- shift 5) cnt root-edit
                                 (aget (.-arr ret) subidx))]
            (if (and (nil? child) (zero? subidx))
              nil
              (let [arr (.-arr ret)]
                (aset arr subidx child)
                ret)))

          (zero? subidx)
          nil

          :else
          (let [arr (.-arr ret)]
            (aset arr subidx nil)
            ret)))
      (let [rngs   (node-ranges ret)
            subidx (dec (aget rngs 32))]
        (cond
          (> shift 5)
          (let [child     (aget (.-arr ret) subidx)
                child-cnt (if (zero? subidx)
                            (aget rngs 0)
                            (- (aget rngs subidx) (aget rngs (dec subidx))))
                new-child (pop-tail! (- shift 5) child-cnt root-edit child)]
            (cond
              (and (nil? new-child) (zero? subidx))
              nil

              (regular? child)
              (let [arr (.-arr ret)]
                (aset rngs subidx (- (aget rngs subidx) 32))
                (aset arr  subidx new-child)
                (if (nil? new-child)
                  (aset rngs 32 (dec (aget rngs 32))))
                ret)

              :else
              (let [rng  (last-range child)
                    diff (- rng (if new-child (last-range new-child) 0))
                    arr  (.-arr ret)]
                (aset rngs subidx (- (aget rngs subidx) diff))
                (aset arr  subidx new-child)
                (if (nil? new-child)
                  (aset rngs 32 (dec (aget rngs 32))))
                ret)))

          (zero? subidx)
          nil

          :else
          (let [arr   (.-arr ret)
                child (aget arr subidx)]
            (aset arr  subidx nil)
            (aset rngs subidx 0)
            (aset rngs 32     (dec (aget rngs 32)))
            ret))))))

(defn do-assoc! [shift root-edit current-node i val]
  (let [ret (ensure-editable root-edit current-node)]
    (if (regular? ret)
      (loop [shift shift
             node  ret]
        (if (zero? shift)
          (let [arr (.-arr node)]
            (aset arr (bit-and i 0x1f) val))
          (let [arr    (.-arr node)
                subidx (bit-and (bit-shift-right i shift) 0x1f)
                child  (ensure-editable root-edit (aget arr subidx))]
            (aset arr subidx child)
            (recur (- shift 5) child))))
      (let [arr    (.-arr ret)
            rngs   (node-ranges ret)
            subidx (bit-and (bit-shift-right i shift) 0x1f)
            subidx (loop [subidx subidx]
                     (if (< i (int (aget rngs subidx)))
                       subidx
                       (recur (inc subidx))))
            i      (if (zero? subidx) i (- i (aget rngs (dec subidx))))]
        (aset arr subidx
              (do-assoc! (- shift 5) root-edit (aget arr subidx) i val))))
    ret))
