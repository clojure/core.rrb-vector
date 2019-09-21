(ns clojure.core.rrb-vector.debug-platform-dependent
  (:require clojure.core.rrb-vector.rrbt
            [clojure.core.rrb-vector.nodes :refer [regular? node-ranges]]
            [clojure.core.rrb-vector :as fv]
            [clojure.core.rrb-vector.trees
             :refer [tail-offset]]
            [goog.string :as gstring]
            goog.string.format))

(defn format [& args]
  (apply gstring/format args))

(defn printf [& args]
  (print (apply gstring/format args)))

(defn internal-node? [x]
  ;; TBD: Is there another type that should be included here?  Clojure
  ;; only has this one, plus a VecNode that is distinct to the
  ;; primitive vectors, which ClojureScript does not have.
  (instance? cljs.core/VectorNode x))

(defn persistent-vector? [x]
  (or (instance? cljs.core/PersistentVector x)
      (instance? cljs.core/Subvec x)
      (instance? clojure.core.rrb-vector.rrbt/Vector x)))

(defn transient-vector? [x]
  (or (instance? cljs.core/TransientVector x)
      (instance? clojure.core.rrb-vector.rrbt/Transient x)))

(defn is-vector? [x]
  (or (persistent-vector? x)
      (transient-vector? x)))

(defn dbg-tailoff [v]
  (cond
    (or (instance? cljs.core/PersistentVector v)
        (instance? cljs.core/TransientVector v))
    (#'cljs.core/tail-off v)

    (or (instance? clojure.core.rrb-vector.rrbt/Vector v)
        (instance? clojure.core.rrb-vector.rrbt/Transient v))
    (tail-offset v)

    :else
    (throw (ex-info (str "Called debug-tailoff on value with unsupported type "
                         (pr-str (type v)))
                    {:value v}))))

(defn subvector-data [v]
  (if (instance? cljs.core/Subvec v)
    {:orig-v v
     :subvector? true
     :v (.-v v)
     :subvec-start (.-start v)
     :subvec-end (.-end v)}
    {:orig-v v
     :subvector? false
     :v v}))

(defn unwrap-subvec-accessors-for [v]
  (let [{:keys [v] :as m} (subvector-data v)]
    (merge m {:get-root #(.-root %)
              :get-shift #(.-shift %)
              :get-tail #(.-tail %)
              :get-cnt #(.-cnt %)
              :get-array #(.-arr %)
              :get-ranges node-ranges
              :regular? regular?
              :tail-len alength})))

(defn dbg-tidx [tv]
  (if (transient-vector? tv)
    (if (instance? cljs.core/TransientVector tv)
      (let [c (.-cnt tv)]
        (if (== c 32)
          32
          (bit-and c 0x01f)))
      (.-tidx tv))))

(defn abbrev-for-type-of [obj]
  (let [tn (pr-str (type obj))
        d (.lastIndexOf tn ".")]
    (subs tn (inc d))))

(defn same-coll? [a b]
  (and (= (count a)
          (count b))
       (= a b)
       (= b a)
       (= (hash a) (hash b))
       ;; TBD: Is there anything JavaScript specific that corresponds
       ;; to Java's .hashCode method?
       ;;(= (.hashCode ^Object a) (.hashCode ^Object b))
       ))

;; In ClojureScript, as in JavaScript, there is only 1 thread, so all
;; updates to transients are automatically thread confined, to the
;; only thread.  There is still an edit field/property on the vector
;; tree nodes, but it is used only for the purpose of distinguishing
;; which nodes are uniquely "owned" by this transient vector,
;; vs. those tree nodes that might be shared with other persistent
;; vectors.  For that purpose, when a vector is made transient, its
;; root tree node is assigned a new unique JavaScript object, as
;; returned by (js-obj).  Any tree node whose edit field is identical
;; to that one is owned by this transient, any not identical are not.
;; Only the root tree node has its edit field changed to nil when the
;; transient vector is converted to persistent, because any other tree
;; nodes that are identical to it will never be identical to another
;; new (js-obj) return value.

;; Thus for the JavaScript implementation, it is not the case that in
;; persistent vectors that all edit fields must be nil, or an
;; AtomicReference that contains nil.  Some of them can be return
;; values of (js-obj) from times when they were part of a transient
;; vector, but are no longer.

;; About the only thing we can check here that I believe must always
;; be true, is that persistent vectors have a root tree node with edit
;; field equal to nil, and transient vectors must have a root tree
;; node with edit field not equal to nil.

(defn edit-nodes-errors [v _]
  (let [{:keys [v get-root]} (unwrap-subvec-accessors-for v)
        root-edit (.-edit (get-root v))
        root-edit-is-nil? (nil? root-edit)]
    (cond
      (and (transient-vector? v)
           root-edit-is-nil?)
      {:error true
       :description (str "A transient vector with type" (pr-str (type v))
                         " has a root edit property with value nil"
                         " - expecting a non-nil JavaScript object")
       :data v}

      (and (persistent-vector? v)
           (not root-edit-is-nil?))
      {:error false, :warning true,
       :description (str "A persistent vector with type " (pr-str (type v))
                         " has a root edit property with value " root-edit
                         " - often this is nil instead."
                         " It requires more thought to be certain"
                         " whether this could lead to problems,"
                         " hence why this is only a warning")
       :data v}

      :else
      {:error false})))
