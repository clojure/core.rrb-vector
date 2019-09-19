(ns clojure.core.rrb-vector.test-infra
  (:require [clojure.test :as test]))

(defn now-msec []
  (System/currentTimeMillis))

(def num-deftests-started (atom 0))
(def last-deftest-start-time (atom nil))

(defmethod test/report :begin-test-var
  [m]
  (let [n (swap! num-deftests-started inc)]
    (when (== n 1)
      (let [p (System/getProperties)]
        (println "java.vm.name" (get p "java.vm.name"))
        (println "java.vm.version" (get p "java.vm.version"))
        (println "(clojure-version)" (clojure-version)))))
  (println)
  (println "starting clj test" (:var m))
  (reset! last-deftest-start-time (now-msec)))

(defmethod test/report :end-test-var
  [m]
  (println "elapsed time (sec)" (/ (- (now-msec) @last-deftest-start-time)
                                   1000.0)))
