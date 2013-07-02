(ns cljs.core.rrb-vector.macros
  (:refer-clojure :exclude [assert]))

(def ^:const elide-assertions? true)
(def ^:const elide-debug-printouts? true)

(defmacro assert [& args]
  (if-not elide-assertions?
    (apply #'clojure.core/assert &form &env args)))

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
       (cljs.core.rrb_vector.rrbt.Vector.
        ~(count params) 5 cljs.core.PersistentVector/EMPTY_NODE ~arr nil
        ~(if params nil 0)))))
