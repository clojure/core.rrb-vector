(ns clojure.core.rrb-vector.debug
  (:require [clojure.core.rrb-vector.parameters :as p]
            [clojure.core.rrb-vector :as fv]
            [clojure.core.rrb-vector.rrbt :as rrbt]
            ;; This page:
            ;; https://clojure.org/guides/reader_conditionals refers
            ;; to code that can go into common cljc files as platform
            ;; independent, and the code in the clj or cljs files as
            ;; platform dependent, so I will use that terminology
            ;; here, too.
            [clojure.core.rrb-vector.debug-platform-dependent :as pd]))

;; The intent is to keep this file as close to
;; src/main/cljs/clojure/core/rrb_vector/debug.cljs as possible, so
;; that when we start requiring Clojure 1.7.0 and later for this
;; library, this file and that one can be replaced with a common file
;; with the suffix .cljc


;; Functions expected to be defined in the appropriate
;; clojure.core.rrb-vector.debug-platform-dependent namespace:

;; pd/internal-node?
;; pd/persistent-vector?
;; pd/transient-vector?
;; pd/is-vector?
;; pd/dbg-tailoff  (formerly debug-tailoff)
;; pd/dbg-tidx (formerly debug-tailoff for clj, debug-tidx for cljs)
;; pd/format
;; pd/printf
;; pd/unwrap-subvec-accessors-for
;; pd/abbrev-for-type-of [vec-or-node]   (formerly abbrev-type-name, but move type/class call inside)
;; pd/same-coll?   (written already for clj, TBD for cljs)

;; Functions returned from unwrap-subvec-accessors-for that have
;; platform-dependent definitions, but the same general 'kind'
;; arguments and return values, where 'kind' could be: any vector,
;; persistent or transient, or a vector tree node object:

;; get-root - All get-* fns formerly called extract-* in the Java
;;     platform dependent version of the debug namespace.
;; get-shift
;; get-tail
;; get-cnt
;; get-array [node]   - clj (.array nm node)   cljs (.-arr node)
;; get-ranges [node]  - clj (ranges nm node)   cljs (node-ranges node)
;; regular? [node]    - clj (.regular nm node) cljs (regular? node)
;; tail-len [tail]    - clj (.alength am tail) cljs (alength tail)

;; NO: nm am - cljs doesn't need them, and clj only uses them for the
;; last few functions above.

(defn children-summary [node shift get-array get-ranges regular? opts]
  (let [children (get-array node)
        reg? (regular? node)
        rngs (if-not reg? (get-ranges node))
        array-len (count children)
        children-seq (if reg? children (butlast children))
        non-nils (remove nil? children-seq)
        regular-children (filter regular? non-nils)
        num-non-nils (count non-nils)
        num-regular-children (count regular-children)
        num-irregular-children (- num-non-nils num-regular-children)
        num-nils (- (count children-seq) num-non-nils)
        exp-array-len (if reg? 32 33)
        bad-array-len? (not= array-len exp-array-len)]
    ;; 'r' for regular, 'i' for irregular
    ;; For either type of node, its first 32 array elements are broken
    ;; down into:
    ;; # regular children
    ;; # irregular children

    ;; # of nil 'children' not shown, since it will always be 32 minus
    ;; # the total of # regular plus irregular children, unless the
    ;; # array is the wrong size, and in that case a BAD-ARRAY-LEN
    ;; # message will be included in the string.
    (pd/format "%s%d+%d%s" (if reg? "r" "i")
               num-regular-children
               num-irregular-children
               (if bad-array-len?
                 (pd/format " BAD-ARRAY-LEN %d != %d" array-len exp-array-len)
                 ""))))

(defn filter-indexes
  "Return a sequence of all indexes of elements e of coll for
  which (pred e) returns logical true.  0 is the index of the first
  element."
  [pred coll]
  (filter (complement nil?)
          (map-indexed (fn [idx e]
                         (if (pred e)
                           idx))
                       coll)))

(defn dbg-vec
 ([v]
  (dbg-vec v {:max-depth nil   ;; integer to limit depth, nil for unlimited
              ;; force showing tree "fringes" beyond max-depth
              :always-show-fringes false
              ;; show vector elements.  false for only count
              :show-elements true
              ;; show summary of number of children of each node, as
              ;; returned by function children-summary
              :show-children-summary false
              ;; default false means show ranges arrays with their raw
              ;; unprocessed contents.  Use true to show only the
              ;; first n elements, where n=(aget (get-ranges node)
              ;; 32), and to show the 'deltas' between consecutive
              ;; pairs, e.g. if the original is (32 64 96 0 ... 0 3),
              ;; then instead show (32 32 32), which, if the data
              ;; structure is correct, is the number of vector
              ;; elements reachable through each of the node's 3
              ;; children.
              :show-ranges-as-deltas false}))
 ([v opts]
  (let [{:keys [v subvector? subvec-start subvec-end get-root get-shift
                get-tail get-cnt get-array get-ranges regular? tail-len]}
        (pd/unwrap-subvec-accessors-for v)
        root  (get-root v)
        shift (get-shift v)
        tail  (get-tail v)
        cnt   (get-cnt v)]
    (when subvector?
      (pd/printf "SubVector from start %d to end %d of vector:\n"
                 subvec-start subvec-end))
    (letfn [(go [indent shift i node on-left-fringe? on-right-fringe?]
              (when node
                (dotimes [_ indent]
                  (print "  "))
                (pd/printf "%02d:%02d %s" shift i (pd/abbrev-for-type-of node))
                (if (zero? shift)
                  ;; this node has only vector elements as its children
                  (if (:show-elements opts)
                    (print ":" (vec (get-array node)))
                    (print ":" (count (get-array node))
                           "vector elements elided"))
                  ;; else this node has only other nodes as its children
                  (do
                    (when (:show-children-summary opts)
                      (print " ")
                      (print (children-summary node shift get-array get-ranges
                                               regular? opts)))
                    (if (not (regular? node))
                      (if (:show-ranges-as-deltas opts)
                        (let [rngs (get-ranges node)
                              r (aget rngs 32)
                              tmp (map - (take r rngs) (take r (cons 0 rngs)))]
                          (print ":" (seq tmp)))
                        (print ":" (seq (get-ranges node)))))))
                (println)
                (let [no-children? (zero? shift)
                      visit-all-children? (and (not no-children?)
                                               (or (nil? (:max-depth opts))
                                                   (< (inc indent)
                                                      (:max-depth opts))))
                      visit-some-children? (or visit-all-children?
                                               (and (not no-children?)
                                                    (:always-show-fringes opts)
                                                    (or on-left-fringe?
                                                        on-right-fringe?)))]
                  (if visit-some-children?
                    (dorun
                     (let [arr (get-array node)
                           a (if (regular? node) arr (butlast arr))
                           non-nil-idxs (filter-indexes (complement nil?) a)
                           first-non-nil-idx (first non-nil-idxs)
                           last-non-nil-idx (last non-nil-idxs)]
                       (map-indexed
                        (fn [i node]
                          (let [child-on-left-fringe?
                                (and on-left-fringe? (= i first-non-nil-idx))
                                child-on-right-fringe?
                                (and on-right-fringe? (= i last-non-nil-idx))
                                visit-this-child?
                                (or visit-all-children?
                                    (and (:always-show-fringes opts)
                                         (or child-on-left-fringe?
                                             child-on-right-fringe?)))]
                            (if visit-this-child?
                              (go (inc indent) (- shift 5) i node
                                  child-on-left-fringe?
                                  child-on-right-fringe?))))
                        a)))))))]
      (pd/printf "%s (%d elements):\n" (pd/abbrev-for-type-of v) (count v))
      (go 0 shift 0 root true true)
      (println (if (pd/transient-vector? v)
                 (pd/format "tail (tidx %d):" (pd/dbg-tidx v))
                 "tail:")
               (vec tail))))))

