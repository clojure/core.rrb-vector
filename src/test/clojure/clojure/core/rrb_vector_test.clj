(ns clojure.core.rrb-vector-test
  (:require clojure.core.rrb-test-infra
            [clojure.core.rrb-vector :as fv]
            [clojure.core.rrb-vector.debug :as dv]
            [clojure.core.reducers :as r]
            [clojure.test.check :as tc]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.generators :as gen])
  (:use clojure.test
        clojure.template)
  (:import (clojure.lang ExceptionInfo)
           (java.util NoSuchElementException)))

;; medium: 50 to 60 sec
;; short: 2 to 3 sec
(def medium-check-catvec-params [250 30 10 60000])
(def short-check-catvec-params [10 30 10 60000])
;;(def check-catvec-params medium-check-catvec-params)
(def check-catvec-params short-check-catvec-params)

(deftest test-slicing
  (testing "slicing"
    (is (dv/check-subvec 32000 10 29999 1234 18048 10123 10191)))
  (testing "slicing (generative)"
    (is (try (dv/generative-check-subvec 250 200000 20)
             (catch ExceptionInfo e
               (throw (ex-info (format "%s: %s %s"
                                       (.getMessage e)
                                       (:init-cnt (ex-data e))
                                       (:s&es (ex-data e)))
                               {}
                               (.getCause e))))))))

(deftest test-splicing
  (testing "splicing"
    (is (dv/check-catvec 1025 1025 3245 1025 32768 1025 1025 10123 1025 1025))
    (is (dv/check-catvec 10 40 40 40 40 40 40 40 40))
    (is (apply dv/check-catvec (repeat 30 33))))
  (testing "splicing (generative)"
    (is (try (apply dv/generative-check-catvec check-catvec-params)
             (catch ExceptionInfo e
               (throw (ex-info (format "%s: %s"
                                       (.getMessage e)
                                       (:cnts (ex-data e)))
                               {}
                               (.getCause e))))))))

(deftest test-reduce
  (let [v1 (vec (range 128))
        v2 (fv/vec (range 128))]
    (testing "reduce"
      (is (= (reduce + v1) (reduce + v2))))
    (testing "reduce-kv"
      (is (= (reduce-kv + 0 v1) (reduce-kv + 0 v2))))))

(deftest test-reduce-2
  (let [v1 (fv/subvec (vec (range 1003)) 500)
        v2 (vec (range 500 1003))]
    (is (= (reduce + 0 v1)
           (reduce + 0 v2)
           (reduce + 0 (r/map identity (seq v1)))
           (reduce + 0 (r/map identity (seq v2)))))))

(deftest test-seq
  (let [v (fv/vec (range 128))
        s (seq v)]
    (testing "seq contents"
      (is (= v s)))
    (testing "chunked-seq?"
      (is (chunked-seq? s)))
    (testing "internal-reduce"
      (is (satisfies? clojure.core.protocols/InternalReduce s)))))

(deftest test-assoc
  (let [v1 (fv/vec (range 40000))
        v2 (reduce (fn [out [k v]]
                     (assoc out k v))
                   (assoc v1 40000 :foo)
                   (map-indexed vector (rseq v1)))]
    (is (= (concat (rseq v1) [:foo]) v2)))
  (are [i] (= :foo
              (-> (range 40000)
                  (fv/vec)
                  (fv/subvec i)
                  (assoc 10 :foo)
                  (nth 10)))
       1 32 1024 32768))

(deftest test-assoc!
  (let [v1 (fv/vec (range 40000))
        v2 (persistent!
            (reduce (fn [out [k v]]
                      (assoc! out k v))
                    (assoc! (transient v1) 40000 :foo)
                    (map-indexed vector (rseq v1))))]
    (is (= (concat (rseq v1) [:foo]) v2)))
  (are [i] (= :foo
              (-> (range 40000)
                  (fv/vec)
                  (fv/subvec i)
                  (transient)
                  (assoc! 10 :foo)
                  (persistent!)
                  (nth 10)))
       1 32 1024 32768))

(deftest test-relaxed
  (is (= (into (fv/catvec (vec (range 123)) (vec (range 68))) (range 64))
         (concat (range 123) (range 68) (range 64)))))

(deftest test-hasheq
  (let [v1 (vec (range 1024))
        v2 (vec (range 1024))
        v3 (fv/catvec (vec (range 512)) (vec (range 512 1024)))
        s1 (seq v1)
        s2 (seq v2)
        s3 (seq v3)]
    (is (= (hash v1) (hash v2) (hash v3) (hash s1) (hash s2) (hash s3)))
    (is (= (hash (nthnext s1 120))
           (hash (nthnext s2 120))
           (hash (nthnext s3 120))))))

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
    (is (= (repeated-subvec-catvec 2371) (interleave (range 2371) (repeat 'x))))
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
