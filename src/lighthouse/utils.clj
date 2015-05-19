(ns lighthouse.utils
  (:import [java.util.concurrent CountDownLatch])
  (:refer-clojure :exclude [promise]))

(defn hostname
  "Returns current host name."
  []
  (.getHostName (java.net.InetAddress/getLocalHost)))

(defprotocol Resettable
  (reset-promise [this] "Reset the promise and return itself."))

(deftype ResettablePromise [^{:tag CountDownLatch :volatile-mutable true} d v]
  Resettable
  (reset-promise [this]
    (let [new-d (CountDownLatch. 1)]
      (reset! v new-d)
      (set! d new-d)
      this))
  clojure.lang.IDeref
  (deref [_] (.await d) @v)
  clojure.lang.IBlockingDeref
  (deref
    [_ timeout-ms timeout-val]
    (if (.await d timeout-ms java.util.concurrent.TimeUnit/MILLISECONDS)
      @v
      timeout-val))
  clojure.lang.IPending
  (isRealized [this]
    (zero? (.getCount d)))
  clojure.lang.IFn
  (invoke
    [this x]
    (when (and (pos? (.getCount d))
               (compare-and-set! v d x))
      (.countDown d)
      this)))

(defn promise
  "Returns a resettable promsie just like clojure.core/promsie except
   that it can be reset after be realized by 'reset-promise' function."
  {:added "1.1"
   :static true}
  []
  (let [d (CountDownLatch. 1)]
    (ResettablePromise. d
                        (atom d))))
