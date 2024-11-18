(ns cryptodamus.core-test
  (:use midje.sweet) 
  (:require [cryptodamus.core :refer :all]))


(facts "`split` splits strings on regular expressions and returns a vector"
      (str/split "a/b/c" #"/") => ["a" "b" "c"]
      (str/split "" #"irrelevant") => [""]
      (str/split "no regexp matches" #"a+\s+[ab]") => ["no regexp matches"])