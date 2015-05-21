(ns lighthouse.discover-test
  (:require [clojure.test :refer :all]
            [lighthouse.discover :refer :all]
            [lighthouse.test_util :refer :all]))

(defonce pns (atom {}))

(defn- register-node [n]
  (swap! pns assoc n (register @test-client "test_api" :id n :data n)))

(defn- unregister-node [n]
  (when-let [pn (get @pns n)]
    (unregister pn)
    (swap! pns dissoc n)))

(defn service-fixture [f]
  (register-node "p1")
  (register-node "p2")
  (register-node "p3")
  (f)
  (unregister-node "p1")
  (unregister-node "p2")
  (unregister-node "p3"))

(use-fixtures :once (compose-fixtures zk-fixture service-fixture))

(defn- get-nodes [b]
  (-> b meta :nodes deref))

(deftest test-balancer
  (let [b (create-service-balancer @test-client "test_api")]
    (while (not= 3 (count (get-nodes b)))
      (Thread/sleep 1000))
    (is (= #{"p1" "p2" "p3"} (apply hash-set (map :data (get-nodes b)))))
    (is (= #{"p1" "p2" "p3"} (apply hash-set (map :data
                                                  (repeatedly 3 (fn [] (b "a")))))))
    (unregister-node "p1")
    (while (not= 2 (count (get-nodes b)))
      (Thread/sleep 1000))
    (is (= #{"p2" "p3"} (apply hash-set (map :data (get-nodes b)))))
    (is (= #{"p2" "p3"} (apply hash-set (map :data
                                                  (repeatedly 2 (fn [] (b "a")))))))
    (register-node "p1")
    (while (not= 3 (count (get-nodes b)))
      (Thread/sleep 1000))
    (is (= #{"p1" "p2" "p3"} (apply hash-set (map :data (get-nodes b)))))
    (is (= #{"p1" "p2" "p3"} (apply hash-set (map :data
                                                  (repeatedly 3 (fn [] (b "a")))))))
    (testing "stop balancer"
      (stop-service-balancer b)
      (Thread/sleep 1000)
      (is (nil? (b "a")))
      (is (= 1 (b "a" 1))))))
