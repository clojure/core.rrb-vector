(ns clojure.core.rrb-vector.test-common
  (:require [clojure.test :as test :refer [deftest testing is]]
            [clojure.core.rrb-vector :as fv]
            [clojure.core.rrb-vector.test-utils :as utils]))

;; The intent is to keep this file as close to
;; src/test/cljs/clojure/core/rrb_vector/test_common.cljs as possible,
;; so that when we start requiring Clojure 1.7.0 and later for this
;; library, this file and that one can be replaced with a common test
;; file with the suffix .cljc

(deftest test-hasheq
  (is (= (hash []) (hash (fv/vector))))  ;; CRRBV-25
  (let [v1 (vec (range 1024))
        v2 (vec (range 1024))
        v3 (fv/catvec (vec (range 512)) (vec (range 512 1024)))
        s1 (seq v1)
        s2 (seq v2)
        s3 (seq v3)]
    (is (= (hash v1) (hash v2) (hash v3) (hash s1) (hash s2) (hash s3)))
    (is (= (hash (nthnext s1 120))
           (hash (nthnext s2 120))
           (hash (nthnext s3 120))))))

;; This problem reproduction code is slightly modified from a version
;; provided in a comment by Mike Fikes on 2018-Dec-09 for this issue:
;; https://clojure.atlassian.net/projects/CRRBV/issues/CRRBV-20

(defn play [my-vector my-catvec my-subvec players rounds]
  (letfn [(swap [marbles split-ndx]
            (my-catvec
             (my-subvec marbles split-ndx)
             (my-subvec marbles 0 split-ndx)))
          (rotl [marbles n]
            (swap marbles (mod n (count marbles))))
          (rotr [marbles n]
            (swap marbles (mod (- (count marbles) n) (count marbles))))
          (place-marble
            [marbles marble]
            (let [marbles (rotl marbles 2)]
              [(my-catvec (my-vector marble) marbles) 0]))
          (remove-marble [marbles marble]
            (let [marbles (rotr marbles 7)
                  first-marble (nth marbles 0)]
              [(my-subvec marbles 1) (+ marble first-marble)]))
          (play-round [marbles round]
            (if (zero? (mod round 23))
              (remove-marble marbles round)
              (place-marble marbles round)))
          (add-score [scores player round-score]
            (if (zero? round-score)
              scores
              (assoc scores player (+ (get scores player 0) round-score))))]
    (loop [marbles (my-vector 0)
           round   1
           player  1
           scores  {}
           ret     []]
      (let [[marbles round-score] (play-round marbles round)
            scores (add-score scores player round-score)]
        (if (> round rounds)
          (conj ret {:round round :marbles marbles})
          (recur marbles
                 (inc round)
                 (if (= player players) 1 (inc player))
                 scores
                 (conj ret {:round round :marbles marbles})))))))

(defn play-core [& args]
  (apply play clojure.core/vector clojure.core/into clojure.core/subvec args))

(defn play-rrbv [& args]
  (apply play fv/vector fv/catvec fv/subvec args))

(deftest test-crrbv-20
  ;; This one passes
  (is (= (play-core 10 1128)
         (play-rrbv 10 1128)))
  ;; This ends up with (play-rrbv 10 1129) throwing an exception
  (is (= (play-core 10 1129)
         (play-rrbv 10 1129)))

  ;; The previous test demonstrates a bug in the transient RRB vector
  ;; implementation.  The one below demonstrates a similar bug in the
  ;; persistent RRB vector implementation.
  (let [v1128 (:marbles (last (play-rrbv 10 1128)))
        v1129-pre (-> v1128
                      (fv/subvec 2)
                      (conj 2001))]
    (is (every? integer? (conj v1129-pre 2002)))))

