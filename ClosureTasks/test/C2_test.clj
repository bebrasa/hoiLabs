(ns C2-test
  (:require [clojure.test :refer :all]
            [C2 :refer :all]))

(defn prime? [n]
  (and (> n 1)
       (let [sqrt-n (Math/sqrt n)]
         (if (< sqrt-n 2)
           true
           (not-any? #(zero? (mod n %))
                     (range 2 (inc (int sqrt-n))))))))

(deftest test-primes
  (testing "first 10 primes"
    (is (= (take 10 (primes))
           [2 3 5 7 11 13 17 19 23 29])))

  (testing "all numbers in first 100 primes are actually prime"
    (is (every? prime? (take 100 (primes)))))

  (testing "sequence is strictly increasing"
    (is (apply < (take 100 (primes))))))
