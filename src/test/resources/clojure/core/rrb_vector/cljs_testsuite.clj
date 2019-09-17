(ns clojure.core.rrb-vector.cljs-testsuite
  (:require
   [clojure.test :refer [is]]
   [cljs.repl.nashorn :as repl-nh]
   [cljs.build.api :as bapi]
   [clojure.java.io :as io])
  (:import
   java.nio.file.Files
   java.nio.file.attribute.FileAttribute))

;; This file was copied from namespace clojure.data.xml.cljs-testsuite
;; in the data.xml library tests, then modified for use by
;; core.rrb-vector, so that core.rrb-vector's ClojureScript tests
;; could also be run on build.clojure.org via a mvn command.

(defn tempdir []
  (str (Files/createTempDirectory
        "cljs-nashorn-" (into-array FileAttribute []))))

(defn compile-testsuite! [dir]
  (let [out (io/file dir "tests.js")
        inputs ["src/main/clojure" "src/test/clojure" "src/test/cljs"]]
    (println "INFO" "Compiling cljs testsuite from" inputs "into" (str out))
    (bapi/build (apply bapi/inputs inputs)
                {:output-to (str out)
                 :output-dir dir
                 :main 'clojure.core.rrb-vector.test-cljs
                 :optimizations :advanced
                 :pseudo-names true
                 :pretty-print true})))

(defn run-testsuite! [dir]
  (System/setProperty "nashorn.persistent.code.cache" "target/nashorn_code_cache")
  (let [engine (repl-nh/create-engine)]
    (compile-testsuite! dir)
    (println "INFO" "Running cljs tests in nashorn with persistent code cache in" (System/getProperty "nashorn.persistent.code.cache"))
    (.eval engine (io/reader (io/file dir "tests.js")))
    (let [{:as res :keys [fail error]} (read-string (.eval engine "clojure.core.rrb_vector.test_cljs._main_nashorn()"))]
      (is (and (zero? fail) (zero? error))
          (pr-str res)))))
