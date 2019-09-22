(ns clojure.core.rrb-vector.test-cljs-only
  (:require [clojure.test :as test :refer [deftest testing is are]]
            [clojure.core.rrb-vector.test-utils :as u]
            [clojure.core.rrb-vector :as fv]
            [clojure.core.rrb-vector.debug :as dv]
            [goog.string :as gstring]
            goog.string.format))

(dv/set-debug-opts! dv/full-debug-opts)

(defn format [& args]
  (apply gstring/format args))

(deftest test-slicing-generative
  (try (dv/generative-check-subvec u/extra-checks? 125 100000 10)
       (catch ExceptionInfo e
         (throw (ex-info (format "%s: %s %s"
                                 (ex-message e)
                                 (:init-cnt (ex-data e))
                                 (:s&es (ex-data e)))
                         {}
                         (ex-cause e))))))

(def medium-check-catvec-params [250 30 10 60000])
(def short-check-catvec-params [10 30 10 60000])
;;(def check-catvec-params medium-check-catvec-params)
(def check-catvec-params short-check-catvec-params)

(deftest test-splicing-generative
  (try (apply dv/generative-check-catvec u/extra-checks? check-catvec-params)
       (catch ExceptionInfo e
         (throw (ex-info (format "%s: %s"
                                 (.getMessage e)
                                 (:cnts (ex-data e)))
                         {}
                         (.getCause e))))))
