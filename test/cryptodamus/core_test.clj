(ns cryptodamus.core-test
  (:use midje.sweet) 
  (:require [cryptodamus.core :refer :all]))

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
