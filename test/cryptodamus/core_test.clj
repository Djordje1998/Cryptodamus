(ns cryptodamus.core-test
  (:use midje.sweet)
  (:require [cryptodamus.core :refer :all]))

(facts "about 'avg'"
       (avg [1 2 3 4 5 6 7 8 9 10]) => 11/2
       (avg [-1 -2 -3 -4 -5]) => -3
       (avg [-1 -2 -3 4 5]) => 3/5
       (avg '(-1 -2 -3 4 5)) => 3/5
       (double (avg [-1 -2 -3 4 5])) => 0.6
       (avg [1]) => 1)

(facts "about 'round-to' for number"
       (round-to 2 5.556) => 5.56
       (round-to 1 5.556) => 5.6
       (round-to 0 5) => 5.0
       (round-to 2 5.554) => 5.55)

(facts "about 'round' for array"
       (round 2 [1.555 2.556 3.554]) => [1.56 2.56 3.55]
       (round 2 [3.555555 4.555556 5.555554]) => [3.56 4.56 5.56]
       (round 2 [1.11111 2.22222 3.33333]) => [1.11 2.22 3.33]
       (round 1 '(1.11111 2.22222 3.33333)) => [1.1 2.2 3.3]
       (round 0 [1.11111 2.22222 3.33333]) => [1.0 2.0 3.0])

(facts "about 'get-historical-data'"
       (fact "return not null"
             (get-historical-data :BTC :d) =not=> nil?
             (get-historical-data :ETH :h) =not=> nil?)
       (fact "return more then 5"
             (> (count (get-historical-data :BTC :d)) 5) => true
             (> (count (get-historical-data :ETH :h)) 5) => true))

(facts "about 'predict-pattern'"
       (fact "return not null"
             (predict-pattern (get-historical-data :BTC :d)) =not=> nil?
             (fact "return more then 5"
                   (> (count (predict-pattern (get-historical-data :BTC :d))) 5) => true)))

(facts "about 'predict-price'"
       (fact "return not null"
             (predict-price :BTC :d) =not=> nil?
             (predict-price :ETH :h) =not=> nil?)
       (fact "return more then 5"
             (> (count (predict-price :BTC :d)) 5) => true
             (> (count (predict-price :ETH :h)) 5) => true))
