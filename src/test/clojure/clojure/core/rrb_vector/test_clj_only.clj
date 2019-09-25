(ns clojure.core.rrb-vector.test-clj-only
  (:require [clojure.test :as test :refer [deftest testing is are]]
            [clojure.template :refer [do-template]]
            [clojure.reflect :as ref]
            [clojure.core.rrb-vector.test-utils :as u]
            [clojure.core.rrb-vector :as fv]
            [clojure.core.rrb-vector.debug :as dv]
            [clojure.test.check :as tc]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.generators :as gen])
  (:import (java.util NoSuchElementException)))

(dv/set-debug-opts! dv/full-debug-opts)

(defn clj-version-at-least [major-minor-vector]
  (let [clj-version ((juxt :major :minor) *clojure-version*)
        cmp (compare clj-version major-minor-vector)]
    (>= cmp 0)))

(deftest test-iterators
  (let [v (fv/catvec (dv/cvec (range 1000)) (dv/cvec (range 1000 2048)))]
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

;; This test can run in cljs, too, but at least in my testing only if
;; we use test.check version 0.10.0 or later.  However, that seems to
;; be incompatible with running cljs tests with Clojure 1.6.0, so for
;; now at least this test is clj-only.
;;
;; Note: according to several deftest forms within the test.check
;; library's own internal set of tests, it
;; uses (is (:result (tc/quick-check ...))) to check whether a result
;; passes or fails.
;;
;; The doc string for the latest version of test.check as of
;; 2019-Sep-25 says :result is a legacy key, and that :pass?  should
;; have the same value.  I like the descriptiveness of :pass? better,
;; and would prefer to use that here, but core.rrb-vector is not using
;; that latest version of test.check yet.  Consider updating to use
;; key :pass? instead of :result if core.rrb-vector updates to a
;; version of test.check that returns that key.
;;
;; When quick-check finds a failing test case, it still returns a map
;; that Clojure considers to be a logical true value, so the test will
;; still pass if you do `(is (tc/quick-check ...))`.

(deftest test-reduce-subvec-catvec-generative
  (letfn [(insert-by-sub-catvec [v n]
            (fv/catvec (fv/subvec v 0 n) (dv/cvec ['x]) (fv/subvec v n)))
          (repeated-subvec-catvec [i]
            (reduce insert-by-sub-catvec (dv/cvec (range i)) (range i 0 -1)))]
    (is (:result (tc/quick-check 1000
          (prop/for-all [cnt (gen/fmap
                              (comp inc #(mod % 60000))
                              gen/pos-int)]
                        (= (repeated-subvec-catvec cnt)
                           (interleave (range cnt) (repeat 'x)))))))))

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
