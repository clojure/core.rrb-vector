(ns cljs.core.rrb-vector.debug
  (:require cljs.core.rrb-vector.rrbt
            [cljs.core.rrb-vector.nodes :refer [regular? ranges]]
            [cljs.core.rrb-vector :as fv]))

(defn dbg-vec [v]
  (let [root  (.-root v)
        shift (.-shift v)
        tail  (.-tail v)]
    (letfn [(go [indent shift i node]
              (when node
                (dotimes [_ indent]
                  (print "  "))
                (printf "%02d:%02d %s" shift i (pr-str (type node)))
                (if-not (or (zero? shift) (regular? node))
                  (print ":" (seq (ranges node))))
                (if (zero? shift)
                  (print ":" (vec (.-arr node))))
                (println)
                (if-not (zero? shift)
                  (dorun
                   (map-indexed (partial go (inc indent) (- shift 5))
                                (let [arr (.-arr node)]
                                  (if (regular? node)
                                    arr
                                    (butlast arr))))))))]
      (printf "%s (%d elements):\n" (pr-str (type v)) (count v))
      (go 0 shift 0 root)
      (println "tail:" (vec tail)))))

(defn first-diff [xs ys]
  (loop [i 0 xs (seq xs) ys (seq ys)]
    (if (try (and xs ys (= (first xs) (first ys)))
             (catch js/Error e
               i))
      (let [xs (try (next xs)
                    (catch js/Error e
                      (prn :xs i)
                      (throw e)))
            ys (try (next ys)
                    (catch js/Error e
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

(defn generative-check-subvec [iterations max-init-cnt slices]
  (try (dotimes [_ iterations]
         (let [init-cnt (rand-int (inc max-init-cnt))
               s1       (rand-int init-cnt)
               e1       (+ s1 (rand-int (- init-cnt s1)))]
           (loop [s&es [s1 e1] cnt (- e1 s1) slices slices]
             (if (or (zero? cnt) (zero? slices))
               (if-not (try (apply check-subvec init-cnt s&es)
                            (catch js/Error e
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
       (catch ExceptionInfo e
         (println (ex-message e))
         (prn (ex-data e))))
  true)

(defn generative-check-catvec [iterations max-vcnt min-cnt max-cnt]
  (try (dotimes [_ iterations]
         (let [vcnt (inc (rand-int (dec max-vcnt)))
               cnts (vec (repeatedly vcnt
                                     #(+ min-cnt
                                         (rand-int (- (inc max-cnt)
                                                      min-cnt)))))]
           (if-not (try (apply check-catvec cnts)
                        (catch js/Error e
                          (throw
                           (ex-info "check-catvec failure w/ Exception"
                                    {:cnts cnts}
                                    e))))
             (throw
              (ex-info "check-catvec failure w/o Exception" {:cnts cnts})))))
       (catch ExceptionInfo e
         (println (ex-message e))
         (prn (ex-data e))))
  true)
