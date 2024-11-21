(ns cryptodamus.core
  (:gen-class))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

(def btc {:h [10 20 30 40 50 60] :d [11 22 33 44 55 66]})
(def eth {:h (vec (range 20)) :d [1.1 2.2 3.3 4.4 5.5 6.6]})
; "Historical data on currencies"
(def historical-data {:BTC btc :ETH eth})

; "Get historical data of currency with given interval"
(defn get-historical-data [c i]
  (i (c historical-data)))

; "Find pattern in past data and return expected pattern"
(defn predict-pattern [d]
  (let [s (partition 5 1 d)]
    (print s)))

(def s1 (partition 5 1 (range 20)))
(count s1)
(first s1)
(rest s1)

; vraca procentualnu razliku izmedju 1 i 2 s1 i s2
(defn matching-pattern-index [s1 s2]
  (map #(Math/abs (- %1 %2)) s1 s2))

(matching-pattern-index [1 2 3] [4 5 6])

; proverava da li je procentualna razlika uvek vece od x (minimalni stepen slicnosti)


(loop [s (partition 5 1 (range 20))
       i 0]
  (print s)
  (print (format "%d - " i))
  (println 
   (matching-pattern-index (first s) (first (rest s))))
  (if (seq s)
    (recur (rest s) (inc i))
    (println "End!")))

; average of elements in array
(defn avg [a]
  (/ (apply + a) (count a)))

; round number to precision
(defn round-to [precision n]
  (let [scale (Math/pow 10 precision)]
    (/ (Math/round (* n scale)) scale)))

(round-to 2 0.23131)

; round items of array to precision
(defn round [precision a]
  (map (partial round-to precision) a))
(round 3 [2.12313 5.51241 6.5111 10.5231 2.556])

; [ 2 5 6 10 2]
(def exmp [2 5 6 10 2])
(def exmp-d [2.23 5.145 6.456 10.567 2.12412])
(def exmp1 '(2 5 6 10 2))
(def exmp2-d '(3.1231 8.25321 9.13412 18.3325 9.231))
(apply + exmp1)
(avg exmp)
(avg exmp1)
(avg exmp2-d)
(Math/avg exmp)


; array of difference in % from avg
(defn delta-price [s]
  (let [a (avg s)]
     (map #(float (/ (- % a) a)) s)))

(delta-price exmp-d)
(round 3 (delta-price exmp-d))
(round 3 (delta-price exmp2-d))

(round 3 (matching-pattern-index 
          (round 3 (delta-price exmp-d))
          (round 3 (delta-price exmp2-d))))

(predict-pattern (get-historical-data :ETH :h))

; "User want to get future price of given currency base on specific interval"
(defn predict-price [c i]
  (predict-pattern (get-historical-data c i)))


