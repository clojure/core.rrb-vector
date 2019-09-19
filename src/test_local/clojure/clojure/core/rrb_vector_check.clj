(ns clojure.core.rrb-vector-check
  (:require [clojure.core.rrb-vector :as fv]
            [clojure.test.check.generators :as gen]
            [collection-check.core :refer [assert-vector-like]])
  (:use clojure.test))

;; On my 2015 MacBook Pro with JDK 11, a few num-tests values and
;; approximate run time of assert-vector-like test on Clojure
;; implementation:

;;  1,000:  16 sec
;; 10,000: 120 sec

(def medium-num-tests 10000)
(def short-num-tests 1000)
;;(def num-tests short-num-tests)
(def num-tests medium-num-tests)

(deftest collection-check
  (println "Before assert-vector-like with num-tests=" num-tests)
  (assert-vector-like num-tests (fv/vector) gen/int)
  (println "After assert-vector-like with num-tests=" num-tests)
  (is (every? nil? (.-array ^clojure.lang.PersistentVector$Node
                            (.-root ^clojure.lang.PersistentVector (vector))))))
