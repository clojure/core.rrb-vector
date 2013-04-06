(ns clojure.core.rrb-vector.fork-join
  (:require [clojure.core.reducers :as r]))

(def pool   @#'r/pool)
(def task   @#'r/fjtask)
(def invoke @#'r/fjinvoke)
(def fork   @#'r/fjfork)
(def join   @#'r/fjjoin)
