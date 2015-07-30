# lighthouse

A Clojure library designed to leader election and node register/discover/balance in a service cluster by zookeeper.

## Usage

Add dependency in your project:

```clj
[lighthouse "0.1.0"]
```

Create a [curator](http://curator.apache.org/) framework client:

```clj
(require '[lighthouse.zk :as zk])
(def cli (zk/mk-client "localhost:2181"))
```


### Service register/discover/balance

In a service node:

```clj
(require '[lighthouse.discover :refer :all])

;;register a node to zookeeper
(register cli "user-service")
```

Every service node can register itself to zookeeper by above code.

The default registerd node id and data is the node's machine hostname,but you can special them by:

```clj
(def node (register cli "user-service" :id "node-1" :data "192.168.1.20:8080"))
```

When you shutdown the node, you can unregister it by `(unregister node)`.

In a service consumer, you can create a balancer for `user-service`:

```clj
(require '[lighthouse.discover :refer :all])

(def user-service-balancer (create-service-balancer cli "user-service"))
```

Then You can get an alive `user-service` node:

```
(user-service-balancer)
```

It returns a node value map: `{:id "node-1" :data "192.168.1.20:8080"}`。

The default balance type is `:round-robin`,you can create other balancer type:

```
(def user-service-balancer (create-service-balancer cli "user-service" :balancer-type :random))
(def user-service-balancer (create-service-balancer cli "user-service" :balancer-type :hash))
```

When you use `:hash` strategy, you must pass a key to balancer for hashing:

```clj
(user-service-balancer "a")
(user-service-balancer "b")
;;pass a default value that returned when can not find an alive node.
(user-service-balancer "a" {:id "default-node" :data "node1.ip"})
```
Stop the balancer:

```clj
(stop-service-balancer user-service-balancer)
```

### Leader election

Every node trys to elect a leader:

```clj
(require '[lighthouse.leader :refer :all])

(start-election cli "/leader_election"
  (fn [cli path id]
    (println id "got leadership."))
  (fn [cli path id]
    (println id "released leadership."))
  :id "node-1")
```

Each node pass it's id,if not present, the default id is the node's matchine hostname.

```clj
(start-election client path on-aquired on-released & {:keys [id]})
```

When the node got the leadership, the `on-aquired` would be called with curator client,path and itself id.
And when the node released the leadership, the `on-released` would be called with curator client,path and itself id.

The `start-election` returns a resettable promise, you can deliver it a true value to stop the election, or a false value to
release the leadership but still be in queue for next election:

```clj
(def p (start-election ......))

;;release the leadership,but still be in queue for next election
(deliver p false)
;;stop current node in election
(deliver p true)
```

When election starts, you can get current leader or participants:

```clj
(get-leader cli "/leader_election")
(get-participants cli "/leader_election")
```

Stop current node in election:

```
(stop)
```


## License

Copyright © 2015 dennis zhuang

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
