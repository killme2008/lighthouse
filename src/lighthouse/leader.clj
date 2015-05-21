(ns lighthouse.leader
  (:require [lighthouse.utils :as u]
            [lighthouse.zk :as zk])
  (:import [java.util.concurrent CountDownLatch])
  (:refer-clojure :exclude [promise])
  (:import [org.apache.curator.framework CuratorFramework CuratorFrameworkFactory CuratorFrameworkFactory$Builder]
           [org.apache.curator.framework.recipes.leader LeaderSelectorListenerAdapter LeaderSelector CancelLeadershipException LeaderSelectorListener]))


(defonce selectors (atom {}))

(defn start-election
  "Start to elect a leader in the special zk path with optional node id.
   Returns a resettable promise, you can deliver it a true value when you want
   to release the leadership and CLOSE the selector, or deliver it a false value
   to release leadership but still in queue.

   If this node is elected as the leader,than the 'on-aquired' funciton will be
   called with the client ,path and id.When the leadership is released,
   the 'on-release' function will be called with the client, path and id."
  [client path on-aquired on-released & {:keys [id]}]
  (zk/ensure-path client path)
  (let [p (u/promise)
        id (or id (u/hostname))
        ^LeaderSelectorListener listener (proxy [LeaderSelectorListenerAdapter] []
                                           (takeLeadership [fk]
                                             ;;success to get leadership
                                             (when on-aquired
                                               (on-aquired fk path id))
                                             (let [shutdown (try
                                                              (deref p)
                                                              (catch InterruptedException _))]
                                               ;;release leadership
                                               (when on-released
                                                 (on-released fk path id))
                                               (if shutdown
                                                 (when-let [s (get-in @selectors [client path id])]
                                                   (swap! selectors
                                                          update-in
                                                          [client path]
                                                          #(dissoc % id))
                                                   (.close s))
                                                 (u/reset-promise p)))))
        ^LeaderSelector selector (LeaderSelector. client path listener)]
    (swap! selectors
           assoc-in
           [client path id]
           (doto selector
             (.setId id)
             (.autoRequeue)
             (.start)))
    p))

(defn get-participants
  "Returns all participants node id."
  [client path]
  (try
    (zk/ensure-path client path)
    (when-let [^LeaderSelector selector (-> @selectors (get-in [client path]) vals first)]
      (map #(.getId %)
           (seq (-> selector
                    (.getParticipants)))))
    (catch org.apache.zookeeper.KeeperException$NoNodeException e)))

(defn get-leader
  "Returns the leader node id"
  [client path]
  (try
    (zk/ensure-path client path)
    (when-let [^LeaderSelector selector (-> @selectors (get-in [client path]) vals first)]
      (-> selector
          (.getLeader)
          (.getId)))
    (catch org.apache.zookeeper.KeeperException$NoNodeException e)))

(defn stop
  "Stop all elections."
  []
  (doseq [s (mapcat vals (vals @selectors))]
    (.close s)))
