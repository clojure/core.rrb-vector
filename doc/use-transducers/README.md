The patch use-transducers.md was developed around July or August 2019,
but not yet included in the production core.rrb-vector code, because
of a desire to continue to make core.rrb-vector compatible with
Clojure 1.5.1 and later, whereas transducers were not implemented in
Clojure until version 1.7.0.

We should re-examine this patch when we are ready to require Clojure
1.7.0 or later as a minimum supported version for the core.rrb-vector
library.  I may include some performance measurements with and without
these changes, to show how much they can improve the performance of
some operations.
