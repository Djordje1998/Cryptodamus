(ns cryptodamus.core
  (:gen-class)
  (:require [cryptodamus.fetch :as api]
            [cryptodamus.utils :as utils]
            [cryptodamus.gui :as gui]))

(defn -main [& args]
  (gui/show-window))

(def btc-last-day2 (api/get-price "bitcoin" (utils/days-ago 20) (utils/days-ago 1)))
(def btc-last-day (vec (range 20)))

; vraca procentualnu razliku izmedju 1 i 2 s1 i s2
(defn dif
  "Returns element-wise differences between two equal-length numeric sequences.
   Throws IllegalArgumentException if sequences have different lengths."
  [s1 s2]
  (if (= (count s1) (count s2))
    (map - s1 s2)
    (throw (IllegalArgumentException. "Sequences must be of the same length"))))

(dif [1 2 3] [4 1 6])

(defn abs-dif
  "Returns absolute element-wise differences between two equal-length numeric sequences.
   Throws IllegalArgumentException if sequences have different lengths."
  [s1 s2]
  (if (= (count s1) (count s2))
    (map #(Math/abs (- %1 %2)) s1 s2)
    (throw (IllegalArgumentException. "Sequences must be of the same length"))))

(abs-dif [1 2 3] [4 1 6])

; proverava da li je procentualna razlika uvek vece od x (minimalni stepen slicnosti)
(def x 10)
; unaprediti tako da dopusti da par elemenata nije u opsegu a opet prodje, miss-number and miss-amount
(defn all-below-limit?
  "Checks if non-empty sequence contains only elements with absolute values <= limit.
   Returns false for empty sequences."
  [s l]
  (and (seq s)
       (every? #(<= (Math/abs %) l) s)))

(all-below-limit? [80 61 80 100 90] x)
(all-below-limit? [1 2 3 4 9 10] x)
(all-below-limit? [] x)

; average of elements in array
(defn avg
  "Calculates the arithmetic mean in a single pass. Returns nil for empty collections."
  [s]
  (when (seq s)
    (let [[sum cnt] (reduce (fn [[s c] v] [(+ s v) (inc c)]) [0.0 0] s)]
      (/ sum cnt))))

(avg [1 2 3 4 5 6])
(avg [])

; round number to precision

(def ^:private common-scales
  "Precomputed scales for common precisions [1e0, 1e1, ..., 1e18]"
  (into-array Double/TYPE (map #(Math/pow 10 %) (range 0 19))))

(defn round-to
  "Rounds a number to given decimal precision using optimized primitive math.
   Precision must be between 0-18 for maximum performance."
  ^double [^long precision ^double n]
  (let [scale (if (<= 0 precision 18)
                (aget common-scales precision)
                (Math/pow 10 precision))
        scaled (* n scale)]
    (double (/ (Math/round scaled) scale))))

(round-to 3 0.23550)

; round items of array to precision
(defn round
  "Rounds each element in collection to specified precision."
  [precision a]
  (map (partial round-to precision) a))

(round 3 [2.12313 5.51241 6.5111 10.5231 2.556])

; COMPARE VARIATIONS
(defn zero-anchoring
  "Anchors the starting point to zero by subtracting the first price from all prices."
  [prices]
  (when (seq prices)
    (let [base (first prices)]
      (map #(- % base) prices))))

(zero-anchoring [1 2 3])
(zero-anchoring [4 5 6])
(zero-anchoring [7 8 9])
(zero-anchoring [10 11 12])

(defn relative-percent-change
  "Calculates percentage change relative to first element. Returns doubles."
  [s]
  (when-let [f (first s)]
    (let [base (double f)]
      (map (fn [e]
             (double (* 100.0 (/ (- (double e) base) base))))
           s))))

(relative-percent-change [99143.84 99644.38 81486.58 101584.41 101728.83])
(relative-percent-change [4 5 6])
(relative-percent-change [7 8 9])
(relative-percent-change [10 11 12])

(defn percentage-change [prices]
  (when (> (count prices) 1)
    (map #(* 100 (/ (- %2 %1) %1))
         prices
         (rest prices))))

(percentage-change [1 2 3])
(percentage-change [7 8 9])
(percentage-change [4 5 6])
(percentage-change [10 11 12])

(defn price-differences [prices]
  (map - (rest prices) prices))

(price-differences [1 2 3])
(price-differences [4 5 6])
(price-differences [7 8 9])
(price-differences [10 11 12])

(defn log-returns [prices]
  (map #(Math/log (/ %2 %1))
       prices
       (rest prices)))

(log-returns [1 2 3])
(log-returns [7 8 9])
(log-returns [4 5 6])
(log-returns [10 11 12])

; array of difference in % from avg
(defn delta-avg
  "Calculates percentage deviation from average. Returns nil for empty collections.
   Throws ArithmeticException when average is zero."
  [s]
  (when-let [avg (avg s)]
    (when (zero? avg)
      (throw (ArithmeticException. "Cannot calculate delta-avg with zero average")))
    (map #(* 100.0 (/ (- % avg) avg)) s)))

(delta-avg [])
(delta-avg [0 0 0])
(delta-avg [25 30 30])
(delta-avg [30 30 30])

(dif (delta-avg [25 30 30]) (delta-avg [30 30 30]))
(all-below-limit? (dif (delta-avg [25 30 30]) (delta-avg [30 30 30])) x)


(def cw 5) ; chunk-window
(def sw 5) ; skip-window
(def sig 1) ; significance

(def s1 (partition cw sw (range 100)))
s1
(take 5 (range 100))
(take-last 5 (range 100))
(last s1)
(count s1)
(first s1)
(rest s1)
; "Find pattern in past data and return expected pattern"
(defn predict-pattern [chart-data]
  (loop [c1 (take-last cw chart-data)
         s (partition cw sw chart-data)
         i 0
         r []]
    (let [first-seq (first s)
          second-seq (second s) ; staviti u r 
          delta-diff (abs-dif (delta-avg first-seq) (delta-avg c1))]
      (if (seq (rest s))
        (recur c1 (rest s) (inc i)
               (if (all-below-limit? delta-diff sig)
                 (do
                   (println "==>" i ". Good")
                   (println "first " first-seq)
                   (println "delta-diff " delta-diff)
                   (into r [{:score (- 100 (* 100 (/ (/ (apply + delta-diff) cw) sig)))
                             :match first-seq
                             :base (first first-seq)
                             :outcome (round 5 (relative-percent-change second-seq))}]))
                 (do
                   (println "==>" i ". Bad")
                   (println "first " first-seq)
                   (println "delta-diff " delta-diff)
                   r)))
        (do
          (println "End!")
          r)))))

(predict-pattern btc-last-day2)

; "User want to get future price of given currency base on specific interval"
(defn predict-price [price-chart]
  (predict-pattern price-chart))

(predict-price btc-last-day)