(ns clojure.core.rrb-vector.debug-platform-dependent
  (:refer-clojure :exclude [format printf])
  (:require [clojure.core.rrb-vector.parameters :as p]
            clojure.core.rrb-vector.rrbt
            [clojure.core.rrb-vector.nodes
             :refer [ranges object-nm primitive-nm object-am]]
            [clojure.core.rrb-vector :as fv])
  (:import (clojure.lang PersistentVector PersistentVector$TransientVector
                         PersistentVector$Node APersistentVector$SubVector)
           (java.util.concurrent.atomic AtomicReference)
           (java.lang.reflect Field Method)
           (clojure.core Vec VecNode ArrayManager)
           (clojure.core.rrb_vector.rrbt Vector Transient)
           (clojure.core.rrb_vector.nodes NodeManager)))

;; Work around the fact that several fields of type
;; PersistentVector$TransientVector are private, but note that this is
;; only intended for debug use.
(def ^Class transient-core-vec-class (class (transient (vector))))
(def ^Field transient-core-root-field (.getDeclaredField transient-core-vec-class "root"))
(.setAccessible transient-core-root-field true)
(def ^Field transient-core-shift-field (.getDeclaredField transient-core-vec-class "shift"))
(.setAccessible transient-core-shift-field true)
(def ^Field transient-core-tail-field (.getDeclaredField transient-core-vec-class "tail"))
(.setAccessible transient-core-tail-field true)
(def ^Field transient-core-cnt-field (.getDeclaredField transient-core-vec-class "cnt"))
(.setAccessible transient-core-cnt-field true)

(def transient-core-vec-tailoff-methods
  (filter #(= "tailoff" (.getName ^Method %))
          (.getDeclaredMethods transient-core-vec-class)))
(assert (= (count transient-core-vec-tailoff-methods) 1))
(def ^Method transient-core-vec-tailoff-method
  (first transient-core-vec-tailoff-methods))
(.setAccessible transient-core-vec-tailoff-method true)


(def ^Class persistent-core-vec-class (class (vector)))
(def persistent-core-vec-tailoff-methods
  (filter #(= "tailoff" (.getName ^Method %))
          (.getDeclaredMethods persistent-core-vec-class)))
(assert (= (count persistent-core-vec-tailoff-methods) 1))
(def ^Method persistent-core-vec-tailoff-method
  (first persistent-core-vec-tailoff-methods))
(.setAccessible persistent-core-vec-tailoff-method true)


(def format clojure.core/format)

(def printf clojure.core/printf)

