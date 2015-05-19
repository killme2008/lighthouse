(ns lighthouse.election
  (:require [lighthouse.utils :as u])
  (:import [java.util.concurrent CountDownLatch])
  (:refer-clojure :exclude [promise])
  (:import [org.apache.curator.framework CuratorFramework CuratorFrameworkFactory CuratorFrameworkFactory$Builder]
           [org.apache.curator.framework.recipes.leader LeaderSelectorListenerAdapter LeaderSelector CancelLeadershipException LeaderSelectorListener]))


(defonce leader-selectors (atom {}))

(defn start-election
  "Start to elect a leader in the special zk path with optional node id.
   Returns a resettable promise, you can deliver it a true value when you want
   to release the leadership and CLOSE the selector, or deliver it a false value
   to release leadership but still in queue.

   If this node is elected as the leader,than the 'on-aquired' funciton will be
   called with the path, id and client.When the leadership is released,
   the 'on-release' function will be called with the path, id and client."
  [client path on-aquired on-released & {:keys [id]}]
  (println client path on-aquired on-released id)
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
                                                 (when-let [s (get-in @leader-selectors [client path])]
                                                   (swap! leader-selectors
                                                          update-in
                                                          [client]
                                                          #(dissoc % path))
                                                   (.close s))
                                                 (u/reset-promise p)))))
        ^LeaderSelector selector (LeaderSelector. client path listener)]
    (swap! leader-selectors
           assoc-in
           [client path]
           (doto selector
             (.setId id)
             (.autoRequeue)
             (.start)))
    p))

(defn get-participants [client path]
  (when-let [^LeaderSelector selector (-> @leader-selectors (get-in [client path]))]
    (map #(.getId %)
         (seq (-> selector
                  (.getParticipants))))))

(defn get-leader
  "Returns the leader's id"
  [client path]
  (when-let [^LeaderSelector selector (-> @leader-selectors (get-in [client path]))]
    (-> selector
        (.getLeader)
        (.getId))))

(defn stop []
  (doseq [s (mapcat vals (vals @leader-selectors))]
    (.close s)))
