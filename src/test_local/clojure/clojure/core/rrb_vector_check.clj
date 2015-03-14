(ns clojure.core.rrb-vector-check
  (:require [clojure.core.rrb-vector :as fv]
            [clojure.test.check.generators :as gen]
            [collection-check :refer [assert-vector-like]])
  (:use clojure.test))

(deftest collection-check
  (is (assert-vector-like 250 (fv/vector) gen/int))
  (is (every? nil? (.-array ^clojure.lang.PersistentVector$Node
                            (.-root ^clojure.lang.PersistentVector (vector))))))
