(defproject lighthouse "0.1.0-RC1"
  :description "A Clojure library designed to leader election and node register/discover/balance in a service cluster by zookeeper."
  :url "https://github.com/killme2008/lighthouse"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.apache.curator/curator-recipes "2.4.2"]]
  :profiles {:dev {:dependencies [[org.apache.curator/curator-test "2.4.2"]]}})
