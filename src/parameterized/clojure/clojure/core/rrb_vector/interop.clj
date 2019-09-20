(ns clojure.core.rrb-vector.interop
  (:require [clojure.core.rrb-vector.protocols
             :refer [PSliceableVector slicev
                     PSpliceableVector splicev]]
            [clojure.core.rrb-vector.rrbt :refer [as-rrbt]])
  (:import (clojure.core Vec)
           (clojure.lang PersistentVector APersistentVector$SubVector)
           (clojure.core.rrb_vector.rrbt Vector)))

(extend-protocol PSliceableVector
  Vec
  (slicev [v start end]
    (slicev (as-rrbt v) start end))

  PersistentVector
  (slicev [v start end]
    (slicev (as-rrbt v) start end))

  APersistentVector$SubVector
  (slicev [v start end]
    (slicev (as-rrbt v) start end)))

(extend-protocol PSpliceableVector
  Vec
  (splicev [v1 v2]
    (splicev (as-rrbt v1) v2))

  PersistentVector
  (splicev [v1 v2]
    (splicev (as-rrbt v1) v2))

  APersistentVector$SubVector
  (splicev [v1 v2]
    (splicev (as-rrbt v1) v2)))
