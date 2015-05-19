(ns lighthouse.discover
  (:import [org.apache.curator.framework.recipes.nodes
            PersistentEphemeralNode
            PersistentEphemeralNode$Mode]
           [org.apache.curator.framework.recipes.cache PathChildrenCache
            PathChildrenCacheEvent
            PathChildrenCacheEvent$Type
            PathChildrenCacheListener])
  (:require [lighthouse.utils :as u]
            [lighthouse.balancer :as b]
            [lighthouse.zk :as zk]))


(def default-path "/lighthouse/services")

(defn- resolve-opts [opts]
  (let [{:keys [path id data]
         :or {path default-path
              id (u/hostname)
              data (u/hostname)}} (apply hash-map opts)
              path (u/normalize-path path)]
    {:path path
     :id id
     :data data}))

(defn register
  "Register a service in zookeeper.Pass a service name."
  [cli service & opts]
  (let [{:keys [path id data]} (resolve-opts opts)
        node-path (format "%s/%s" path service)]
    (zk/ensure-path cli node-path)
    (->
     cli
     (PersistentEphemeralNode.
      PersistentEphemeralNode$Mode/EPHEMERAL_SEQUENTIAL
      (format "%s/%s/%s" path service id)
      (.getBytes data))
     (.start))))

(defn get-current-nodes [pc service & opts]
  (let [{:keys [path]} (resolve-opts opts)
        node-path (format "%s/%s" path service)]
    (distinct
     (map (fn [child]
            {:id (when-let [p (.getPath child)]
                   (.substring p (-> node-path count inc)))
             :data (String. (.getData child))})
          (seq (.getCurrentData pc))))))

(defn start-watch
  "Start to watch a service in zookeeper for service nodes adding/removing.
   When it happens, the on-updated function will be called with node list,and
   each node is a map of {:id id :data data}.
   Returns the PathChildrenCache.
   See http://curator.apache.org/apidocs/org/apache/curator/framework/recipes/cache/PathChildrenCache.html"
  [cli service on-updated & opts]
  (let [{:keys [path id]} (resolve-opts opts)
        node-path (format "%s/%s" path service)
        pc (PathChildrenCache.  cli node-path true)]
    (println path service)
    (zk/ensure-path cli node-path)
    (..
     pc
     (getListenable)
     (addListener
      (proxy [PathChildrenCacheListener] []
        (childEvent [cli e]
          (when (#{PathChildrenCacheEvent$Type/CHILD_REMOVED
                   PathChildrenCacheEvent$Type/CHILD_ADDED}
                 (.getType e))
            (when on-updated
              (on-updated
               (apply get-current-nodes pc service opts))))))))
    (.start pc)
    pc))

(defn stop-watch
  "Stop the PathChildrenCache."
  [pc]
  (.close pc))
