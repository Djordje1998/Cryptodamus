(ns cryptodamus.core-test
  (:use midje.sweet) 
  (:require [cryptodamus.core :refer :all]))

(facts "Return prediction of given currency and interval with more then 5 prices"
       (predict-price :BTC :d) =not=> nil?
       (> (count (predict-price :BTC :d)) 5) => true)
