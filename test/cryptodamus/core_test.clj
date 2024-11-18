(ns cryptodamus.core-test
  (:use midje.sweet) 
  (:require [cryptodamus.core :refer :all]))

(facts "Return prediction of given currency and interval"
       (predict-price :BTC :d) =not=> nil?)