(defn internal-node? [obj]
  (contains? #{PersistentVector$Node VecNode} (class obj)))

(defn persistent-vector? [obj]
  (contains? #{PersistentVector Vec Vector}
             (class obj)))

(defn transient-vector? [obj]
  (contains? #{PersistentVector$TransientVector Transient}
             (class obj)))

(defn is-vector? [obj]
  (contains? #{PersistentVector Vec Vector
               PersistentVector$TransientVector Transient}
             (class obj)))

(defn dbg-tailoff [v]
  (cond
    (instance? PersistentVector v)
    (.invoke persistent-core-vec-tailoff-method v (object-array 0))

    (= PersistentVector$TransientVector (class v))
    (.invoke transient-core-vec-tailoff-method v (object-array 0))

    :else
    (.tailoff v)))

(defn dbg-tidx [v]
  (- (count v) (dbg-tailoff v)))

(defn subvector-data [v]
  (if (instance? APersistentVector$SubVector v)
    (let [^APersistentVector$SubVector v v]
      {:orig-v v
       :subvector? true
       :v (.v v)
       :subvec-start (.start v)
       :subvec-end (.end v)})
    {:orig-v v
     :subvector? false
     :v v}))

;; All of the classes below have a .tailoff method implementation that
;; works correctly for that class.  You can use the debug-tailoff
;; function to work around the fact that this method is not public for
;; some of the vector classes.

(defn accessors-for [v]
  (condp identical? (class v)
    PersistentVector (let [nm object-nm, am object-am]
                       {:get-root #(.-root ^PersistentVector %)
                        :get-shift #(.-shift ^PersistentVector %)
                        :get-tail #(.-tail ^PersistentVector %)
                        :get-cnt #(.-cnt ^PersistentVector %)
                        :get-array #(.array ^NodeManager nm %)
                        :get-ranges #(ranges ^NodeManager nm %)
                        :regular? #(.regular ^NodeManager nm %)
                        :tail-len #(.alength ^ArrayManager am %)
                        })
    PersistentVector$TransientVector
                     (let [nm object-nm, am object-am]
                       {:get-root #(.get transient-core-root-field ^PersistentVector$TransientVector %)
                        :get-shift #(.get transient-core-shift-field ^PersistentVector$TransientVector %)
                        :get-tail #(.get transient-core-tail-field ^PersistentVector$TransientVector %)
                        :get-cnt #(.get transient-core-cnt-field ^PersistentVector$TransientVector %)
                        :get-array #(.array ^NodeManager nm %)
                        :get-ranges #(ranges ^NodeManager nm %)
                        :regular? #(.regular ^NodeManager nm %)
                        :tail-len #(.alength ^ArrayManager am %)
                        })
    Vec              (let [nm primitive-nm, am #(.-am ^Vec %)]
                       {:get-root #(.-root ^Vec %)
                        :get-shift #(.-shift ^Vec %)
                        :get-tail #(.-tail ^Vec %)
                        :get-cnt #(.-cnt ^Vec %)
                        :get-array #(.array ^NodeManager nm %)
                        :get-ranges #(ranges ^NodeManager nm %)
                        :regular? #(.regular ^NodeManager nm %)
                        :tail-len #(.alength ^ArrayManager am %)
                        })
    Vector           (let [nm (.-nm ^Vector v), am #(.-am ^Vector %)]
                       {:get-root #(.-root ^Vector %)
                        :get-shift #(.-shift ^Vector %)
                        :get-tail #(.-tail ^Vector %)
                        :get-cnt #(.-cnt ^Vector %)
                        :get-array #(.array ^NodeManager nm %)
                        :get-ranges #(ranges ^NodeManager nm %)
                        :regular? #(.regular ^NodeManager nm %)
                        :tail-len #(.alength ^ArrayManager am %)
                        })
    Transient        (let [nm (.-nm ^Transient v), am (.-am ^Transient v)]
                       {:get-root #(.debugGetRoot ^Transient %)
                        :get-shift #(.debugGetShift ^Transient %)
                        :get-tail #(.debugGetTail ^Transient %)
                        :get-cnt #(.debugGetCnt ^Transient %)
                        :get-array #(.array ^NodeManager nm %)
                        :get-ranges #(ranges ^NodeManager nm %)
                        :regular? #(.regular ^NodeManager nm %)
                        :tail-len #(.alength ^ArrayManager am %)
                        })))

(defn unwrap-subvec-accessors-for [v]
  (let [{:keys [v] :as m} (subvector-data v)
        accessors (accessors-for v)]
    (merge m accessors)))

(defn abbrev-for-type-of [obj]
  (let [cn (.getName (class obj))
        d  (.lastIndexOf cn ".")]
    (subs cn (inc d))))

(defn same-coll? [a b]
  (and (= (count a)
          (count b)
          (.size ^java.util.Collection a)
          (.size ^java.util.Collection b))
       (= a b)
       (= b a)
       (= (hash a) (hash b))
       (= (.hashCode ^Object a) (.hashCode ^Object b))))

;; TBD: No cljs specific version yet
(defn count-nodes [& vs]
  (let [m (java.util.IdentityHashMap.)]
    (doseq [v vs]
      (let [{:keys [v get-root get-shift get-array]}
            (unwrap-subvec-accessors-for v)]
        (letfn [(go [n shift]
                  (when n
                    (.put m n n)
                    (if-not (zero? shift)
                      (let [arr (get-array n)
                            ns  (take 32 arr)]
                        (doseq [n ns]
                          (go n (- shift 5)))))))]
          (go (get-root v) (get-shift v)))))
    (.size m)))

(defn int-array? [x]
  (and (not (nil? x))
       (.isArray (class x))
       (= Integer/TYPE (. (class x) getComponentType))))

;; TBD: No cljs-specific version of this function yet
#_(defn ranges-not-int-array [x]
  (seq (remove int-array? (objects-in-slot-32-of-obj-arrays x))))

(defn atomicref? [x]
  (instance? AtomicReference x))

(defn thread? [x]
  (instance? java.lang.Thread x))

(defn non-identical-edit-nodes [v all-vector-tree-nodes]
  (let [{:keys [v]} (unwrap-subvec-accessors-for v)
        node-maps (all-vector-tree-nodes v)
        ^java.util.IdentityHashMap ihm (java.util.IdentityHashMap.)]
    (doseq [i node-maps]
      (when (= :internal (:kind i))
        (.put ihm (.edit (:node i)) true)))
    ihm))

(defn edit-nodes-errors [v all-vector-tree-nodes]
  (let [{:keys [v get-root]} (unwrap-subvec-accessors-for v)
        klass (class v)
        ^java.util.IdentityHashMap ihm (non-identical-edit-nodes
                                        v all-vector-tree-nodes)
        objs-maybe-some-nils (.keySet ihm)
        ;; I do not believe that Clojure's built-in vector types can
        ;; ever have edit fields equal to nil, but there are some
        ;; cases where I have seen core.rrb-vector edit fields equal
        ;; to nil.  As far as I can tell this seems harmless, as long
        ;; as it is in a persistent vector, not a transient one.
        objs (remove nil? objs-maybe-some-nils)
        neither-nil-nor-atomicref (remove atomicref? objs)]
    (if (seq neither-nil-nor-atomicref)
      {:error true
       :description (str "Found edit object with class "
                         (class (first neither-nil-nor-atomicref))
                         " - expecting nil or AtomicReference")
       :data ihm
       :not-atomic-refs neither-nil-nor-atomicref}
      (let [refd-objs (map #(.get ^AtomicReference %) objs)
            non-nils (remove nil? refd-objs)
            not-threads (remove thread? non-nils)
            root-edit (.edit (get-root v))]
        (cond
          (seq not-threads)
          {:error true
           :description (str "Found edit AtomicReference ref'ing neither nil"
                             " nor a Thread object")
           :data ihm}
          (persistent-vector? v)
          (if (= (count non-nils) 0)
            {:error false}
            {:error true
             :description (str "Within a persistent (i.e. not transient)"
                               " vector, found at least one edit"
                               " AtomicReference object that ref's a Thread"
                               " object.  Expected all of them to be nil.")
             :data ihm
             :val1 (count non-nils)
             :val2 non-nils})
          
          (transient-vector? v)
          (cond
            (not= (count non-nils) 1)
            {:error true
             :description (str "Within a transient vector, found "
                               (count non-nils) " edit AtomicReference"
                               " object(s) that ref's a Thread object."
                               "  Expected exactly 1.")
             :data ihm
             :val1 (count non-nils)
             :val2 non-nils}
            (not (atomicref? root-edit))
            {:error true
             :description (str "Within a transient vector, found root edit"
                               " field that was ref'ing an object with class "
                               (class root-edit)
                               " - expected AtomicReference.")
             :data root-edit}
            (not (thread? (.get ^AtomicReference root-edit)))
            (let [obj (.get ^AtomicReference root-edit)]
              {:error true
               :description (str "Within a transient vector, found root edit"
                                 " field ref'ing an AtomicReference object,"
                                 " but that in turn ref'd something with class "
                                 (class obj)
                                 " - expected java.lang.Thread.")
               :data obj})
            :else {:error false})

          :else {:error true
                 :description (str "Unknown class " klass " for object checked"
                                   " by edit-nodes-wrong-number-of-threads")
                 :data v})))))
