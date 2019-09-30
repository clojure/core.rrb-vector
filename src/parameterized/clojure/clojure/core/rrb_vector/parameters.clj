(ns clojure.core.rrb-vector.parameters)

;; The values in comments before each def are the value of that
;; parameter:

;; * when the shift-increment is 5
;; * when the shift-increment is 3
;; * when the shift-increment is 2

;; 5 3 2
(def shift-increment 5)

;; 10 6 4
(def shift-increment-times-2 (* 2 shift-increment))

;; 32 8 4
(def max-branches (bit-shift-left 1 shift-increment))

;; 0x1f 0x7 0x3
(def branch-mask (dec max-branches))

;; 31 7 3
(def max-branches-minus-1 (dec max-branches))

;; 30 6 2
(def max-branches-minus-2 (- max-branches 2))

;; 33 9 5
(def non-regular-array-len (inc max-branches))

;; 1024 64 16
(def max-branches-squared (* max-branches max-branches))
