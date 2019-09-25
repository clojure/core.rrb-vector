(ns clojure.core.rrb-vector.test-utils
  (:require [clojure.test :as test]
            [clojure.string :as str]
            [clojure.core.rrb-vector.rrbt :as rrbt]))

;; Parts of this file are nearly identical to
;; src/test/cljs/clojure/core/rrb_vector/test_utils.cljs, but also
;; significant parts are specific to each of the clj/cljs versions, so
;; while they could later be combined into a .cljc file, it may not
;; give much benefit to do so.

(def extra-checks? false)

(defn reset-optimizer-counts! []
  (println "reset all optimizer counts to 0")
  (reset! rrbt/peephole-optimization-count 0)
  (reset! rrbt/fallback-to-slow-splice-count1 0)
  (reset! rrbt/fallback-to-slow-splice-count2 0))

(defn print-optimizer-counts []
  (println "optimizer counts: peephole=" @rrbt/peephole-optimization-count
           "fallback1=" @rrbt/fallback-to-slow-splice-count1
           "fallback2=" @rrbt/fallback-to-slow-splice-count2))

(defn now-msec []
  (System/currentTimeMillis))

(def num-deftests-started (atom 0))
(def last-deftest-start-time (atom nil))

(defn print-jvm-classpath []
  (let [cp-str (System/getProperty "java.class.path")
        cp-strs (str/split cp-str #":")]
    (println "java.class.path:")
    (doseq [cp-str cp-strs]
      (println "  " cp-str))))

(defn print-test-env-info []
  (try
    (let [shift-var (resolve
                     'clojure.core.rrb-vector.parameters/shift-increment)]
      (println "shift-increment=" @shift-var " (from parameters namespace)"))
    (catch Exception e
      (println "shift-increment=5 (assumed because no parameters namespace)")))
  (println "extra-checks?=" extra-checks?)
  (let [p (System/getProperties)]
    (println "java.vm.name" (get p "java.vm.name"))
    (println "java.vm.version" (get p "java.vm.version"))
    (print-jvm-classpath)
    (println "(clojure-version)" (clojure-version))))

(defmethod test/report :begin-test-var
  [m]
  (let [n (swap! num-deftests-started inc)]
    (when (== n 1)
      (print-test-env-info)))
  (println)
  (println "starting clj test" (:var m))
  (reset! last-deftest-start-time (now-msec)))

(defmethod test/report :end-test-var
  [m]
  (println "elapsed time (sec)" (/ (- (now-msec) @last-deftest-start-time)
                                   1000.0)))

;; Enable tests to be run on versions of Clojure before 1.10, when
;; ex-message was added.

(defn ex-message-copy
  "Returns the message attached to ex if ex is a Throwable.
  Otherwise returns nil."
  {:added "1.10"}
  [ex]
  (when (instance? Throwable ex)
    (.getMessage ^Throwable ex)))

(defn ex-cause-copy
  "Returns the cause of ex if ex is a Throwable.
  Otherwise returns nil."
  {:added "1.10"}
  [ex]
  (when (instance? Throwable ex)
    (.getCause ^Throwable ex)))
