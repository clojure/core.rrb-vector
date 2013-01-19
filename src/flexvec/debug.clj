(ns flexvec.debug
  (:require flexvec.rrbt
            [flexvec.nodes :refer [ranges object-nm primitive-nm
                                   pv-root pv-shift pv-tail]]
            [flexvec.core :as fv])
  (:import (clojure.lang PersistentVector)
           (clojure.core Vec)
           (flexvec.rrbt Vector)
           (flexvec.nodes NodeManager)))

(defn dbg-vec [v]
  (let [[extract-root extract-shift extract-tail ^NodeManager nm]
        (condp identical? (class v)
          PersistentVector [pv-root pv-shift pv-tail object-nm]
          Vec              [#(.-root ^Vec %)
                            #(.-shift ^Vec %)
                            #(.-tail ^Vec %)
                            primitive-nm]
          Vector           [#(.-root ^Vector %)
                            #(.-shift ^Vector %)
                            #(.-tail ^Vector %)
                            (.-nm ^Vector v)])
        root  (extract-root v)
        shift (extract-shift v)
        tail  (extract-tail v)]
    (letfn [(go [indent shift i node]
              (when node
                (dotimes [_ indent]
                  (print "  "))
                (printf "%02d:%02d %s" shift i
                        (let [cn (.getName (class node))
                              d  (.lastIndexOf cn ".")]
                          (subs cn (inc d))))
                (if-not (or (zero? shift) (.regular nm node))
                  (print ":" (seq (ranges nm node))))
                (if (zero? shift)
                  (print ":" (vec (.array nm node))))
                (println)
                (if-not (zero? shift)
                  (dorun
                   (map-indexed (partial go (inc indent) (- shift 5))
                                (let [arr (.array nm node)]
                                  (if (.regular nm node)
                                    arr
                                    (butlast arr))))))))]
      (printf "%s (%d elements):\n" (.getName (class v)) (count v))
      (go 0 shift 0 root)
      (println "tail:" (vec tail)))))

(defn first-diff [xs ys]
  (loop [i 0 xs (seq xs) ys (seq ys)]
    (if (try (and xs ys (= (first xs) (first ys)))
             (catch Exception e
               (.printStackTrace e)
               i))
      (let [xs (try (next xs)
                    (catch Exception e
                      (prn :xs i)
                      (throw e)))
            ys (try (next ys)
                    (catch Exception e
                      (prn :ys i)
                      (throw e)))]
        (recur (inc i) xs ys))
      (if (or xs ys)
        i
        -1))))

(defn check-subvec [init & starts-and-ends]
  (let [v1 (loop [v   (vec (range init))
                  ses (seq starts-and-ends)]
             (if ses
               (let [[s e] ses]
                 (recur (subvec v s e) (nnext ses)))
               v))
        v2 (loop [v   (fv/vec (range init))
                  ses (seq starts-and-ends)]
             (if ses
               (let [[s e] ses]
                 (recur (fv/subvec v s e) (nnext ses)))
               v))]
    (= v1 v2)))

(defn check-catvec [& counts]
  (let [ranges (map range counts)
        v1 (apply concat ranges)
        v2 (apply fv/catvec (map fv/vec ranges))]
    (= v1 v2)))
