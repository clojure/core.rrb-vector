(ns clojure.core.rrb-vector-test
  (:require [clojure.core.rrb-vector :as fv]
            [clojure.core.rrb-vector.debug :as dv]
            [goog.string :as gstring]
            goog.string.format))

(set-print-fn! js/print)

(defn format [& args]
  (apply gstring/format args))

(defn test-slicing []
  (assert (dv/check-subvec 32000 10 29999 1234 18048 10123 10191)))

(defn test-slicing-generative []
  (try (dv/generative-check-subvec 125 100000 10)
       (catch ExceptionInfo e
         (throw (ex-info (format "%s: %s %s"
                                 (ex-message e)
                                 (:init-cnt (ex-data e))
                                 (:s&es (ex-data e)))
                         {}
                         (ex-cause e))))))

(defn test-splicing []
  (assert (dv/check-catvec 1025 1025 3245 1025 32768 1025 1025 10123 1025 1025))
  (assert (dv/check-catvec 10 40 40 40 40 40 40 40 40))
  (assert (apply dv/check-catvec (repeat 30 33))))

(defn test-splicing-generative []
  (try (dv/generative-check-catvec 125 15 10 30000)
       (catch ExceptionInfo e
         (throw (ex-info (format "%s: %s"
                                 (.getMessage e)
                                 (:cnts (ex-data e)))
                         {}
                         (.getCause e))))))

(defn test-reduce []
  (let [v1 (vec (range 128))
        v2 (fv/vec (range 128))]
    (assert (= (reduce + v1) (reduce + v2)))
    (assert (= (reduce-kv + 0 v1) (reduce-kv + 0 v2)))))

(defn test-seq []
  (let [v (fv/vec (range 128))
        s (seq v)]
    (assert (= v s))
    (assert (chunked-seq? s))
    (assert (satisfies? IReduce s))))

(defn test-assoc []
  (let [v1 (fv/vec (range 40000))
        v2 (reduce (fn [out [k v]]
                     (assoc out k v))
                   (assoc v1 40000 :foo)
                   (map-indexed vector (rseq v1)))]
    (assert (= (concat (rseq v1) [:foo]) v2)))
  (loop [i 1]
    (if (< i 35000)
      (let [v (-> (range 40000)
                  (fv/vec)
                  (fv/subvec i)
                  (assoc 10 :foo))]
        (assert (= :foo (nth v 10)))
        (recur (* i 32))))))

(defn test-assoc! []
  (let [v1 (fv/vec (range 40000))
        v2 (persistent!
            (reduce (fn [out [k v]]
                      (assoc! out k v))
                    (assoc! (transient v1) 40000 :foo)
                    (map-indexed vector (rseq v1))))]
    (assert (= (concat (rseq v1) [:foo]) v2)))
  (loop [i 1]
    (if (< i 35000)
      (let [v (-> (range 40000)
                  (fv/vec)
                  (fv/subvec i)
                  (transient)
                  (assoc! 10 :foo)
                  (persistent!))]
        (assert (= :foo (nth v 10)))
        (recur (* i 32))))))

(defn test-relaxed []
  (assert (= (into (fv/catvec (vec (range 123)) (vec (range 68))) (range 64))
             (concat (range 123) (range 68) (range 64))))
  (assert (= (dv/slow-into (fv/catvec (vec (range 123)) (vec (range 68)))
                           (range 64))
             (concat (range 123) (range 68) (range 64)))))

(defn test-splice-high-subtree-branch-count []
  (let [x        (fv/vec (repeat 1145 \a))
        y        (fv/catvec (fv/subvec x 0 778) (fv/subvec x 778 779) [1] (fv/subvec x 779))
        z        (fv/catvec (fv/subvec y 0 780) [2] (fv/subvec y 780 781) (fv/subvec y 781))
        res      (fv/catvec (fv/subvec z 0 780) [] [3] (fv/subvec z 781))
        expected (concat (repeat 779 \a) [1] [3] (repeat 366 \a))]
    (assert (= res expected))))

(defn run-tests []
  (test-slicing)
  (test-slicing-generative)
  (test-splicing)
  (test-splicing-generative)
  (test-reduce)
  (test-seq)
  (test-assoc)
  (test-assoc!)
  (test-relaxed)
  (test-splice-high-subtree-branch-count)
  (println "Tests completed without exception."))

(run-tests)
