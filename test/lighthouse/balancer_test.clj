(ns lighthouse.balancer-test
  (:require [clojure.test :refer :all]
            [lighthouse.balancer :refer :all]))

(def test-nodes (vec (range 0 10)))

(deftest test-balancers
  (testing "round-robin"
    (let [b (create-balancer :round-robin)]
      (is (nil? (get-node b nil nil)))
      (is (nil? (get-node b [] nil)))
      (is (= 1 (get-node b test-nodes "")))
      (is (= 2 (get-node b test-nodes "")))
      (is (= 3 (get-node b test-nodes "")))))
  (testing "hash"
    (let [b (create-balancer :hash)]
      (is (nil? (get-node b nil nil)))
      (is (nil? (get-node b [] nil)))
      (is (= 1 (get-node b test-nodes "a")))
      (is (= 1 (get-node b test-nodes "a")))
      (is (= 8 (get-node b test-nodes "b")))
      (is (= 8 (get-node b test-nodes "b")))
      (is (= 6 (get-node b test-nodes "c")))
      (is (= 6 (get-node b test-nodes "c")))))
  (testing "random"
    (let [b (create-balancer :random)]
      (is (nil? (get-node b nil nil)))
      (is (nil? (get-node b [] nil)))
      (is (get-node b test-nodes "a"))
      (is (get-node b test-nodes "a"))
      (is (get-node b test-nodes "b"))
      (is (get-node b test-nodes "b"))
      (is (get-node b test-nodes "c")))))
