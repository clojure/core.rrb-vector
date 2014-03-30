(ns clojure.core.rrb-vector.debug
  (:require clojure.core.rrb-vector.rrbt
            [clojure.core.rrb-vector.nodes
             :refer [ranges object-nm primitive-nm]]
            [clojure.core.rrb-vector :as fv])
  (:import (clojure.lang PersistentVector)
           (clojure.core Vec)
           (clojure.core.rrb_vector.rrbt Vector)
           (clojure.core.rrb_vector.nodes NodeManager)))

(defn dbg-vec [v]
  (let [[extract-root extract-shift extract-tail ^NodeManager nm]
        (condp identical? (class v)
          PersistentVector [#(.-root ^PersistentVector %)
                            #(.-shift ^PersistentVector %)
                            #(.-tail ^PersistentVector %)
                            object-nm]
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

(defn same-coll? [a b]
  (and (= (count a)
          (count b)
          (.size ^java.util.Collection a)
          (.size ^java.util.Collection b))
       (= a b)
       (= b a)
       (= (hash a) (hash b))
       (= (.hashCode ^Object a) (.hashCode ^Object b))))

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
    (same-coll? v1 v2)))

(defn check-catvec [& counts]
  (let [ranges (map range counts)
        v1 (apply concat ranges)
        v2 (apply fv/catvec (map fv/vec ranges))]
    (same-coll? v1 v2)))

(defn generative-check-subvec [iterations max-init-cnt slices]
  (dotimes [_ iterations]
    (let [init-cnt (rand-int (inc max-init-cnt))
          s1       (rand-int init-cnt)
          e1       (+ s1 (rand-int (- init-cnt s1)))]
      (loop [s&es [s1 e1] cnt (- e1 s1) slices slices]
        (if (or (zero? cnt) (zero? slices))
          (if-not (try (apply check-subvec init-cnt s&es)
                       (catch Exception e
                         (throw
                          (ex-info "check-subvec failure w/ Exception"
                                   {:init-cnt init-cnt :s&es s&es}
                                   e))))
            (throw
             (ex-info "check-subvec failure w/o Exception"
                      {:init-cnt init-cnt :s&es s&es})))
          (let [s (rand-int cnt)
                e (+ s (rand-int (- cnt s)))
                c (- e s)]
            (recur (conj s&es s e) c (dec slices)))))))
  true)

(defn generative-check-catvec [iterations max-vcnt min-cnt max-cnt]
  (dotimes [_ iterations]
    (let [vcnt (inc (rand-int (dec max-vcnt)))
          cnts (vec (repeatedly vcnt
                                #(+ min-cnt
                                    (rand-int (- (inc max-cnt) min-cnt)))))]
      (if-not (try (apply check-catvec cnts)
                   (catch Exception e
                     (throw
                      (ex-info "check-catvec failure w/ Exception"
                               {:cnts cnts}
                               e))))
        (throw
         (ex-info "check-catvec failure w/o Exception" {:cnts cnts})))))
  true)