(defn first-diff
  "Compare two sequences to see if they have = elements in the same
  order, and both sequences have the same number of elements.  If all
  of those conditions are true, and no exceptions occur while calling
  seq, first, and next on the seqs of xs and ys, then return -1.

  If two elements at the same index in each sequence are found not =
  to each other, or the sequences differ in their number of elements,
  return the index, 0 or larger, at which the first difference occurs.

  If an exception occurs while calling seq, first, or next, throw an
  exception that contains the index at which this exception occurred."
  [xs ys]
  (loop [i 0 xs (seq xs) ys (seq ys)]
    (if (try (and xs ys (= (first xs) (first ys)))
             (catch Exception e
               (.printStackTrace e)
               i))
      (let [xs (try (next xs)
                    (catch Exception e
                      (prn :xs i)
                      (throw e)))
            ys (try (next ys)
                    (catch Exception e
                      (prn :ys i)
                      (throw e)))]
        (recur (inc i) xs ys))
      (if (or xs ys)
        i
        -1))))

;; When using non-default parameters for the tree data structure,
;; e.g. shift-increment not 5, then in test code with calls to
;; checking-* functions, they will be expecting those same non-default
;; parameter values, and will give errors if they are ever given a
;; vector returned by clojure.core/vec, because without changes to
;; Clojure itself, they always have shift-increment 5 and max-branches
;; 32.
;;
;; If we use (fv/vec coll) consistently in the test code, that in many
;; cases returns a core.rrb-vector data structure, but if given a
;; Clojure vector, it still returns that Clojure vector unmodified,
;; which has the same issues for checking-* functions.  By
;; calling (fv/vec (seq coll)) when not using default parameters, we
;; force the return value of cvec to always be a core.rrb-vector data
;; structure.
;;
;; The name 'cvec' is intended to mean "construct a vector", and only
;; intended for use in test code that constructs vectors used as
;; parameters to other functions operating on vectors.
(defn cvec [coll]
  (if (= p/shift-increment 5)
    (clojure.core/vec coll)
    (fv/vec (seq coll))))

(defn slow-into [to from]
  (reduce conj to from))

(defn all-vector-tree-nodes [v]
  (let [{:keys [v get-root get-shift get-array regular?]}
        (pd/unwrap-subvec-accessors-for v)
        root  (get-root v)
        shift (get-shift v)]
    (letfn [(go [depth shift node]
              (if node
                (if (not= shift 0)
                  (cons
                   {:depth depth :shift shift :kind :internal :node node}
                   (apply concat
                          (map (partial go (inc depth) (- shift 5))
                               (let [arr (get-array node)]
                                 (if (regular? node)
                                   arr
                                   (butlast arr))))))
                  (cons {:depth depth :shift shift :kind :internal :node node}
                        (map (fn [x]
                               {:depth (inc depth) :kind :leaf :value x})
                             (get-array node))))))]
      (cons {:depth 0 :kind :base :shift shift :value v}
            (go 1 shift root)))))

;; All nodes that should be internal nodes are one of the internal
;; node types satisfying internal-node?  All nodes that are less
;; than "leaf depth" must be internal nodes, and none of the ones
;; at "leaf depth" should be.  Probably the most general restriction
;; checking for leaf values should be simply that they are any type
;; that is _not_ an internal node type.  They could be objects that
;; return true for is-vector? for example, if a vector is an element
;; of another vector.

(defn leaves-with-internal-node-type [node-infos]
  (filter (fn [node-info]
            (and (= :leaf (:kind node-info))
                 (pd/internal-node? (:node node-info))))
          node-infos))

(defn non-leaves-not-internal-node-type [node-infos]
  (filter (fn [node-info]
            (and (= :internal (:kind node-info))
                 (not (pd/internal-node? (:node node-info)))))
          node-infos))

;; The definition of nth in deftype Vector implies that every
;; descendant of a 'regular' node must also be regular.  That would be
;; a straightforward sanity check to make, to return an error if a
;; non-regular node is found with a regular ancestor in the tree.

(defn basic-node-errors [v]
  (let [{:keys [v get-shift]} (pd/unwrap-subvec-accessors-for v)
        shift (get-shift v)
        nodes (all-vector-tree-nodes v)
        by-kind (group-by :kind nodes)
        leaf-depths (set (map :depth (:leaf by-kind)))
        expected-leaf-depth (+ (quot shift 5) 2)
        max-internal-node-depth (->> (:internal by-kind)
                                     (map :depth)
                                     (apply max))
        ;; Be a little loose in checking here.  If we want to narrow
        ;; it down to one expected answer, we would need to look at
        ;; the tail to see how many elements it has, then use the
        ;; different between (count v) and that to determine how many
        ;; nodes are in the rest of the tree, whether it is 0 or
        ;; non-0.
        expected-internal-max-depths
        (cond
          (= (count v) 0) #{(- expected-leaf-depth 2)}
          (> (count v) 33) #{(dec expected-leaf-depth)}
          :else #{(dec expected-leaf-depth)
                  (- expected-leaf-depth 2)})]
    (cond
      (not= (mod shift 5) 0)
      {:error true
       :description (str "shift value in root must be a multiple of 5.  Found "
                         shift)
       :data shift}

      ;; It is OK for this set size to be 0 if no leaves, but if there
      ;; are leaves, they should all be at the same depth.
      (> (count leaf-depths) 1)
      {:error true
       :description (str "There are leaf nodes at multiple different depths: "
                         leaf-depths)
       :data leaf-depths}

      (and (= (count leaf-depths) 1)
           (not= (first leaf-depths) expected-leaf-depth))
      {:error true
       :description (str "Expecting all leaves to be at depth " expected-leaf-depth
                         " because root has shift=" shift
                         " but found leaves at depth " (first leaf-depths))
       :data leaf-depths}

      (not (contains? expected-internal-max-depths max-internal-node-depth))
      {:error true
       :description (str "Expecting there to be some internal nodes at one of"
                         " these depths: "
                         expected-internal-max-depths
                         " because count=" (count v)
                         " and root has shift=" shift
                         " but max depth among all internal nodes found was "
                         max-internal-node-depth)}

      (seq (leaves-with-internal-node-type nodes))
      {:error true
       :description "A leaf (at max depth) has one of the internal node types, returning true for internal-node?"
       :data (first (leaves-with-internal-node-type nodes))}

      (seq (non-leaves-not-internal-node-type nodes))
      {:error true
       :description "A non-leaf node has a type that returns false for internal-node?"
       :data (first (non-leaves-not-internal-node-type nodes))}

      :else
      {:error false})))

