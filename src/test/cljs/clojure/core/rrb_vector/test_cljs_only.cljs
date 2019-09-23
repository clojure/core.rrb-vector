(ns clojure.core.rrb-vector.test-cljs-only
  (:require [clojure.test :as test :refer [deftest testing is are]]
            [clojure.core.rrb-vector.test-utils :as u]
            [clojure.core.rrb-vector :as fv]
            [clojure.core.rrb-vector.debug :as dv]
            [clojure.core.rrb-vector.debug-platform-dependent :as dpd]))

(dv/set-debug-opts! dv/full-debug-opts)
