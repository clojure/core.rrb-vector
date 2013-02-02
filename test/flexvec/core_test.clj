(ns flexvec.core-test
  (:require [flexvec.core :as fv]
            [flexvec.debug :as dv])
  (:use clojure.test)
  (:import (clojure.lang ExceptionInfo)))

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
    (is (apply dv/check-catvec (repeat 30 33))))
  (testing "splicing (generative)"
    (is (try (dv/generative-check-catvec 250 30 10 60000)
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

(deftest test-seq
  (let [v (fv/vec (range 128))
        s (seq v)]
    (testing "seq contents"
      (is (= v s)))
    (testing "chunked-seq?"
      (is (chunked-seq? s)))
    (testing "internal-reduce"
      (is (satisfies? clojure.core.protocols/InternalReduce s)))))
