(ns lighthouse.leader-test
  (:require [clojure.test :refer :all]
            [lighthouse.leader :refer :all]
            [lighthouse.test_util :refer :all]))

(use-fixtures :once zk-fixture)

(deftest test-election
  (let [c (atom [])
        f (fn [_ _ id] (swap! c conj id))
        path "/test_election"
        p1 (start-election @test-client path f f :id "p1")
        _ (Thread/sleep 1000)
        p2 (start-election @test-client path f f :id "p2")]
    (while (nil? (get-leader @test-client path))
      (Thread/sleep 10))
    (is (= "p1" (get-leader @test-client path)))
    (is (= '("p1" "p2") (get-participants @test-client path)))
    (is (= ["p1"] @c))
    (deliver p1 false)
    (Thread/sleep 1000)
    (is (= "p2" (get-leader @test-client path)))
    (is (= '("p2" "p1") (get-participants @test-client path)))
    (is (= ["p1" "p1" "p2"] @c))
    (deliver p2 false)
    (Thread/sleep 1000)
    (is (= "p1" (get-leader @test-client path)))
    (is (= '("p1" "p2") (get-participants @test-client path)))
    (is (= ["p1" "p1" "p2" "p2" "p1"] @c))
    (deliver p1 true)
    (Thread/sleep 1000)
    (is (= "p2" (get-leader @test-client path)))
    (is (= '("p2") (get-participants @test-client path)))
    (is (= ["p1" "p1" "p2" "p2" "p1" "p1" "p2"] @c))
    (deliver p2 true)
    (Thread/sleep 1000)
    (is (nil? (get-leader @test-client path)))
    (is (nil? (seq (get-participants @test-client path))))
    (is (= ["p1" "p1" "p2" "p2" "p1" "p1" "p2" "p2"] @c))))
