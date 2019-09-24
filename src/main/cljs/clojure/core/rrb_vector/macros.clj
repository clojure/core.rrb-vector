(ns clojure.core.rrb-vector.macros
  (:refer-clojure :exclude [assert]))

(def ^:const elide-assertions? true)
(def ^:const elide-debug-printouts? true)

(defmacro assert [& args]
  (if-not elide-assertions?
    `(clojure.core/assert ~@args)))

(defmacro dbg [& args]
  (if-not elide-debug-printouts?
    `(prn ~@args)))

(defmacro dbg- [& args])

(defmacro ^:private gen-vector-method [& params]
  (let [arr (gensym "arr__")]
    `(let [~arr (cljs.core/make-array ~(count params))]
       ~@(map-indexed (fn [i param]
                        `(cljs.core/aset ~arr ~i ~param))
                      params)
       (clojure.core.rrb-vector.rrbt/Vector.
        ~(count params) 5 cljs.core/PersistentVector.EMPTY_NODE ~arr nil nil))))
