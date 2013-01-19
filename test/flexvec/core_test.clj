(ns flexvec.core-test
  (:require [flexvec.core :as fv]
            [flexvec.debug :as dv])
  (:use clojure.test))

(defn rangev [& args]
  (vec (apply range args)))

(defn rangevs [& args]
  (mapv rangev args))

(deftest test-slicing
  (testing "slicing"
    (is (dv/check-subvec 32000 10 29999 1234 18048 10123 10191))))

(deftest test-splicing
  (testing "splicing"
    (is (dv/check-catvec 1025 1025 3245 1025 32768 1025 1025 10123 1025 1025))
    (is (apply dv/check-catvec (repeat 30 33)))))