;; I believe that objects-in-slot-32-of-obj-arrays and
;; ranges-not-int-array are only called directly from one test
;; namespace right now.  Consider making a combined invariant checking
;; function in this debug namespace that can be used from any test
;; namespace (or other debug-time code) that a developer wants to.

(defn objects-in-slot-32-of-obj-arrays
  "Function to look for errors of the form where a node's node.array
  object, which is often an array of 32 or 33 java.lang.Object's, has
  an element at index 32 that is not nil, and refers to an object that
  is of any type _except_ an array of ints.  There appears to be some
  situation in which this can occur, but it seems to almost certainly
  be a bug if that happens, and we should be able to detect it
  whenever it occurs."
  [v]
  (let [{:keys [v get-array]} (pd/unwrap-subvec-accessors-for v)
        node-maps (all-vector-tree-nodes v)
        internal (filter #(= :internal (:kind %)) node-maps)]
    (keep (fn [node-info]
            ;; TBD: Is there a way to do ^objects type hint for clj,
            ;; but none for cljs?  Is it harmful for cljs to have such
            ;; a type hint?
            ;;(let [^objects arr (get-array (:node node-info))
            (let [arr (get-array (:node node-info))
                  n (count arr)]
              (if (== n 33)
                (aget arr 32))))
          internal)))

;; TBD: Should this function be defined in platform-specific file?
;;(defn ranges-not-int-array [x]
;;  (seq (remove int-array? (objects-in-slot-32-of-obj-arrays x))))


;; edit-nodes-errors is completely defined in platform-specific source
;; files.  It is simply quite different between clj/cljs.
(defn edit-nodes-errors [v]
  (pd/edit-nodes-errors v all-vector-tree-nodes))


(defn regular-node-errors [root-node? root-node-cnt children]
  ;; For regular nodes, there should be zero or more 'full' children,
  ;; followed optionally by one 'partial' child, followed by nils.
  (let [[full-children others] (split-with :full? children)
        [partial-children others] (split-with #(and (not (:full? %))
                                                    (not= :nil (:kind %)))
                                              others)
        [nil-children others] (split-with #(= :nil (:kind %)) others)
        num-full (count full-children)
        num-partial (count partial-children)
        num-non-nil (+ num-full num-partial)]
    (cond
      (not= 0 (count others))
      {:error true, :kind :internal,
       :description (str "Found internal regular node with "
                         num-full " full, " num-partial " partial, "
                         (count nil-children) " nil, "
                         (count others) " 'other' children."
                         " - expected 0 children after nils.")}
      (> num-partial 1)
      {:error true, :kind :internal,
       :description (str "Found internal regular node with "
                         num-full " full, " num-partial " partial, "
                         (count nil-children) " nil children"
                         " - expected 0 or 1 partial.")}
      (not (or (and root-node?
                    (<= root-node-cnt 32)  ;; all elements in tail
                    (= 0 num-non-nil))
               (<= 1 num-non-nil 32)))
      {:error true, :kind :internal
       :description
       (str "Found internal regular node with # full + # partial=" num-non-nil
            " children outside of range [1, " 32 "]."
            " root-node?=" root-node? " root-node-cnt=" root-node-cnt)
       :data children}
      :else
      {:error false, :kind :internal,
       :full? (= 32 (count full-children))
       :count (reduce + (map #(or (:count %) 0) children))})))


(defn non-regular-node-errors [node get-ranges children]
  (let [rng (get-ranges node)
        [non-nil-children others] (split-with #(not= :nil (:kind %)) children)
        [nil-children others] (split-with #(= :nil (:kind %)) others)
        num-non-nil (count non-nil-children)
        num-nil (count nil-children)
        expected-ranges (reductions + (map :count non-nil-children))]
    (cond
      (not= 0 (count others))
      {:error true, :kind :internal,
       :description (str "Found internal non-regular node with "
                         num-non-nil " non-nil, " num-nil " nil, "
                         (count others) " 'other' children."
                         " - expected 0 children after nils.")}
      (not= num-non-nil (aget rng 32))
      {:error true, :kind :internal,
       :description (str "Found internal non-regular node with "
                         num-non-nil " non-nil, " num-nil " nil children, and"
                         " last elem of ranges=" (aget rng 32)
                         " - expected it to match # non-nil children.")}
      (not= expected-ranges (take (count expected-ranges) (seq rng)))
      {:error true, :kind :internal,
       :description (str "Found internal non-regular node with "
                         num-non-nil " non-nil, " num-nil " nil children, and"
                         " # children prefix sums: " (seq expected-ranges)
                         " - expected that to match stored ranges: "
                         (seq rng))}
      ;; I believe that there must always be at least one
      ;; non-nil-child.  By checking for this condition, we will
      ;; definitely find out if it is ever violated.
      ;; TBD: What if we have a tree with ranges, and then remove all
      ;; elements?  Does the resulting tree triger this error?
      (not (<= 1 (aget rng 32) 32))
      {:error true, :kind :internal
       :description (str "Found internal non-regular node with (aget rng 32)"
                         "=" (aget rng 32) " outside of range [1, 32].")}
      :else
      {:error false, :kind :internal, :full? false,
       :count (last expected-ranges)})))


(defn max-capacity-divided-by-1024 [root-shift]
  (let [shift-amount (max 0 (- root-shift 5))]
    (bit-shift-left 1 shift-amount)))


(defn fraction-full [v]
  (let [{:keys [v get-shift]} (pd/unwrap-subvec-accessors-for v)
        root-shift (get-shift v)
        tail-off (pd/dbg-tailoff v)
        max-tree-cap (bit-shift-left 1 (+ root-shift 5))]
    (/ (* 1.0 tail-off) max-tree-cap)))


(defn ranges-errors [v]
  (let [{:keys [v get-root get-shift get-tail get-cnt get-array get-ranges
                regular? tail-len]}
        (pd/unwrap-subvec-accessors-for v)
        root  (get-root v)
        root-node-cnt (count v)
        root-shift (get-shift v)
        tail-off (pd/dbg-tailoff v)
        tail (get-tail v)]
    (letfn [
      (go [shift node]
        (cond
          (nil? node) {:error false :kind :nil}
          (zero? shift) (let [n (count (get-array node))]
                          (merge {:error (zero? n), :kind :leaves,
                                  :full? (= n 32), :count n}
                                 (if (zero? n)
                                   {:description
                                    (str "Leaf array has 0 elements."
                                         "  Expected > 0.")})))
          :else ;; non-0 shift
          (let [children (map (partial go (- shift 5))
                              (let [arr (get-array node)]
                                (if (regular? node)
                                  arr
                                  (butlast arr))))
                errs (filter :error children)]
            (cond
              (seq errs) {:error true, :description "One or more errors found",
                          :data errs}
              (not= 32 (count children))
              {:error true, :kind :internal,
               :description (str "Found internal node that has "
                                 (count children) " children - expected 32.")}
              (regular? node) (regular-node-errors (= shift root-shift)
                                                   root-node-cnt children)
              :else (non-regular-node-errors node get-ranges children)))))]
      (let [x (go root-shift root)]
        (cond
          (:error x) x
          (not= tail-off (:count x))
          {:error true, :kind :root,
           :description (str "Found tail-off=" tail-off " != " (:count x)
                             "=count of values beneath internal nodes")
           :internal-node-leaf-count (:count x) :tail-off tail-off
           :cnt (get-cnt v)}
          (and (pd/transient-vector? v)
               (not= (tail-len tail) 32))
          {:error true, :kind :root,
           :description (str "Found transient vector with tail length "
                             (tail-len tail) " - expecting 32")}
          ;; It is always a bad thing if shift becomes more than 32,
          ;; because the bit-shift-left and bit-shift-right operations
          ;; on 32-bit ints actually behave like (bit-shift-left
          ;; x (mod shift-amount 32)) for shift-amount over 32.  It is
          ;; also likely a bug in the implementation if that happens.
          (>= root-shift 32)
          {:error true, :kind :root,
           :description (str "shift of root is " root-shift " >= 32,"
                             " which is not supported.")}
          ;; This is not necessarily a bug, but it seems likely to be
          ;; a bug if a tree is less than 1/1024 full compared to its
          ;; max capacity.  1/32 full is normal when a tree becomes 1
          ;; deeper than it was before.
          (< 0 (:count x) (max-capacity-divided-by-1024 root-shift))
          {:error false, :warning true, :kind :root-too-deep,
           :description (str "For root shift=" root-shift " the maximum "
                             "capacity divided by 1024 is "
                             (max-capacity-divided-by-1024 root-shift)
                             " but the tree contains only "
                             (:count x) " vector elements outside of the tail")}
          :else x)))))

#_(defn add-return-value-checks [f err-desc-str return-value-check-fn]
  (fn [& args]
    (let [ret (apply f args)]
      (apply return-value-check-fn err-desc-str ret args)
      ret)))

(defn copying-seq [v]
  (let [{:keys [v subvector? subvec-start subvec-end
                get-root get-shift get-tail get-array regular?]}
        (pd/unwrap-subvec-accessors-for v)
        root  (get-root v)
        shift (get-shift v)]
    (letfn [(go [shift node]
              (if node
                (if (not= shift 0)
                  (apply concat
                         (map (partial go (- shift 5))
                              (let [arr (get-array node)]
                                (if (regular? node)
                                  arr
                                  (butlast arr)))))
                  (seq (get-array node)))))]
      (doall  ;; always return a fully realized sequence.
       (let [all-elems (concat (go shift root)
                               (if (pd/transient-vector? v)
                                 (take (pd/dbg-tidx v) (get-tail v))
                                 (seq (get-tail v))))]
         (if subvector?
           (take (- subvec-end subvec-start) (drop subvec-start all-elems))
           all-elems))))))


(def failure-data (atom []))
(def warning-data (atom []))

(defn clear-failure-data! []
  (reset! failure-data []))

(let [orig-conj clojure.core/conj]
  (defn record-failure-data [d]
    (swap! failure-data orig-conj d))
  (defn record-warning-data [d]
    (swap! warning-data orig-conj d)))

;; I would like to achieve a goal of providing an easy-to-use way that
;; a Clojure or ClojureScript developer could call a function, or
;; invoke their own code in a macro, and then within the run-time
;; scope of that, a selected set of calls to functions like conj,
;; conj!, pop, pop!, transient, subvec, slicev, catvec, splicev, and
;; perhaps others, would have extra checks enabled, such that if they
;; detected a bug, they would stop the execution immediately with a
;; lot of debug information recorded as near to the point of the
;; failure as can be achieved by checking the return values of such
;; function calls.

;; It would also be good if this goal could be achieved without having
;; a separate implementation of all of those functions, and/or custom
;; versions of Clojure, ClojureScript, or the core.rrb-vector library
;; to use.  Actually a separate implementation of core.rrb-vector
;; might be acceptable and reasonable to implement and maintain, but
;; separate versions of Clojure and ClojureScript seems like too much
;; effort for the benefits achieved.

;; I have investigated approaches that attempt to use with-redefs on
;; the 'original Vars' in Clojure, and also in a ClojureScript
;; Node-based REPL.

;; There are differences between with-redefs behavior on functions in
;; clojure.core between Clojure and ClojureScript, because
;; direct-linking seems to also include user code calling to
;; clojure.core functions with ClojureScript:
;; https://clojure.atlassian.net/projects/CLJS/issues/CLJS-3154

;; At least in Clojure, and perhaps also in ClojureScript, there is
;; sometimes an effect similar to direct linking involved when calling
;; protocol methods on objects defined via deftype.  That prevents
;; with-redefs, and any technique that changes the definition of a Var
;; with alter-var-root! or set!, from causing the alternate function
;; to be called.

;; Here are the code paths that I think are most useful for debug
;; checks of operations on vectors.

;; Functions in clojure.core:

;; Lower value, because they are simpler functions, and in particular
;; do not operate on RRB vector trees with ranges inside:
;; vec vector vector-of

;; Similarly the RRB vector variants of those functions create regular
;; RRB vectors, so not as likely to have bugs.

;; peek can operate on trees with ranges inside, but always accesses
;; the tail, so not nearly as likely to have bugs.

;; Higher value, because they can operate on RRB vectors with ranges
;; inside the tree:

;; conj pop assoc
;; conj! pop! assoc!
;; transient persistent!
;; seq rseq

;; Functions in clojure.core.rrb-vector namespace, and internal
;; implementation functions/protocol-methods that they use:

;; defn fv/catvec
;;   calls itself recursively for many args (clj and cljs versions)
;;   -splicev protocol function (splicev for clj)
;;     When -splicev is called on PersistentVector or Subvec, -as-rrbt
;;       converts it to Vector, then method below is called.
;;     deftype Vector -splicev / splicev method
;;       -as-rrbt (cljs) / as-rrbt (clj)
;;         -slicev (cljs) / slicev (clj) if used on a subvector object
;;       defn splice-rrbts
;;         defn splice-rrbts-main
;;           Calls many internal implementation detail functions.
;;         peephole-optimize-root
;;         fallback-to-slow-splice-if-needed

;; defn fv/subvec
;;   -slicev (cljs) / slicev (clj) protocol function
;;     deftype Vector -slicev method
;;       Calls many internal implementation detail functions,
;;       e.g. slice-left slice-right make-array array-copy etc.

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; See the documentation of the several checking-* functions for the
;; keys supported inside of the @debug-opts map.

(def debug-opts (atom {}))

(def full-debug-opts {:trace false
                      :validate true
                      :return-value-checks
                      [edit-nodes-errors
                       basic-node-errors
                       ranges-errors]
                      ;; false -> throw an exception when error detected
                      :continue-on-error false
                      ;; true -> do not throw an exception when warning found
                      :continue-on-warning true})

(defn set-debug-opts!
  "set-debug-opts! modified the debug-opts atom of the core.rrb-vector
  library, which configures what kinds of extra checks are performed
  when calling the checking-* versions of functions defined in the
  library's debug namespace.

  Example call:

    (require '[clojure.core.rrb-vector.debug :as d])
    (d/set-debug-opts! d/full-debug-opts)

  This call enables as thorough of extra verification checks as is
  supported by existing code, when you call any of the checking-*
  variants of the functions in this namespace, e.g. checking-catvec,
  checking-subvec.

  It will also slow down your code to do so.  checking-* functions
  return the same values as their non checking-* original functions
  they are based upon, so you can write application code that mixes
  calls to both, calling the checking-* versions only occasionally, if
  you have a long sequence of operations that you want to look for
  bugs within core.rrb-vector's implementation of."
  [opts]
  (reset! debug-opts
          {:catvec opts        ;; affects checking-catvec behavior,
                               ;; via calling checking-splicev and
                               ;; checking-splice-rrbts and enabling
                               ;; their extra checks.
           :subvec opts        ;; affects checking-subvec behavior,
                               ;; via calling checking-slicev and
                               ;; enabling its extra checks
           :pop opts           ;; affects checking-pop
           :pop! opts          ;; affects checking-pop!
           :transient opts}))  ;; affects checking-transient

(defn validation-failure [err-msg-str failure-data opts]
  (println "ERROR:" err-msg-str)
  (record-failure-data failure-data)
  (when-not (:continue-on-error opts)
    (throw (ex-info err-msg-str failure-data))))

(defn sanity-check-vector-internals
  "This function is called by all of the checking-* variants of
  functions in the debug namespace.  It calls all of the functions
  in (:return-value-checks opts) in the order given, passing each of
  those functions a return value 'ret'.  Each function performs sanity
  checks on the 'ret' data structure used to represent the vector.

  Those functions should return a map with key :error having a logical
  true value if any errors were found, or a key :warning having a
  logical true value if any warnings were found, otherwise both of
  those values must be logical false in the returned map (or no such
  key is present in the returned map at all).

  Three examples of such functions are included in core.rrb-vector's
  debug namespace.

  * edit-nodes-errors
  * basic-node-errors
  * ranges-errors

  They each look for different problems in the vector data structure
  internals.  They were developed as separate functions in case there
  was ever a significant performance advantage to configuring only
  some of them to be called, not all of them, for long tests.

  If any errors are found, this function calls record-failure-data, to
  record the details in a global atom.  It prints a message to *out*,
  and if (:continue-on-error opts) is logical false, it throws a data
  conveying exception using ex-info containing the same message, and
  the same error details map passed to record-failure-data.

  If no exception is thrown due to an error, then repeat the same
  checks for a warning message, recording details via calling
  record-warning-data, and throwing an exception
  if (:continue-on-warning opts) is logical false."
  [err-desc-str ret args opts]
  (doseq [check-fn (:return-value-checks opts)]
    (let [i (check-fn ret)]
      (when (:error i)
        (let [msg (str "found error in ret value from " err-desc-str
                       ": " (:description i))
              failure-data {:err-desc-str err-desc-str, :ret ret,
                            :args args, :error-info i}]
          (println "ERROR:" msg)
          (record-failure-data failure-data)
          (when-not (:continue-on-error opts)
            (throw (ex-info msg failure-data)))))
      (when (:warning i)
        ;; It is perfectly normal for fv/subvec and slicev to return a
        ;; vector that causes this warning.
        (when-not (and (= err-desc-str "slicev")
                       (= :root-too-deep (:kind i)))
          (let [msg (str "possible issue with ret value from " err-desc-str
                         ": " (:description i))
                failure-data {:err-desc-str err-desc-str, :ret ret,
                              :args args, :error-info i}]
            (println "WARNING:" msg)
            (record-warning-data failure-data)
            (when-not (:continue-on-warning opts)
              (throw (ex-info msg failure-data)))))))))

(defn validating-pop
  "validating-pop is not really designed to be called from user
  programs.  checking-pop can do everything that validating-pop can,
  and more.  See its documentation.

  A typical way of calling validating-pop is:

      (require '[clojure.core.rrb-vector.debug :as d])
      (d/validating-pop clojure.core/pop \"pop\" coll)

  Most of the validating-* functions behave similarly.  This one
  contains the most complete documentation, and the others refer to
  this one.  They all differ in the function that they are intended to
  validate, and a few other details, which will be collected in one
  place here for function validating-pop so one can quickly see the
  differences between validating-pop and the other validating-*
  functions.

      good example f: clojure.core/pop
      opts map: (get @d/debug-opts :pop)

  The first argument can be any function f.  f is expected to take
  arguments and return a value equal to what clojure.core/pop would,
  given the argument coll.

  validating-pop will first make a copy of the seq of items in coll,
  as a safety precaution, because some kinds of incorrect
  implementations of pop could mutate their input argument.  That
  would be a bug, of course, but aiding a developer in detecting bugs
  is the reason validating-pop exists.  It uses the function
  copying-seq to do this, which takes at least linear time in the size
  of coll.

  It will then calculate a sequence that is = to the expected return
  value, e.g. for pop, all items in coll except the last one.

  Then validating-pop will call (f coll), then call copying-seq on the
  return value.

  If the expected and returned sequences are not =, then a map
  containing details about the arguments and actual return value is
  created and passed to d/record-failure-data, which appends the map
  to the end of a vector that is the value of an atom named
  d/failure-data.  An exception is thrown if (:continue-on-error opts)
  is logical false, with ex-data equal to this same map of error data.

  If the expected and actual sequences are the same, no state is
  modified and no exception is thrown.

  If validating-pop does not throw an exception, the return value is
  (f coll)."
  [f err-desc-str coll]
  (let [coll-seq (copying-seq coll)
        exp-ret-seq (butlast coll-seq)
        ret (f coll)
        ret-seq (copying-seq ret)]
    (when (not= ret-seq exp-ret-seq)
      (validation-failure
       "(pop coll) returned incorrect value"
       {:err-desc-str err-desc-str, :ret ret,
        :args (list coll),
        :coll-seq coll-seq, :ret-seq ret-seq,
        :exp-ret-seq exp-ret-seq}
       (get @debug-opts :pop)))
    ret))

(defn checking-pop
  "These two namespace aliases will be used later in this
  documentation:

      (require '[clojure.core.rrb-vector.debug :as d])
      (require '[clojure.core.rrb-vector.debug-platform-dependent :as pd])

  checking-pop passes its argument to clojure.core/pop, and if it
  returns, it returns whatever clojure.core/pop does.  If checking-pop
  detects any problems, it will record information about the problems
  found in one or both of the global atoms 'd/failure-data' and
  'd/warning-data', and optionally throw an exception.

  If coll is not a vector type according to pd/is-vector?, then
  checking-pop simply behaves exactly like clojure.core/pop, with no
  additional checks performed.  All of checking-pop's extra checks are
  specific to vectors.

  If coll is a vector, then checking-pop looks up the key :pop in a
  global atom 'd/debug-opts'.  The result of that lookup is a map we
  will call 'opts' below.

      opts map: (get @d/debug-opts :pop)
      function called if (:validating opts) is logical true:
          validating-pop

  If (:trace opts) is true, then a debug trace message is printed to
  *out*.

  If (:validate opts) is true, then validating-pop is called, using
  clojure.core/pop to do the real work, but validating-pop will check
  whether the return value looks correct relative to the input
  parameter value, i.e. it is equal to a sequence of values containing
  all but the last element of the input coll's sequence of values.
  See validating-pop documentation for additional details.  This step
  records details of problems found in the atoms d/failure-data.

  (:return-value-checks opts) should be a sequence of functions that
  each take the vector returned from calling clojure.core/pop, and
  return data about any errors or warnings they find in the internals
  of the vector data structure.  Errors or warnings are appended to
  atoms d/failure-data and/or d/warning-data.

  If either the validate or return value checks steps find an error,
  they throw an exception if (:continue-on-error opts) is logical
  false.

  If the return value checks step finds no error, but does find a
  warning, it throws an exception if (:continue-on-warning opts) is
  logical false."
  [coll]
  (if-not (pd/is-vector? coll)
    (clojure.core/pop coll)
    (let [opts (get @debug-opts :pop)
          err-desc-str "pop"]
      (when (:trace opts)
        (println "checking-pop called with #v=" (count coll)
                 "(type v)=" (type coll)))
      (let [ret (if (:validate opts)
                  (validating-pop clojure.core/pop err-desc-str coll)
                  (clojure.core/pop coll))]
        (sanity-check-vector-internals err-desc-str ret [coll] opts)
        ret))))

(defn validating-pop!
  "validating-pop! behaves the same as validating-pop, with the
  differences described here.  See validating-pop for details.
  
      good example f: clojure.core/pop!
      opts map: (get @d/debug-opts :pop!)

  If no exception is thrown, the return value is (f coll)."
  [f err-desc-str coll]
  (let [coll-seq (copying-seq coll)
        exp-ret-seq (butlast coll-seq)
        ret (f coll)
        ret-seq (copying-seq ret)]
    (when (not= ret-seq exp-ret-seq)
      (validation-failure
       "(pop! coll) returned incorrect value"
       {:err-desc-str err-desc-str, :ret ret,
        :args (list coll),
        :coll-seq coll-seq, :ret-seq ret-seq,
        :exp-ret-seq exp-ret-seq}
       (get @debug-opts :pop!)))
    ret))

(defn checking-pop!
  "checking-pop! is similar to checking-pop, with the differences
  summarized below.  See checking-pop documentation for details.

      opts map: (get @d/debug-opts :pop!)
      function called if (:validating opts) is logical true:
          validating-pop!"
  [coll]
  (if-not (pd/is-vector? coll)
    (clojure.core/pop! coll)
    (let [opts (get @debug-opts :pop!)
          err-desc-str "pop!"]
      (when (:trace opts)
        (println "checking-pop! called with #v=" (count coll)
                 "(type v)=" (type coll)))
      (let [ret (if (:validate opts)
                  (validating-pop! clojure.core/pop! err-desc-str coll)
                  (clojure.core/pop! coll))]
        (sanity-check-vector-internals err-desc-str ret [coll] opts)
        ret))))

(defn validating-transient
  "validating-transient behaves the same as validating-pop, with the
  differences described here.  See validating-pop for details.
  
      good example f: clojure.core/transient
      opts map: (get @d/debug-opts :transient)

  If no exception is thrown, the return value is (f coll)."
  [f err-desc-str coll]
  (let [coll-seq (copying-seq coll)
        exp-ret-seq coll-seq
        ret (f coll)
        ret-seq (copying-seq ret)]
    (when (not= ret-seq exp-ret-seq)
      (validation-failure
       "(transient coll) returned incorrect value"
       {:err-desc-str err-desc-str, :ret ret,
        :args (list coll),
        :coll-seq coll-seq, :ret-seq ret-seq,
        :exp-ret-seq exp-ret-seq}
       (get @debug-opts :transient)))
    ret))

(defn checking-transient
  "checking-transient is similar to checking-pop, with the differences
  summarized below.  See checking-pop documentation for details.

      opts map: (get @d/debug-opts :transient)
      function called if (:validating opts) is logical true:
          validating-transient"
  [coll]
  (if-not (pd/is-vector? coll)
    (clojure.core/transient coll)
    (let [opts (get @debug-opts :transient)
          err-desc-str "transient"]
      (when (:trace opts)
        (println "checking-transient called with #v=" (count coll)
                 "(type v)=" (type coll)))
      (let [ret (if (:validate opts)
                  (validating-transient clojure.core/transient err-desc-str
                                        coll)
                  (clojure.core/transient coll))]
        (sanity-check-vector-internals err-desc-str ret [coll] opts)
        ret))))

(defn validating-splice-rrbts-main
  "validating-splice-rrbts-main behaves the same as validating-pop, with
  the differences described here.  See validating-pop for details.
  
      good example f: clojure.core.rrb-vector.rrbt/splice-rrbts-main
      opts map: (get @d/debug-opts :catvec)  ;; _not_ :splice-rrbts-main

  Given that splice-rrbts-main is an internal implementation detail of
  the core.rrb-vector library, it is expected that it is more likely
  you would call validating-catvec instead of this function.

  If no exception is thrown, the return value is (f v1 v2)."
  [err-desc-str nm am v1 v2]
  (let [orig-fn rrbt/splice-rrbts-main
        v1-seq (copying-seq v1)
        v2-seq (copying-seq v2)
        exp-ret-seq (concat v1-seq v2-seq)
        ret (orig-fn nm am v1 v2)
        ret-seq (copying-seq ret)]
    (when (not= ret-seq exp-ret-seq)
      (validation-failure
       "splice-rrbts-main returned incorrect value"
       {:err-desc-str err-desc-str, :ret ret,
        :args (list nm am v1 v2)
        :v1-seq v1-seq, :v2-seq v2-seq, :ret-seq ret-seq,
        :exp-ret-seq exp-ret-seq}
       (get @debug-opts :catvec)))
    ret))

(defn checking-splice-rrbts-main
  "checking-splice-rrbts-main is similar to checking-pop, with the
  differences summarized below.  See checking-pop documentation for
  details.

  Unlike checking-pop, it seems unlikely that a user of
  core.rrb-vector would want to call this function directly.  See
  checking-catvec.  checking-splice-rrbts-main is part of the
  implementation of checking-catvec.

      opts map: (get @d/debug-opts :catvec)  ;; _not_ :splice-rrbts-main
      function called if (:validating opts) is logical true:
          validating-splice-rrbts-main"
  [& args]
  (let [opts (get @debug-opts :catvec)
        err-desc-str "splice-rrbts-main"]
    (when (:trace opts)
      (let [[_ _ v1 v2] args]
        (println "checking-splice-rrbts-main called with #v1=" (count v1)
                 "#v2=" (count v2)
                 "(type v1)=" (type v1)
                 "(type v2)=" (type v2))))
    (let [ret (if (:validate opts)
                (apply validating-splice-rrbts-main err-desc-str args)
                (apply rrbt/splice-rrbts-main args))]
      (sanity-check-vector-internals err-desc-str ret args opts)
      ret)))

(defn checking-splice-rrbts
  "checking-splice-rrbts is similar to checking-pop, with the
  differences summarized below.  See checking-pop documentation for
  details.

  Unlike checking-pop, it seems unlikely that a user of
  core.rrb-vector would want to call this function directly.  See
  checking-catvec.  checking-splice-rrbts is part of the
  implementation of checking-catvec.

      opts map: (get @d/debug-opts :catvec)  ;; _not_ :splice-rrbts
      function called if (:validating opts) is logical true:
          validating-splice-rrbts"
  [& args]
  (let [opts (get @debug-opts :catvec)
        err-desc-str1 "splice-rrbts checking peephole-optimize-root result"
        err-desc-str2 "splice-rrbts checking fallback-to-slow-splice-if-needed result"
        [nm am v1 v2] args]
    (when (:trace opts)
      (println "checking-splice-rrbts called with #v1=" (count v1)
               "#v2=" (count v2)
               "(type v1)=" (type v1)
               "(type v2)=" (type v2)))
    (let [r1 (checking-splice-rrbts-main nm am v1 v2)
          r2 (rrbt/peephole-optimize-root r1)]
      ;; Optimize a bit by only doing all of the sanity checks on r2
      ;; if it is not the same identical data structure r1 that
      ;; checking-splice-rrbts-main already checked.
      (when-not (identical? r2 r1)
        (sanity-check-vector-internals err-desc-str1 r2 args opts))
      (let [r3 (rrbt/fallback-to-slow-splice-if-needed v1 v2 r2)]
        (when-not (identical? r3 r2)
          (sanity-check-vector-internals err-desc-str2 r3 args opts))
        r3))))

(defn checking-splicev
  "checking-splicev is identical to splicev, except that it calls
  checking-splice-rrbts instead of splice-rrbts, for configurable
  additional checking on each call to checking-splice-rrbts.

  It is more likely that a core.rrb-vector library user will want to
  call checking-catvec rather than this one.  checking-splicev is part
  of the implementation of checking-catvec."
  [v1 v2]
  (let [rv1 (rrbt/as-rrbt v1)]
    (checking-splice-rrbts (.-nm rv1) (.-am rv1)
                           rv1 (rrbt/as-rrbt v2))))

(defn checking-catvec-impl
  "checking-catvec-impl is identical to catvec, except that it calls
  checking-splicev instead of splicev, for configurable additional
  checking on each call to checking-splicev."
  ([]
     [])
  ([v1]
     v1)
  ([v1 v2]
     (checking-splicev v1 v2))
  ([v1 v2 v3]
     (checking-splicev (checking-splicev v1 v2) v3))
  ([v1 v2 v3 v4]
     (checking-splicev (checking-splicev v1 v2) (checking-splicev v3 v4)))
  ([v1 v2 v3 v4 & vn]
     (checking-splicev (checking-splicev (checking-splicev v1 v2) (checking-splicev v3 v4))
                       (apply checking-catvec-impl vn))))

(defn validating-catvec
  "validating-catvec behaves similarly to validating-pop, but note
  that it does not allow you to pass in a function f on which to
  concatenate its arguments.  It hardcodes d/checking-catvec-impl for
  that purpose.  See validating-pop for more details.
  
      opts map: (get @d/debug-opts :catvec)

  If no exception is thrown, the return value is (apply
  checking-catvec-impl vs)."
  [err-desc-str & vs]
  (let [orig-fn checking-catvec-impl  ;; clojure.core.rrb-vector/catvec
        vs-seqs (doall (map copying-seq vs))
        exp-ret-seq (apply concat vs-seqs)
        ret (apply orig-fn vs)
        ret-seq (copying-seq ret)]
    (when (not= ret-seq exp-ret-seq)
      (validation-failure
       "catvec returned incorrect value"
       {:err-desc-str err-desc-str, :ret ret, :args vs,
        :vs-seqs vs-seqs, :ret-seq ret-seq,
        :exp-ret-seq exp-ret-seq}
       (get @debug-opts :catvec)))
    ret))

(defn checking-catvec
  "checking-catvec is similar to checking-pop, with the
  differences summarized below.  See checking-pop documentation for
  details.

  Note that (get @d/debug-otps :catvec) is used to control tracing,
  validating, and return value sanity checks for checking-catvec as a
  whole.  This includes controlling those options for the function
  checking-splice-rrbts, which is used to concatenate pairs of vectors
  when you call checking-catvec with 3 or more vectors.  This takes a
  bit longer to do the checking on every concatenation, but catches
  problems closer to the time they are introduced.

      opts map: (get @d/debug-opts :catvec)
      function called if (:validating opts) is logical true:
          validating-catvec"
  [& args]
  (let [opts (get @debug-opts :catvec)
        err-desc-str "catvec"]
    (when (:trace opts)
      (println "checking-catvec called with" (count args) "args:")
      (dorun (map-indexed (fn [idx v]
                            (println "    arg" (inc idx) " count=" (count v)
                                     "type=" (type v)))
                          args)))
    (let [ret (if (:validate opts)
                (apply validating-catvec err-desc-str args)
                (apply checking-catvec-impl ;; clojure.core.rrb-vector/catvec
                       args))]
      (sanity-check-vector-internals err-desc-str ret args opts)
      ret)))

(defn validating-slicev
  "validating-slicev behaves similarly to validating-pop, but note
  that it does not allow you to pass in a function f to call.  It
  hardcodes slicev for that purpose.  See validating-pop for more
  details.
  
      opts map: (get @d/debug-opts :subvec)  ;; _not_ :slicev"
  ([err-desc-str coll start]
   (validating-slicev err-desc-str coll start (count coll)))
  ([err-desc-str coll start end]
   (let [coll-seq (copying-seq coll)
         exp-ret-seq (take (- end start) (drop start coll-seq))
         ret (clojure.core.rrb-vector.protocols/slicev
              coll start end)
         ret-seq (copying-seq ret)]
     (when (not= ret-seq exp-ret-seq)
       (validation-failure
        "(slicev coll start end) returned incorrect value"
        {:err-desc-str err-desc-str, :ret ret,
         :args (list coll start end),
         :coll-seq coll-seq, :ret-seq ret-seq,
         :exp-ret-seq exp-ret-seq}
        (get @debug-opts :subvec)))
     ret)))

(defn checking-slicev
  "checking-slicev is similar to checking-pop, with the differences
  summarized below.  See checking-pop documentation for details.

  Unlike checking-pop, it seems unlikely that a user of
  core.rrb-vector would want to call this function directly.  See
  checking-subvec.  checking-slicev is part of the implementation of
  checking-subvec.

      opts map: (get @d/debug-opts :subvec)  ;; _not_ :slicev
      function called if (:validating opts) is logical true:
          validating-slicev"
  [& args]
  (let [opts (get @debug-opts :subvec)
        err-desc-str "slicev"]
    (when (:trace opts)
      (let [[v start end] args]
        (println "checking-slicev #v=" (count v) "start=" start "end=" end
                 "type=" (type v))))
    (let [ret (if (:validate opts)
                (apply validating-slicev err-desc-str args)
                (apply clojure.core.rrb-vector.protocols/slicev
                       args))]
      (sanity-check-vector-internals err-desc-str ret args opts)
      ret)))

(defn checking-subvec
  "checking-subvec is similar to checking-pop, with the differences
  summarized below.  See checking-pop documentation for details.

      opts map: (get @d/debug-opts :subvec)
      function called if (:validating opts) is logical true:
          validating-slicev"
  ([v start]
   (checking-slicev v start (count v)))
  ([v start end]
   (checking-slicev v start end)))

(defn check-subvec
  "Perform a sequence of calls to subvec an a core.rrb-vector vector,
  as well as a normal Clojure vector, returning true if they give the
  same results, otherwise false.  Intended for use in tests of this
  library."
  [extra-checks? init & starts-and-ends]
  (let [v1 (loop [v   (vec (range init))
                  ses (seq starts-and-ends)]
             (if ses
               (let [[s e] ses]
                 (recur (clojure.core/subvec v s e) (nnext ses)))
               v))
        my-subvec (if extra-checks? checking-subvec fv/subvec)
        v2 (loop [v   (fv/vec (range init))
                  ses (seq starts-and-ends)]
             (if ses
               (let [[s e] ses]
                 (recur (my-subvec v s e) (nnext ses)))
               v))]
    (pd/same-coll? v1 v2)))

(defn check-catvec
  "Perform a sequence of calls to catvec or checking-catvec on one or
  more core.rrb-vector vectors.  Return true if Clojure's built-in
  concat function give the same results, otherwise false.  Intended
  for use in tests of this library."
  [extra-checks? & counts]
  (let [prefix-sums (reductions + counts)
        ranges (map range (cons 0 prefix-sums) prefix-sums)
        v1 (apply concat ranges)
        my-catvec (if extra-checks? checking-catvec fv/catvec)
        v2 (apply my-catvec (map fv/vec ranges))]
    (pd/same-coll? v1 v2)))

(defn generative-check-subvec
  "Perform many calls to check-subvec with randomly generated inputs.
  Intended for use in tests of this library.  Returns true if all
  tests pass, otherwise throws an exception containing data about the
  inputs that caused the failing test."
  [extra-checks? iterations max-init-cnt slices]
  (dotimes [_ iterations]
    (let [init-cnt (rand-int (inc max-init-cnt))
          s1       (rand-int init-cnt)
          e1       (+ s1 (rand-int (- init-cnt s1)))]
      (loop [s&es [s1 e1] cnt (- e1 s1) slices slices]
        (if (or (zero? cnt) (zero? slices))
          (if-not (try (apply check-subvec extra-checks? init-cnt s&es)
                       (catch Exception e
                         (throw
                          (ex-info "check-subvec failure w/ Exception"
                                   {:init-cnt init-cnt :s&es s&es}
                                   e))))
            (throw
             (ex-info "check-subvec failure w/o Exception"
                      {:init-cnt init-cnt :s&es s&es})))
          (let [s (rand-int cnt)
                e (+ s (rand-int (- cnt s)))
                c (- e s)]
            (recur (conj s&es s e) c (dec slices)))))))
  true)

(defn generative-check-catvec
  "Perform many calls to check-catvec with randomly generated inputs.
  Intended for use in tests of this library.  Returns true if all
  tests pass, otherwise throws an exception containing data about the
  inputs that caused the failing test."
  [extra-checks? iterations max-vcnt min-cnt max-cnt]
  (dotimes [_ iterations]
    (let [vcnt (inc (rand-int (dec max-vcnt)))
          cnts (vec (repeatedly vcnt
                                #(+ min-cnt
                                    (rand-int (- (inc max-cnt) min-cnt)))))]
      (if-not (try (apply check-catvec extra-checks? cnts)
                   (catch Exception e
                     (throw
                      (ex-info "check-catvec failure w/ Exception"
                               {:cnts cnts}
                               e))))
        (throw
         (ex-info "check-catvec failure w/o Exception" {:cnts cnts})))))
  true)
