;   Copyright (c) Rich Hickey and contributors. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clojure.core.rrb-vector.parameters)

;; This namespace exists primarily so that the parameterized version
;; of this code, and the 'production' version of this code, can be
;; more similar to each other, by requiring this namespace from most
;; of the other namespaces.

;; Even though the values below are not used in most of the production
;; code, they can serve a little bit as documentation of these
;; parameter values.

(def shift-increment 5)

(def shift-increment-times-2 (* 2 shift-increment))
(def max-branches (bit-shift-left 1 shift-increment))
(def branch-mask (dec max-branches))
(def max-branches-minus-1 (dec max-branches))
(def max-branches-minus-2 (- max-branches 2))
(def non-regular-array-len (inc max-branches))
(def max-branches-squared (* max-branches max-branches))
