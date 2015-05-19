(ns lighthouse.balancer
  (:import [java.util.concurrent.atomic AtomicLong]
           [java.util Random]))

(defprotocol Balancer
  (get-node [this nodes k] "Return a node by a balance strategy."))

(defrecord RoundRobinBalancer [^AtomicLong c]
  Balancer
  (get-node [_ nodes _]
    (get nodes (mod (.incrementAndGet c) (count nodes)))))

(defrecord RandomBalancer [^Random rand]
  Balancer
  (get-node [_ nodes _]
    (get nodes (.nextInt rand (count nodes)))))

(defrecord HashBalancer [hash-fn]
  Balancer
  (get-node [_ nodes k]
    (get nodes (mod (hash-fn k) (count nodes)))))

(defn create-balancer [type]
  (case type
    :hash (HashBalancer. hash)
    :random (RandomBalancer. (Random.))
    :round-robin (RoundRobinBalancer. (AtomicLong. 0))))
