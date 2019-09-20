(ns clojure.core.rrb-vector.parameters)

;; The values in comments before each def are the value of that
;; parameter when the shift-increment is 5, followed by its value when
;; the shift-increment is 2.

;; 5 2
(def shift-increment 5)

;; 32 4
(def max-branches (bit-shift-left 1 shift-increment))

;; 0x1f 0x3
(def branch-mask (dec max-branches))

;; 31 3
(def max-branches-minus-1 (dec max-branches))

;; 30 2
(def max-branches-minus-2 (- max-branches 2))

;; 33 5
(def non-regular-array-len (inc max-branches))

;; 1024 16
(def max-branches-squared (* max-branches max-branches))
