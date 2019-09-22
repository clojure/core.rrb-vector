(ns clojure.core.rrb-vector.test-clj-only
  (:require [clojure.test :as test :refer [deftest testing is are]]
            [clojure.reflect :as ref]
            [clojure.core.rrb-vector.test-infra :as infra]
            [clojure.core.rrb-vector.test-utils :as utils]
            [clojure.core.rrb-vector :as fv]
            [clojure.core.rrb-vector.debug :as dv]
            [clojure.test.check :as tc]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.generators :as gen])
  (:use clojure.template)
  (:import (clojure.lang ExceptionInfo)
           (java.util NoSuchElementException)))

(def extra-checks? false)
(dv/set-debug-opts! dv/full-debug-opts)

(defn clj-version-at-least [major-minor-vector]
  (let [clj-version ((juxt :major :minor) *clojure-version*)
        cmp (compare clj-version major-minor-vector)]
    (>= cmp 0)))

;; medium: 50 to 60 sec
;; short: 2 to 3 sec
(def medium-check-catvec-params [250 30 10 60000])
(def short-check-catvec-params [10 30 10 60000])
;;(def check-catvec-params medium-check-catvec-params)
(def check-catvec-params short-check-catvec-params)

(deftest test-slicing
  (testing "slicing (generative)"
    (is (try (dv/generative-check-subvec extra-checks? 250 200000 20)
             (catch ExceptionInfo e
               (throw (ex-info (format "%s: %s %s"
                                       (.getMessage e)
                                       (:init-cnt (ex-data e))
                                       (:s&es (ex-data e)))
                               {}
                               (.getCause e))))))))

(deftest test-splicing
  (testing "splicing (generative)"
    (is (try (apply dv/generative-check-catvec extra-checks?
                    check-catvec-params)
             (catch ExceptionInfo e
               (throw (ex-info (format "%s: %s"
                                       (.getMessage e)
                                       (:cnts (ex-data e)))
                               {}
                               (.getCause e))))))))

(deftest test-iterators
  (let [v (fv/catvec (vec (range 1000)) (vec (range 1000 2048)))]
    (is (= (iterator-seq (.iterator ^Iterable v))
           (iterator-seq (.iterator ^Iterable (seq v)))
           (iterator-seq (.listIterator ^java.util.List v))
           (iterator-seq (.listIterator ^java.util.List (seq v)))
           (range 2048)))
    (is (= (iterator-seq (.listIterator ^java.util.List v 100))
           (iterator-seq (.listIterator ^java.util.List (seq v) 100))
           (range 100 2048)))
    (letfn [(iterator [xs]
              (.iterator ^Iterable xs))
            (list-iterator
              ([xs]
                 (.listIterator ^java.util.List xs))
              ([xs start]
                 (.listIterator ^java.util.List xs start)))]
      (do-template [iexpr cnt]
        (is (thrown? NoSuchElementException
              (let [iter iexpr]
                (dotimes [_ (inc cnt)]
                  (.next ^java.util.Iterator iter)))))
        (iterator v)                2048
        (iterator (seq v))          2048
        (list-iterator v)           2048
        (list-iterator (seq v))     2048
        (list-iterator v 100)       1948
        (list-iterator (seq v) 100) 1948))))

(deftest test-reduce-subvec-catvec
  (letfn [(insert-by-sub-catvec [v n]
            (fv/catvec (fv/subvec v 0 n) ['x] (fv/subvec v n)))
          (repeated-subvec-catvec [i]
            (reduce insert-by-sub-catvec (vec (range i)) (range i 0 -1)))]
    (is (tc/quick-check 100
          (prop/for-all [cnt (gen/fmap
                               (comp inc #(mod % 60000))
                               gen/pos-int)]
            (= (repeated-subvec-catvec cnt)
               (interleave (range cnt) (repeat 'x))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; This code was copied from the issue:
;; https://clojure.atlassian.net/projects/CRRBV/issues/CRRBV-13

(defn assoc-in-bytevec [my-vector-of use-transient? n indices]
  (let [coll (into (my-vector-of :byte) (range n))
        coll2 (reduce (fn [coll i]
                        (if use-transient?
                          (assoc! coll i -1)
                          (assoc coll i -1)))
                      (if use-transient?
                        (transient coll)
                        coll)
                      indices)]
    (if use-transient?
      (persistent! coll2)
      coll2)))

(defn assoc-in-bytevec-core [& args]
  (apply assoc-in-bytevec clojure.core/vector-of args))

(defn assoc-in-bytevec-rrbv [& args]
  (apply assoc-in-bytevec fv/vector-of args))

(deftest test-crrbv-13
  (println "deftest test-crrbv-13")
  ;; Some cases work, probably the ones where the tail is being
  ;; updated.
  (doseq [use-transient? [false true]]
    (doseq [args [[10 [5]]
                  [32 [0]]
                  [32 [32]]
                  [64 [32]]
                  [64 [64]]]]
      (is (= (apply assoc-in-bytevec-core false args)
             (apply assoc-in-bytevec-rrbv use-transient? args))
          (str "args=" (cons use-transient? args))))
    (doseq [args [[64 [0]]
                  [64 [1]]
                  [64 [31]]]]
      (is (= (apply assoc-in-bytevec-core false args)
             (apply assoc-in-bytevec-rrbv use-transient? args))
          (str "args=" (cons use-transient? args))))))

;; Double check that the type of the mutable fields used to store the
;; cached hash values of collections are 32-bit int, not 64-bit long,
;; because 64-bit long do not have the Java Memory Model thread-safety
;; guarantees that 32-bit int values do.

(defn member-data-by-name [klass field-name-as-symbol]
  (let [klass-dat (ref/type-reflect klass)
        members (:members klass-dat)]
    (first (filter (fn [x] (= field-name-as-symbol (:name x)))
                   members))))

(deftest test-crrbv-26
  ;; For reasons I do not understand, the code below throws an
  ;; exception with Clojure 1.6.0 and earlier because it cannot find
  ;; the Vector and VecSeq classes.  It seems to work fine on Clojure
  ;; 1.7.0 and later, and checking on those versions is enough for the
  ;; purposes of this test.
  (when (clj-version-at-least [1 7])
    (let [vector-class (Class/forName "clojure.core.rrb_vector.rrbt.Vector")
          vecseq-class (Class/forName "clojure.core.rrb_vector.rrbt.VecSeq")]
      (is (= 'int (:type (member-data-by-name vector-class '_hash))))
      (is (= 'int (:type (member-data-by-name vector-class '_hasheq))))
      (is (= 'int (:type (member-data-by-name vecseq-class '_hash))))
      (is (= 'int (:type (member-data-by-name vecseq-class '_hasheq)))))))
