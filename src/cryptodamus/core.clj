(ns cryptodamus.core
  (:gen-class)
  (:require [cryptodamus.fetch :as api]
            [cryptodamus.utils :as utils]
            [cryptodamus.gui :as gui]
            [criterium.core :as criterium]))

(defn -main [& args]
  (gui/show-window))

(def btc-last-day2 (api/get-price "bitcoin" (utils/days-ago 20) (utils/days-ago 1)))
(def btc-last-day (vec (range 20)))

(defn dif
  "Returns element-wise differences between two equal length numeric sequences.
   Throws IllegalArgumentException if sequences have different lengths."
  [s1 s2]
  (if (= (count s1) (count s2))
    (map - s2 s1)
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

(defn all-below-limit?
  "Checks if non-empty sequence contains only elements with absolute values <= limit.
   Returns false for empty sequences."
  [s l]
  (if (seq s)
    (every? #(<= (Math/abs (double %)) (double l)) s)
    false))

(def x 10)
(all-below-limit? [80 61 80 100 90] x)
(all-below-limit? [1 2 3 4 9 10] x)
(all-below-limit? [] x)

(defn avg
  "Calculates the arithmetic mean in a single pass. Returns nil for empty collections."
  [s]
  (when (seq s)
    (let [[sum cnt] (reduce (fn [[s c] v] [(+ s v) (inc c)]) [0.0 0] s)]
      (/ sum cnt))))

(avg [1 2 3 4 5 6])
(avg [])

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
  [precision s]
  (map (partial round-to precision) s))

(round 3 [2.12313 5.51241 6.5111 10.5231 2.556])

; COMPARE VARIATIONS
(defn zero-anchoring
  "Anchors the starting point to zero by subtracting the first price from all prices."
  [prices]
  (when (seq prices)
    (let [base (double (first prices))]
      (map #(double (- % base)) prices))))

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

; not tested
(defn percentage-change [prices]
  (when (> (count prices) 1)
    (map #(* 100 (/ (- %2 %1) %1))
         prices
         (rest prices))))

(percentage-change [1 2 3])
(percentage-change [7 8 9])
(percentage-change [4 5 6])
(percentage-change [10 11 12])

; not tested
(defn price-differences [prices]
  (map - (rest prices) prices))

(price-differences [1 2 3])
(price-differences [4 5 6])
(price-differences [7 8 9])
(price-differences [10 11 12])

; not tested
(defn log-returns [prices]
  (map #(Math/log (/ %2 %1))
       prices
       (rest prices)))

(log-returns [1 2 3])
(log-returns [7 8 9])
(log-returns [4 5 6])
(log-returns [10 11 12])

(defn delta-avg
  "Calculates percentage deviation from average. Returns nil for empty collections.
   For zero average, calculates deviations relative to maximum absolute value."
  [s]
  (when-let [avg (avg s)]
    (cond
      (every? zero? s)
      (repeat (count s) 0.0)  ; all zeros -> return all zeros

      (zero? avg)
      (let [max-abs (apply max (map #(Math/abs %) s))]
        (map #(* 100.0 (/ % max-abs)) s))  ; normalize relative to max absolute value

      :else
      (map #(* 100.0 (/ (- % avg) avg)) s))))

(delta-avg [])
(delta-avg [25 30 30])
(delta-avg [30 30 30])
(delta-avg [0 0 0])
(delta-avg [-1 0 1])

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

(defn predict-price
  "Predicts future prices based on historical patterns.
   Returns top n predicted price sequences sorted by pattern match score."
  [price-chart n]
  (when-let [patterns (predict-pattern price-chart)]
    (let [last-price (double (last price-chart))
          sorted-patterns (sort-by :score > patterns)
          top-patterns (take n sorted-patterns)
          predictions (map (fn [{:keys [outcome]}]
                             (mapv (fn [pct]
                                     (* last-price (+ 100.0 pct) 0.01))
                                   outcome))
                           top-patterns)]
      (println "last-price" last-price)
      (println "sorted-patterns" sorted-patterns)
      (println "top-patterns" top-patterns)
      (println "predictions" predictions)
      {:predictions predictions
       :scores (mapv :score top-patterns)})))

(predict-price btc-last-day2 5)



; EVALUATE PREDICTION


(defn split-last-n
  "Splits a primitive double array into two double arrays."
  [^long x ^doubles arr]
  (let [n (alength arr)
        split-index (- n x)]
    [(java.util.Arrays/copyOfRange arr 0 split-index)
     (java.util.Arrays/copyOfRange arr split-index n)]))

(def test-array (double-array (range 1000000)))
(criterium/with-progress-reporting (criterium/quick-bench (split-last-n 200 test-array))) ; 1,458845 ms

(def train-data (get (split-last-n 5 (double-array btc-last-day2)) 0))
train-data

(def test-data (get (split-last-n 5 (double-array btc-last-day2)) 1))
test-data


(predict-price train-data 5)
test-data



(defn evaluate-prediction
  "Evaluates prediction accuracy against test data."
  [predictions test-data & {:keys [tolerance] :or {tolerance 5.0}}]
  (when (and (seq predictions) (seq test-data))
    (let [diffs (abs-dif predictions test-data)
          mean-error (double (/ (apply + diffs) (count diffs)))
          max-error (apply max diffs)
          within-tolerance (count (filter #(<= % tolerance) diffs))
          accuracy-pct (* 100.0 (/ within-tolerance (count diffs)))]
      {:mean-error mean-error
       :max-error max-error
       :within-tolerance within-tolerance
       :total-points (count diffs)
       :accuracy-pct accuracy-pct
       :differences diffs})))

(evaluate-prediction (first (:predictions (predict-price train-data 5))) test-data)


