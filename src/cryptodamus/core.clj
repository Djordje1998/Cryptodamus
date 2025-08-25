(ns cryptodamus.core)

(defn dif
  "Returns element-wise differences between two equal length numeric sequences.
   Throws IllegalArgumentException if sequences have different lengths."
  [s1 s2]
  (if (= (count s1) (count s2))
    (map - s2 s1)
    (throw (IllegalArgumentException. "Sequences must be of the same length"))))

(defn abs-dif
  "Returns absolute element-wise differences between two equal-length numeric sequences.
   Throws IllegalArgumentException if sequences have different lengths."
  [s1 s2]
  (if (= (count s1) (count s2))
    (map #(Math/abs (- %2 %1)) s1 s2)
    (throw (IllegalArgumentException. "Sequences must be of the same length"))))

(defn all-below-limit?
  "Checks if non-empty sequence contains only elements with absolute values <= limit.
   Returns false for empty sequences."
  [s l]
  (if (seq s)
    (every? #(<= (Math/abs (double %)) (double l)) s)
    false))

(defn avg
  "Calculates the arithmetic mean in a single pass. Returns nil for empty collections."
  [s]
  (when (seq s)
    (let [[sum cnt] (reduce (fn [[s c] v] [(+ s v) (inc c)]) [0.0 0] s)]
      (/ sum cnt))))

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

(defn round
  "Rounds each element in collection to specified precision."
  [precision s]
  (map (partial round-to precision) s))

(defn zero-anchoring
  "Anchors the starting point to zero by subtracting the first price from all prices."
  [prices]
  (when (seq prices)
    (let [base (double (first prices))]
      (map #(double (- % base)) prices))))

(defn relative-percent-change
  "Calculates percentage change relative to first element. Returns doubles."
  [s]
  (when-let [f (first s)]
    (let [base (double f)]
      (map (fn [e] (double (* 100.0 (/ (- (double e) base) base)))) s))))

(defn percentage-change
  "Calculates period-over-period percentage changes for a price sequence."
  [prices]
  (when (> (count prices) 1)
    (map (fn [prev curr]
           (when (zero? prev)
             (throw (ArithmeticException. "Division by zero in percentage-change")))
           (let [delta (- (double curr) (double prev))
                 base  (double prev)
                 pct (if (neg? base)
                       (* 100.0 (/ (Math/abs delta) (Math/abs base)))
                       (* 100.0 (/ delta base)))]
              (round-to 14 pct)))
         prices
         (rest prices))))

(defn price-differences
  "Calculates absolute price differences between consecutive periods."
  [prices]
  (map - (rest prices) prices))

(defn log-returns
  "Calculates logarithmic returns for a price sequence."
  [prices]
  (map #(Math/log (/ %2 %1))
       prices
       (rest prices)))

(defn delta-avg
  "Calculates percentage deviation from average. Returns nil for empty collections.
   For zero average, calculates deviations relative to maximum absolute value."
  [s]
  (when-let [avg (avg s)]
    (cond
      (every? zero? s)
      (repeat (count s) 0.0)

      (zero? avg)
      (let [max-abs (apply max (map #(Math/abs %) s))]
        (map #(* 100.0 (/ % max-abs)) s))

      :else
      (map #(* 100.0 (/ (- % avg) avg)) s))))

(def comparator-fns
  "Map of available comparator functions for pattern matching"
  {"delta-avg" delta-avg
   "percentage-change" percentage-change
   "log-returns" log-returns
   "price-differences" price-differences
   "relative-percent-change" relative-percent-change
   "zero-anchoring" zero-anchoring})

(defn calculate-pattern-score
  "Calculate pattern matching score based on differences and significance threshold.
   Returns a score between 0-100, where 100 means perfect match and 0 means no match."
  [delta-diff sig cw]
  (let [avg-abs-diff (/ (apply + (map #(Math/abs %) delta-diff)) cw)
        max-abs-diff (apply max (map #(Math/abs %) delta-diff))
        avg-score (max 0.0 (- 1.0 (/ avg-abs-diff sig)))
        max-score (max 0.0 (- 1.0 (/ max-abs-diff sig)))
        combined-score (+ (* 0.7 avg-score) (* 0.3 max-score))]
    (* 100.0 combined-score)))

(defn predict-pattern
  "Find pattern in past data and return expected pattern outcomes."
  [chart-data cw sw pw sig comparator-fn]
  (cond
    (or (nil? chart-data) (empty? chart-data)) []
    (< (count chart-data) cw) (throw (IllegalArgumentException. "Insufficient data for given chunk window"))
    :else
    (loop [c1 (take-last cw chart-data)
           s (partition cw sw chart-data)
           i 0
           r []]
      (let [first-seq (first s)
            outcome-start-idx (+ (* i sw) cw)
            outcome-seq (when (< outcome-start-idx (count chart-data))
                          (take pw (drop outcome-start-idx chart-data)))
            delta-diff (dif (comparator-fn first-seq) (comparator-fn c1))]
        (if (and (seq (rest s)) outcome-seq (>= (count outcome-seq) pw))
          (recur c1 (rest s) (inc i)
                 (if (all-below-limit? delta-diff sig)
                   (into r [{:score (calculate-pattern-score delta-diff sig cw)
                             :match first-seq
                             :base (first first-seq)
                             :outcome (round 5 (relative-percent-change outcome-seq))}])
                   r))
          r)))))

(defn sort-patterns-by-score
  "Sorts pattern matches by score in descending order."
  [patterns]
  (sort-by :score > patterns))

(defn print-sorted-patterns
  "Prints pattern matches sorted by score in a readable format."
  [patterns]
  (doseq [{:keys [score match outcome]} (sort-patterns-by-score patterns)]
    (println "\nScore:" (round-to 2 score))
    (println "Pattern:" (vec match))
    (println "Expected outcome:" (vec outcome))))

(defn predict-price
  "Predicts future prices based on historical patterns.
   Returns top nop predicted price sequences sorted by pattern match score."
  [price-chart nop cw sw pw sig comparator-fn]
  (println "predict-price") 
  (when-let [patterns (predict-pattern price-chart cw sw pw sig comparator-fn)]
    (let [last-price (double (last price-chart))
          sorted-patterns (sort-patterns-by-score patterns)
          top-patterns (take nop sorted-patterns)
          predictions (mapv (fn [{:keys [outcome]}]
                              (mapv (fn [pct] (* last-price (/ (+ 100.0 pct) 100.0)))
                                    outcome))
                            top-patterns)]
      {:predictions predictions
       :scores (mapv :score top-patterns)})))

(defn split-last-n
  "Splits a primitive double array into two double arrays.
   Returns [training-data test-data] where test-data contains the last x elements."
  [^long x ^doubles arr]
  (let [n (alength arr)
        split-index (- n x)]
    [(java.util.Arrays/copyOfRange arr 0 split-index)
     (java.util.Arrays/copyOfRange arr split-index n)]))

(defn abs-percentage-diff
  "Calculates absolute percentage difference between predicted and actual values."
  [predicted actual]
  (when-not (zero? actual)
    (* 100.0 (/ (Math/abs (- predicted actual)) (Math/abs actual)))))

(defn evaluate-predictions
  "Evaluates prediction accuracy against test data with configurable tolerance."
  [predictions test-data & {:keys [tolerance] :or {tolerance 5.0}}]
  ;; Tests expect an exception when predictions is empty
  (when-not (seq predictions)
    (throw (ArithmeticException. "Empty predictions")))
  (let [all-diffs (filter some?
                          (mapcat (fn [prediction]
                                    (map abs-percentage-diff prediction test-data))
                                  predictions))
        total-points (count all-diffs)]
    (if (zero? total-points)
      {:mean-error Double/MAX_VALUE
       :max-error Double/MAX_VALUE
       :within-tolerance 0
       :total-points 0
       :accuracy-pct 0.0
       :num-predictions (count predictions)}
      (let [mean-error (/ (reduce + all-diffs) total-points)
            max-error (apply max all-diffs)
            within-tolerance (count (filter #(<= % tolerance) all-diffs))
            accuracy-pct (* 100.0 (/ within-tolerance total-points))]
        {:mean-error mean-error
         :max-error max-error
         :within-tolerance within-tolerance
         :total-points total-points
         :accuracy-pct accuracy-pct
         :num-predictions (count predictions)}))))

(defn optimize-config
  "Tests different configurations and comparator functions, returns configs sorted by prediction accuracy."
  [price-data {:keys [cw-range sw-range pw-range sig-range comparator-fns]} nop]
  (let [configs (for [cw cw-range
                      sw sw-range
                      pw pw-range
                      sig sig-range
                      [comp-name comp-fn] (or comparator-fns comparator-fns)]
                  {:cw cw :sw sw :pw pw :sig sig :comparator-name comp-name :comparator-fn comp-fn})
        evaluate-config (fn [config]
                          (try
                            (let [window-size (:cw config)
                                  [train test] (split-last-n window-size (double-array price-data))
                                  prediction-result (predict-price train nop (:cw config) (:sw config) (:pw config) (:sig config) (:comparator-fn config))]
                              (if (and prediction-result (:predictions prediction-result) (seq (:predictions prediction-result)))
                                (let [evaluation (evaluate-predictions (:predictions prediction-result) test)]
                                  (assoc config
                                         :score (:accuracy-pct evaluation)
                                         :mean-error (:mean-error evaluation)))
                                (assoc config
                                       :score 0.0
                                       :mean-error Double/MAX_VALUE)))
                            (catch Exception e
                              (assoc config
                                     :score 0.0
                                     :mean-error Double/MAX_VALUE))))
        evaluated-configs (map evaluate-config configs)
        sorted-configs (sort-by :score > evaluated-configs)]
    (take 10 sorted-configs)))
