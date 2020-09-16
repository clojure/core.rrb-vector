;   Copyright (c) Rich Hickey and contributors. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clojure.core.rrb-vector.interop
  (:require [clojure.core.rrb-vector.protocols
             :refer [PSliceableVector -slicev
                     PSpliceableVector -splicev]]
            [clojure.core.rrb-vector.rrbt :refer [-as-rrbt]]))

(extend-protocol PSliceableVector
  cljs.core/PersistentVector
  (-slicev [v start end]
    (-slicev (-as-rrbt v) start end))

  cljs.core/Subvec
  (-slicev [v start end]
    (-slicev (-as-rrbt v) start end)))

(extend-protocol PSpliceableVector
  cljs.core/PersistentVector
  (-splicev [v1 v2]
    (-splicev (-as-rrbt v1) v2))

  cljs.core/Subvec
  (-splicev [v1 v2]
    (-splicev (-as-rrbt v1) v2)))
