# Background on Clojure collection hash calculation

The persistent collections included with Clojure are immutable when
accessed via Clojure's published methods, e.g. `conj`, `assoc`,
`peek`, `seq`, etc.

Their implementation actually uses mutable fields to store cached
versions of their Java `.hashCode` and `clojure.core/hash` values
(those values are different from each other for most collection
values starting with Clojure 1.6.0).

On the JVM, all fields of a newly constructed object are first
initialized to their default JVM initial values, e.g. 0 for a
primitive `int` field, `null` for all references, etc.  Then any
values assigned in the constructor are assigned.  If the field is
declared `final`, then as long as the reference to the object is not
made visible to any other object before the constructor finishes
executing, any thread that later sees the object should see only the
value assigned while the constructor executed, not the default JVM
initial value.  This is promised by the Java Memory Model and the
specialness of the `final` field modifier.

However, the cached hash fields in Clojure, and some other Java
objects, is intentionally stored in non-final fields, so that if no
other code ever needs to know the hash of the value, no time is ever
spent calculating it.  This is a performance optimization.

If some other code does later want the hash value, then it is
calculated on demand at that time, and the calculated value is written
into the field, so that later calls to get the hash value by the same
thread are guaranteed to avoid calculating it again.  If another
thread calls the function/method to get the hash value, because the
hash field has no special Java modifiers like `final` or `volatile`,
the Java Memory Model says that it might get the updated value, or it
might get the value from an older write to that field, e.g. the
initial value of 0 from the JVM default initialization of all fields
-- _even if_ the constructor assigned a non-0 value to the field.

Thus for thread safety of getting hash values of immutable values
using this "initialize a default value, and calculate on demand
later", if the field where the cached value is stored has no special
modifiers like `final` or `volatile`, and the function to get the hash
value is not declared `synchronized` (none of which are true for the
Clojure implementation), the only safe value to assign in the
constructor is none at all (leaving the field as the default JVM
initial value of 0), or to assign a value of 0 explicitly.  If any
other value is assigned during the constructor, e.g. -1, other threads
calling the hash function might read a 0 from that field, or -1,
depending upon all kinds of factors that are impossible to control or
observe from a Java program.

Before Clojure 1.9.0, every collection did assign a value of -1 to
these fields during the constructor call, which was unsafe.  This was
fixed in the Clojure 1.9.0 release.  See this JIRA ticket for more
details: https://clojure.atlassian.net/browse/CLJ-2091

It links to this article on this pattern of writing Java code that has
intentional data races, but is still correct according to the Java
Memory Model:
http://jeremymanson.blogspot.com/2008/12/benign-data-races-in-java.html

Note that JavaScript run time environments are single threaded (not
counting WebWorkers, but as far as I know, no ClojureScript objects
are shared between the main thread and any WebWorker threads via
shared memory), so these issues do not arise, and any initial value
can be stored in the hash field without a problem.


# Java `.hashCode` vs. `clojure.core/hash`

Some background and history on why `.hashCode` and `clojure.core/hash`
return different values from each other can be found in the Clojure
equality guide, especially the ["Equality and hash"
section](https://clojure.org/guides/equality#equality_and_hash).


# Details in core.rrb-vector Clojure implementation

`core.rrb-vector`'s Clojure implementation should use an initial value
of the mutable hash fields of 0, for the same reasons described above
that any such fields should be initialized to 0 on the JVM.

In `core.rrb-vector` release 0.0.14 and earlier, these fields were
incorrectly initialized to -1.  Again, this does not cause any
problems in a single-threaded program, and would only cause problems
in some timing-dependent cases (perhaps rarely, but there is no
guarantee of this) in a multi-threaded program.

This is a list of classes in `core.rrb-vector`'s Clojure
implementation that were changed to correct this problem, after the
release of version 0.0.14:

* `VecSeq` - last field is Clojure _hasheq, second to last is Java _hash
* `Vector` - same as `VecSeq`
* `Transient` - no hash fields.

I believe that calling `clojure.core/hash` on a transient collection
falls through to some default hash implementation that is based on the
identity of the mutable object, and is not the same for all transients
with "the same" contents, the way it is for immutable collections.


## `VecSeq` methods and constructor calls

The constructor to `VecSeq` in method `withMeta` actually appears safe
to me to initialize the returned object with the same values as the
original collection.  The hash of the returned collection is
guaranteed to be the same as that of the collection on which
`withMeta` was called, so the initial values will either be 0, or the
correct final hash value.

All occurrences of the string VecSeq in the implementation are in the
one source file rrbt.clj, so I feel safe in saying I have corrected
all constructor calls to VecSeq.

I also corrected an unsafe data race in the implementation of method
`hasheq` for class `VecSeq`.  It was reading the field `_hasheq` twice
per call, instead of only once.  My fixed version reads that field at
most once.  See this article for the details of the changes I made and
why:
http://jeremymanson.blogspot.com/2008/12/benign-data-races-in-java.html


## `Vector` methods and constructor calls

The constructor to `Vector` in method `withMeta` actually appears safe
to me, for the same reason as it is for the same method in class
`VecSeq` described in the previous section.

Files containing constructor calls for class `Vector`:

* rrbt.clj - many, fixed
* rrb_vector.clj - fixed

Files checked for occurrences of `-1`:

* rrb_vector.clj - none remaining after constructor calls to `Vector`
  were fixed.
* debug.clj - only one, used to return a "not found" value from
  `first-diff`
* fork_join.clj - none, and no constructor calls
* interop.clj.clj - none, and no constructor calls
* nodes.clj - none, and no constructor calls to `Vector` or `VecSeq`.
  Many to `VecNode`, but it has no hash fields.
* protocols.clj - none, and no constructor calls
* rrbt.clj
  * Many occurrences of `Vector.` constructor calls were updated to
    use initial hash values of 0 instead of -1.
  * Fixed racy hash function/method bugs in these methods, for the
    same reason as described in previous section.
    * Vector/hashCode
    * Vector/hasheq
    * VecSeq/hasheq
* transients.clj - none, and no mentions of `Vector` or `Vecseq` or
  hash


# Details in core.rrb-vector ClojureScript implementation

Since there are no thread-safety issues here, the only thing to double
check is that the same value (e.g. 0, -1, or whatever constant value)
is used consistently in all places where a new object is created that
contains a cached hash field, and wherever the hash function is
calculated.

Classes in the ClojureScript `core.rrb-vector` implementation with a
hash field:

* `RRBChunkedSeq` - the last field of the constructor call, named `__hash`
* `Vector` - the last field of the constructor call, named `__hash`

All constructor calls for `RRBChunkedSeq` are in file rrbt.cljs, and
all use `nil` as the initial value for the field `__hash`.  Its
`-hash` method calls `caching-hash` provided by the ClojureScript core
code, which uses `nil` as the "hash not calculated yet" value.

All constructor calls for `Vector` are in a few files, and all use
`nil` as the initial value for the field `__hash`.  Its `-hash` method
also uses `caching-hash`, as described in previous paragraph.
