The production version of the Clojure/Java core.rrb-vector library is
in the directory `src/main/clojure`.  It uses a maximum tree branching
factor of 32, i.e. all tree nodes have at most 32 children, the same
as Clojure's built in persistent vectors.  This is good for look-up
efficiency, but when testing the code, it requires a large number of
vector elements and/or operations on the vectors in order to reach
"interesting" tree structures that exercise all parts of the code, and
find bugs that may be there.

The `src/parameterized/clojure` directory contains source code for a
modified version of the core.rrb-vector library, which uses parameters
defined in file
`src/parameterized/clojure/clojure/core/rrb_vector/parameters.clj` to
control the maximum tree branching factor.  The existing code allows
you to change the value of `shift-increment` to any value that is 2 or
larger, and the maximum tree branching factor will then be 2 to the
power of `shift-increment`.

I have found a bug in the core.rrb-vector library that I do not yet
fully understand.  I have a patch to the code that causes the problem
not to occur, but I do not understand the original or modified code
well enough to be confident that it is a correct fix.

I do have an easily reproducible test case that causes the problem to
occur with the parameterized version of the code when
`shift-increment` is 2, so a maximum tree branch factor of 4.  I
believe it is likely that a different test sequence could be found
that exhibits the same bug with the production code's branch factor of
32, but it would likely be a far longer sequence of operations,
e.g. perhaps as long as millions of operations or more.

Here is a way to reproduce the problem with the parameterized version
of the code.

```bash
git clone https://github.com/clojure/core.rrb-vector
cd core.rrb-vector
git checkout f69df0f95e450bb4ff8e3294f3265d3d25f4e5db
patch -p1 < use-shift-increment-2.patch
```

You can see these tests fail:

```bash
./script/jdo check
```

To reproduce the same problem in the REPL, it takes a few more steps,
shown below.

```bash
# Start a JVM with a socket REPL listening on TCP port 50505
./script/jdo
```

```clojure
;; Connect to the socket REPL from your favorite dev environment, via
;; either a socket REPL, nREPL, whatever you prefer and know how to
;; set up.

(require '[clojure.core.rrb-vector :as fv]
         '[clojure.core.rrb-vector.debug :as dv]
         '[clojure.core.rrb-vector.rrbt :as rrbt])

;; Enable full debug options, except trace printing is off, for all
;; checking-* functions in the debug namespace.
(dv/set-debug-opts! dv/full-debug-opts)

;; The shortest sequence of operations I currently know that reaches
;; the point where the bug occurs only after about 2000 calls to
;; insert-by-sub-vcatvec, with a particular sequence of arguments.

;; It takes a couple of minutes to get there when using
;; checking-catvec and checking-subvec, but we can capture the vectors
;; that cause the problem once we get there, and as long as we keep
;; the same JVM running we can examine their contents all we want.

(def my-catvec dv/checking-catvec)
(def my-subvec dv/checking-subvec)

;; This code is slightly modified from that in the deftest named
;; test-reduce-subvec-catvec in the test-common namespace.

(defn insert-at-index-n [v n]
  (my-catvec (my-subvec v 0 n)
             (dv/cvec ['x])
             (my-subvec v n)))

(defn insert-by-sub-catvec [v [n sz]]
  (let [ret (insert-at-index-n v n)]
    (when (or (< n 20)
              (zero? (mod n 10)))
      (println "n=" n))
    (let [ret-nums (filter number? ret)]
      (if (not= (filter number? ret) (range sz))
        (throw (ex-info (str "Failure for sz=" sz " n=" n)
                        {:v v :sz sz :n n}))))
    ret))

(defn repeated-subvec-catvec [sz]
  (reduce insert-by-sub-catvec
          (dv/cvec (range sz))
          (map (fn [x] [x sz])
               (range sz 0 -1))))

(count @dv/failure-data)
;; should be 0 initially

(def sz 2061)
(def x (repeated-subvec-catvec sz))
;; That caused an exception to be thrown just after "n= 15" was printed

;; Record the exception in e1
(def e1 *e)
(count @dv/failure-data)
;; should be 1 now, since data about 1 error has been appended to the vector
;; @dv/failure-data.

;; extract the data from the error
(def ed (nth @dv/failure-data 0))
(keys ed)
(:err-desc-str ed)
;; "splice-rrbts-main"
(def vret (:ret ed))
(count (:args ed))
;; should be 4.  We only care about the last two args, which are
;; the two vectors that we were trying to concatenate.
(def v1 (nth (:args ed) 2))
(def v2 (nth (:args ed) 3))
(def errinf (:error-info ed))
errinf
;; {:error true, :description "One or more errors found", :data ({:error true, :kind :internal, :description "Found internal non-regular node with 1 non-nil, 3 nil children, and # children prefix sums: (39) - expected that to match stored ranges: (33 0 0 0 1)"})}

(dv/dbg-vec v1)
(dv/dbg-vec v2)
(dv/dbg-vec vret)
```

In the long output of the dbg-vec on vret, there is only one node that
has a ranges array printed as (33 0 0 0 1), so that must be the one
that the error message above is referring to.

Here are the few lines before and after that node, which are near the
beginning of the output:

```
Vector (4109 elements):
16:00 PersistentVector$Node: (33 4108 0 0 2)
  14:00 PersistentVector$Node: (33 0 0 0 1)
    12:00 PersistentVector$Node: (37 39 0 0 2)
      10:00 PersistentVector$Node: (21 29 35 37 4)
```

The line starting with 14:00 is the node that has the reported
problem.

Its first child is the node printed on the line starting with 12:00.
Note that its ranges array contains (37 39 0 0 2), so that node has 2
children, the first with 37 vector elements as leaves beneath it, the
second with 39-37=2 vector elements with leaves beneath it (if those
values are correct -- I believe they are, else ranges-errors would
have complained about those before its parent node).

The node 14:00 should have a ranges array (39 0 0 0 1), but it is (33
0 0 0 1), so it is under-counting the number of vector elements
beneath it by 6.  Its parent node, the one printed on line 16:00, is
consistent with node 14:00, but also too small by 6.

