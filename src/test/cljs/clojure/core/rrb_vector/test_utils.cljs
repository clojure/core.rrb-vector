(ns clojure.core.rrb-vector.test-utils
  (:require [clojure.core.rrb-vector.rrbt :as rrbt]))

;; The intent is to keep this file as close to
;; src/test/clojure/clojure/core/rrb_vector/test_utils.clj as possible,
;; so that when we start requiring Clojure 1.7.0 and later for this
;; library, this file and that one can be replaced with a common test
;; file with the suffix .cljc

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
