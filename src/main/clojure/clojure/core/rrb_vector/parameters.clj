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
