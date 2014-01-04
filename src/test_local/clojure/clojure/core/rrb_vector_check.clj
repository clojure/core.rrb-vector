(ns clojure.core.rrb-vector-check
  (:require [clojure.core.rrb-vector :as fv]
            [collection-check :refer [assert-vector-like]])
  (:use clojure.test))

(deftest collection-check
  (is (assert-vector-like (fv/vector) simple-check.generators/int))
  (is (every? nil? (.-array ^clojure.lang.PersistentVector$Node
                            (.-root ^clojure.lang.PersistentVector (vector))))))
