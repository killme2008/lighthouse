(ns lighthouse.test_util
  (:require [lighthouse.zk :as zk])
  (:import [org.apache.curator.test TestingServer]))

(defonce test-server (atom nil))
(defonce test-client (atom nil))

(defn start-zk []
  (compare-and-set! test-server nil
                    (TestingServer. 2383))
  (compare-and-set! test-client nil
                    (zk/mk-client "localhost:2383")))

(defn stop-zk []
  (when-let [c @test-client]
    (.close c))
  (when-let [s @test-server]
    (.stop s)))

(defn zk-fixture [f]
  (start-zk)
  (f)
  (stop-zk))
