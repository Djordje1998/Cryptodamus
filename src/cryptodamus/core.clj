(ns cryptodamus.core
  (:gen-class))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

(def btc {:h [10 20 30 40 50 60] :d [11 22 33 44 55 66]})
(def eth {:h [1 2 3 4 5 6] :d [1.1 2.2 3.3 4.4 5.5 6.6]})
(def historical-data {:BTC btc :ETH eth})

(defn get-historical-data [c i]
  (i (c historical-data)))

(defn predict-pattern [d]
  d)

(defn predict-price [c i]
  (predict-pattern (get-historical-data c i)))


