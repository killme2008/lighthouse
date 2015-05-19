(ns lighthouse.zk
  (:import [org.apache.curator.framework CuratorFramework CuratorFrameworkFactory CuratorFrameworkFactory$Builder]
           [org.apache.curator.retry ExponentialBackoffRetry]))

(defn ^CuratorFramework mk-client
  "Create a started curator framework client."
  [connect-str & opts]
  (let [builder (CuratorFrameworkFactory/builder)
        h (merge
           (apply hash-map opts)
           {:connect-timeout 10000
            :session-timeout 10000
            :retry-interval 1000
            :retry-times 5
            :retry-sleep-ms 1000})]
    (doto builder
      (.connectString connect-str)
      (.connectionTimeoutMs (:connect-timeout h))
      (.sessionTimeoutMs (:session-timeout h))
      (.retryPolicy (ExponentialBackoffRetry. (:retry-interval h)
                                              (:retry-times h)
                                              (:retry-sleep-ms h))))
    (let [fk (.build builder)]
      (.start fk)
      fk)))
