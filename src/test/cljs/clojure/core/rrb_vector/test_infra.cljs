(ns clojure.core.rrb-vector.test-infra)

;; Enable tests to be run on versions of Clojure before 1.10, when
;; ex-message was added.

(defn ex-message-copy
  "Returns the message attached to the given Error / ExceptionInfo object.
  For non-Errors returns nil."
  [ex]
  (when (instance? js/Error ex)
    (.-message ex)))

(defn ex-cause-copy
  "Returns exception cause (an Error / ExceptionInfo) if ex is an
  ExceptionInfo.
  Otherwise returns nil."
  [ex]
  (when (instance? ExceptionInfo ex)
    (.-cause ex)))