(deftest test-crrbv-21
  ;; The following sequence of operations gives a different exception
  ;; than the above, and I suspect is probably a different root cause
  ;; with a distinct fix required.  It might be the same root cause as
  ;; npe-for-1025-then-pop! but I will add a separate test case until
  ;; I know for sure.  Even if they are the same root cause, it does
  ;; not take long to run.

  ;; Note: Even once this bug is fixed, I want to know the answer to
  ;; whether starting from v1128 and then pop'ing off each number of
  ;; elements, until it is down to empty or very nearly so, causes any
  ;; of the error checks within the current version of ranges-errors
  ;; to give an error.  It may require some correcting.
  (let [v1128 (:marbles (last (play-rrbv 10 1128)))
        vpop1 (reduce (fn [v i] (pop v))
                      v1128 (range 1026))]
    (is (every? integer? (pop vpop1)))
    ;; The transient version below gives a similar exception, but the
    ;; call stack goes through the transient version of popTail,
    ;; rather than the persistent version of popTail that the one
    ;; above does.  It seems likely that both versions of popTail have
    ;; a similar bug.
    (is (every? integer? (persistent! (pop! (transient vpop1)))))))

(deftest test-crrbv-22
  (testing "pop! from a regular transient vector with 32*32+1 elements"
    (let [v1025 (into (fv/vector) (range 1025))]
      (is (= (persistent! (pop! (transient v1025)))
             (range 1024)))))
  (testing "pop from a persistent regular vector with 32*32+1 elements"
    (let [v1025 (into (fv/vector) (range 1025))]
      (is (= (pop v1025)
             (range 1024))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; This code was copied from
;; https://github.com/mattiasw2/adventofcode1/blob/master/src/adventofcode1/nineteen_b.clj

;; mentioned in issue
;; https://clojure.atlassian.net/projects/CRRBV/issues/CRRBV-14

(defn puzzle-b [n my-vec my-catvec my-subvec]
  (letfn [(remove-at [arr idx]
            (my-catvec (my-subvec arr 0 idx) (my-subvec arr (inc idx))))
          (create-arr [size]
            (my-vec (range 1 (inc size))))
          (fv-rest [arr]
            (my-subvec arr 1))
          (calculate-opposite [n]
            (int (/ n 2)))
          (move [elfs]
            (let [lc (count elfs)]
              (if (= 1 lc)
                {:ok (first elfs)}
                (let [current      (first elfs)
                      opposite-pos (calculate-opposite lc)
                      _ (assert (> opposite-pos 0))
                      _ (assert (< opposite-pos lc))
                      opposite-elf (nth elfs opposite-pos)
                      other2       (fv-rest (remove-at elfs opposite-pos))]
                  (my-catvec other2 [current])))))
          (puzzle-b-sample [elfs round]
            (let [elfs2 (move elfs)]
              (if (:ok elfs2)
                (:ok elfs2)
                (recur elfs2 (inc round)))))]
    (puzzle-b-sample (create-arr n) 1)))

(defn puzzle-b-core [n]
  (puzzle-b n clojure.core/vec clojure.core/into clojure.core/subvec))

(defn get-shift [v]
  (.-shift v))

(defn vstats [v]
  (str "cnt=" (count v)
       " shift=" (get-shift v)
       ;;" %=" (format "%5.1f" (* 100.0 (dv/fraction-full v)))
       ))

;;(def custom-catvec-data (atom []))

(defn custom-catvec [& args]
  (let [;;n (count @custom-catvec-data)
        max-arg-shift (apply max (map get-shift args))
        ret (apply fv/catvec args)
        ret-shift (get-shift ret)]
    (when (or (>= ret-shift 30)
              (> ret-shift max-arg-shift))
      (doall (map-indexed
              (fn [idx v]
                (println (str "custom-catvec ENTER v" idx "  " (vstats v))))
              args))
      (println (str "custom-catvec LEAVE ret " (vstats ret))))
    ;;(swap! custom-catvec-data conj {:args args :ret ret})
    ;;(println "custom-catvec RECRD in index" n "of @custom-catvec-data")
    ret))

(defn puzzle-b-rrbv [n]
  (puzzle-b n fv/vec custom-catvec fv/subvec))

(deftest test-crrbv-14
  ;; This one passes
  (utils/reset-optimizer-counts!)
  (is (= (puzzle-b-core 977)
         (puzzle-b-rrbv 977)))
  (utils/print-optimizer-counts)
  ;; (puzzle-b-rrbv 978) throws
  ;; ArrayIndexOutOfBoundsException
  (utils/reset-optimizer-counts!)
  (is (integer? (puzzle-b-rrbv 978)))
  (utils/print-optimizer-counts))
