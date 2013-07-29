(ns cljs.core.rrb-vector-test
  (:require [cljs.core.rrb-vector :as fv]
            [cljs.core.rrb-vector.debug :as dv]))

(set-print-fn! js/print)

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
    (assert (= (concat (rseq v1) [:foo]) v2))))

(defn test-assoc! []
  (let [v1 (fv/vec (range 40000))
        v2 (persistent!
            (reduce (fn [out [k v]]
                      (assoc! out k v))
                    (assoc! (transient v1) 40000 :foo)
                    (map-indexed vector (rseq v1))))]
    (assert (= (concat (rseq v1) [:foo]) v2))))

(defn test-relaxed []
  (assert (= (into (fv/catvec (vec (range 123)) (vec (range 68))) (range 64))
             (concat (range 123) (range 68) (range 64))))
  (assert (= (dv/slow-into (fv/catvec (vec (range 123)) (vec (range 68))) (range 64))
             (concat (range 123) (range 68) (range 64)))))

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
  (println "Tests completed without exception."))

(run-tests)
