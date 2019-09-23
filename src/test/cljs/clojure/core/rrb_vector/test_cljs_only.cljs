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
