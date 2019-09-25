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

;; collection-check.core/assert-vector-like calls test.chuck/checking.
;; The README for the test.chuck library says that test.chuck/checking
;; is intended to be called directly within a `deftest` form, with no
;; need for any `is` or `are` calls, because test.chuck/checking makes
;; calls to those macros inside itself.
;;
;; I have confirmed, by intentionally making the function fv/vector
;; return incorrect values in some cases, that this deftest does fail
;; as it should, given the direct call to function assert-vector-like.

(deftest collection-check
  (println "Before assert-vector-like with num-tests=" num-tests)
  (assert-vector-like num-tests (fv/vector) gen/int)
  (println "After assert-vector-like with num-tests=" num-tests)
  (is (every? nil? (.-array ^clojure.lang.PersistentVector$Node
                            (.-root ^clojure.lang.PersistentVector (vector))))))
