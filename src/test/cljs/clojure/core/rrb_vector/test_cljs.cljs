(ns clojure.core.rrb-vector.test-cljs
  (:require [cljs.test :as test]
            [clojure.core.rrb-vector :as fv]
            clojure.core.rrb-vector.test-common
            clojure.core.rrb-vector.test-cljs-only
            clojure.core.rrb-vector.long-test))

;; This file was copied from namespace clojure.data.xml.test-cljs
;; in the data.xml library tests, then modified for use by
;; core.rrb-vector, so that core.rrb-vector's ClojureScript tests
;; could also be run on build.clojure.org via a mvn command.

(def ^:dynamic *results*)

(def num-deftests-started (atom 0))
(def last-deftest-start-time (atom nil))

(defn now-msec []
  (js/Date.now))

(defmethod test/report [:cljs.test/default :begin-test-var]
  [m]
  (let [n (swap! num-deftests-started inc)]
    (when (== n 1)
      (println "*clojurescript-version*" *clojurescript-version*)))
  (println)
  (println "starting cljs test" (:var m))
  (reset! last-deftest-start-time (now-msec)))

(defmethod test/report [:cljs.test/default :end-test-var]
  [m]
  (println "elapsed time (sec)" (/ (- (now-msec) @last-deftest-start-time)
                                   1000.0)))

(defmethod test/report [::test/default :end-run-tests]
  [m]
  (assert (nil? *results*))
  (set! *results* m))

;; Function -main-nashorn is called when running tests using a maven
;; command such as this one:
;; mvn -DCLOJURE_VERSION=1.10.1 -Dclojure.version=1.10.1 clean test

;; Function run is called when running tests using a 'lein cljsbuild'
;; command such as this one:
;; lein with-profile +cljs,+1.10 cljsbuild test

;; To run the tests in a namespace from here, it must be :require'd
;; above, and its name must match the regex given to a run-all-tests
;; call.  You may also call run-all-tests with no args to run tests in
;; all namespaces, but even then it must be :require'd above.

(defn ^:export -main-nashorn []
  (set! *print-newline* false)
  (set! *print-fn* js/print)
  (set! *print-err-fn* js/print)
  (binding [*results* nil]
    (println "Running Basic Tests")
    (test/run-all-tests #"clojure\.core\.rrb-vector\.test-.*")
    ;;(test/run-all-tests #"clojure\.core\.rrb-vector\..*test.*")
    (pr-str *results*)))

(defn ^:export run []
  (println "Running Basic Tests")
  (test/run-all-tests #"clojure\.core\.rrb-vector\.test-.*")
  ;;(test/run-all-tests #"clojure\.core\.rrb-vector\..*test.*")
  )
