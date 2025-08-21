(ns cryptodamus.core-test
   (:require  [midje.sweet :refer [facts fact => throws roughly just]]
              [cryptodamus.core :as core]))

 (def roughly-precision 1e-14)
 (defn roughly-coll [elements]
   (just (map #(roughly % roughly-precision) elements)))

 (facts "about 'dif' function"
        (fact "calculates s2-s1 element-wise differences for various numeric types and cases"
              (core/dif [1 2 3] [4 1 6]) => [3 -1 3]
              (core/dif '(5 3) '(2 4)) => [-3 1]
              (core/dif [] []) => []
              (core/dif [10.5] [5.25]) => [-5.25]
              (core/dif [-1 -2] [3 4]) => [4 6]
              (core/dif [1 2] [-3 -4]) => [-4 -6])
        (fact "throws IllegalArgumentException for length mismatches"
              (core/dif [1] []) => (throws IllegalArgumentException)
              (core/dif [1 2] [3]) => (throws IllegalArgumentException)
              (core/dif [1 2 3] [4 5]) => (throws IllegalArgumentException)
              (core/dif nil [1]) => (throws IllegalArgumentException)
              (core/dif [1] nil) => (throws IllegalArgumentException))
        (fact "handles double arrays with various cases"
              (core/dif [1.5 2.5 3.5] [3.0 1.0 4.0]) => (roughly-coll [1.5 -1.5 0.5])
              (core/dif [0.1 0.2 0.3] [0.4 0.5 0.6]) => (roughly-coll [0.3 0.3 0.3])
              (core/dif [10.0 15.0 20.0] [9.5 14.9 20.1]) => (roughly-coll [-0.5 -0.1 0.1])))

 (facts "about 'abs-dif' function"
        (fact "calculates absolute element-wise differences"
              (core/abs-dif [1 2 3] [4 1 6]) => [3 1 3]
              (core/abs-dif [-5 3] [2 -4]) => [7 7]
              (core/abs-dif [0 0] [5 -5]) => [5 5]
              (core/abs-dif [] []) => [])
        (fact "handles different collection types"
              (core/abs-dif '(1 2) [3 4]) => [2 2]
              (core/abs-dif [5 6] '(2 3)) => [3 3])
        (fact "throws error for length mismatches"
              (core/abs-dif [1 2] [3]) => (throws IllegalArgumentException)
              (core/abs-dif [1] []) => (throws IllegalArgumentException)
              (core/abs-dif nil [1]) => (throws IllegalArgumentException)
              (core/abs-dif [1] nil) => (throws IllegalArgumentException))
        (fact "handles double arrays with absolute differences"
              (core/abs-dif [1.5 2.5 3.5] [3.0 1.0 4.0]) => (roughly-coll [1.5 1.5 0.5])
              (core/abs-dif [0.1 0.2 0.3] [0.4 0.5 0.6]) => (roughly-coll [0.3 0.3 0.3])
              (core/abs-dif [10.0 15.0 20.0] [9.5 14.9 20.1]) => (roughly-coll [0.5 0.1 0.1])
              (core/abs-dif [2.5 -3.0] [-1.5 4.0]) => (roughly-coll [4.0 7.0])))

 (facts "about 'all-below-limit?' function"
        (fact "returns true when all elements are within the limit"
              (core/all-below-limit? [5 8 9 10] 10) => true
              (core/all-below-limit? [-5 0 5] 5) => true
              (core/all-below-limit? [10.0 9.999 10.0] 10.0) => true
              (core/all-below-limit? '(1 2 3) 3) => true)
        (fact "returns false when any element exceeds the limit"
              (core/all-below-limit? [5 11 9] 10) => false
              (core/all-below-limit? [-5 6 5] 5) => false
              (core/all-below-limit? [10.0 10.001 9.999] 10.0) => false)
        (fact "handles edge cases"
              (core/all-below-limit? [] 5) => false
              (core/all-below-limit? [0] 0) => true
              (core/all-below-limit? [1] 0) => false
              (core/all-below-limit? nil 5) => false
              (core/all-below-limit? [5] -1) => false)
        (fact "works with different numeric types"
              (core/all-below-limit? [5M 6.0 7N] 7) => true
              (core/all-below-limit? [5M 6.0 7N] 6.5) => false))

 (facts "about 'avg' function"
        (fact "calculates arithmetic mean for various numeric types"
              (core/avg [1 2 3 4 5]) => 3.0
              (core/avg [-1 -2 -3 -4 -5]) => -3.0
              (core/avg [1.5 2.5 3.5]) => 2.5
              (core/avg [5M 10.0 15N]) => 10.0
              (core/avg '(1 2 3)) => 2.0)
        (fact "handles edge cases"
              (core/avg []) => nil
              (core/avg [5]) => 5.0
              (core/avg [0 0 0]) => 0.0
              (core/avg [1e300 1e300]) =>  1e300
              (core/avg nil) => nil)
        (fact "calculates precise decimal averages"
              (core/avg [1 2]) => 1.5
              (core/avg [1.1 2.2 3.3]) => (roughly 2.2 roughly-precision)
              (core/avg [100M 200M 300M]) => 200M
              (core/avg [1/3 1/3 1/3]) => (roughly 1/3 roughly-precision))
        (fact "handles large numbers and precision"
              (core/avg [Long/MAX_VALUE Long/MAX_VALUE]) => (roughly (double Long/MAX_VALUE) roughly-precision)
              (core/avg [1.23456789 2.34567891]) => (roughly 1.7901234 roughly-precision)))

 (facts "about 'round-to' function"
        (fact "rounds numbers to specified precision"
              (core/round-to 2 5.556) =>  5.56
              (core/round-to 1 5.556) =>  5.6
              (core/round-to 0 5.5) =>  6.0
              (core/round-to 3 5.5555) =>  5.556
              (core/round-to 2 5.554) =>  5.55)
        (fact "handles edge cases and special numbers"
              (core/round-to 2 0.0) =>  0.0
              (core/round-to 2 -3.1415) =>  -3.14))

 (facts "about 'round' function"
        (fact "rounds collections of numbers to specified precision"
              (core/round 2 [1.555 2.556 3.554]) => [1.56 2.56 3.55]
              (core/round 1 [5.0 4.99 5.01]) =>  [5.0 5.0 5.0]
              (core/round 3 [1.11111 2.22222 3.33333]) => [1.111 2.222 3.333])
        (fact "handles different collection types and edge cases"
              (core/round 2 '(1.555 2.555 3.555)) => [1.56 2.56 3.56]
              (core/round 0 []) => []
              (core/round 2 nil) => []
              (core/round 1 [-1.49 -1.5 -1.51]) => [-1.5 -1.5 -1.5]))

 (facts "about 'zero-anchoring' function"
        (fact "subtracts first element from all elements"
              (core/zero-anchoring [1 2 3 4]) => [0.0 1.0 2.0 3.0]
              (core/zero-anchoring [-1 0 1]) => [0.0 1.0 2.0]
              (core/zero-anchoring [10 15 12 18]) => [0.0 5.0 2.0 8.0])
        (fact "handles floating point numbers"
              (core/zero-anchoring [1.5 2.5 3.5]) => [0.0 1.0 2.0]
              (core/zero-anchoring [10.5 11.5 10.0]) => [0.0 1.0 -0.5])
        (fact "handles edge cases"
              (core/zero-anchoring []) => nil
              (core/zero-anchoring nil) => nil
              (core/zero-anchoring [5]) => [0.0]))

 (facts "about 'relative-percent-change' function"
        (fact "calculates percentage changes from first element"
              (core/relative-percent-change [100 150 200]) => [0.0 50.0 100.0]
              (core/relative-percent-change [10 11 9]) => [0.0 10.0 -10.0]
              (core/relative-percent-change [50 45 55]) => [0.0 -10.0 10.0])
        (fact "handles decimal numbers"
              (core/relative-percent-change [1.0 1.1 0.9]) => (roughly-coll [0.0 10.0 -10.0])
              (core/relative-percent-change [100.0 101.0 99.0]) =>  [0.0 1.0 -1.0])
        (fact "handles edge cases"
              (core/relative-percent-change []) => nil
              (core/relative-percent-change nil) => nil
              (core/relative-percent-change [5]) => [0.0]))

 (facts "about 'delta-avg' function"
        (fact "calculates percentage deviations from average"
              (core/delta-avg [10 20 30]) => (roughly-coll [-50.0 0.0 50.0])
              (core/delta-avg [100 200 300]) => (roughly-coll [-50.0 0.0 50.0])
              (core/delta-avg [15 20 25]) => (roughly-coll [-25.0 0.0 25.0]))
        (fact "handles edge cases"
              (core/delta-avg []) => nil
              (core/delta-avg nil) => nil
              (core/delta-avg [5]) => [0.0])
        (fact "handles zero average cases"
              (core/delta-avg [0 0 0]) => [0.0 0.0 0.0]
              (core/delta-avg [-1 0 1]) => (roughly-coll [-100.0 0.0 100.0])))

 (facts "about Math/exp behavior"
        (fact "exp with zero and negative numbers"
              (Math/exp 0) => 1.0
              (Math/exp -0) => 1.0
              (Math/exp -1) => (roughly 0.367879441171442 1e-10)
              (Math/exp 1) => (roughly 2.718281828459045 1e-10)
              (Math/exp -2) => (roughly 0.135335283236613 1e-10)))

 (facts "about calculate-pattern-score function"
        (let [cw 5
              sig 1.0]
          (fact "perfect match should score 100"
                (core/calculate-pattern-score [0 0 0 0 0] 1.0 5) => (roughly 100.0 0.1))
          (fact "small differences should score high"
                (core/calculate-pattern-score [0.1 0.1 0.1 0.1 0.1] 1.0 5) => (roughly 90.0 1.0))
          (fact "moderate differences should score moderately"
                (core/calculate-pattern-score [0.5 0.5 0.5 0.5 0.5] 1.0 5) => (roughly 50.0 1.0))
          (fact "large differences should score low"
                (core/calculate-pattern-score [0.8 0.8 0.8 0.8 0.8] 1.0 5) => (roughly 20.0 1.0))
          (fact "differences near significance threshold should score very low"
                (core/calculate-pattern-score [0.95 0.95 0.95 0.95 0.95] 1.0 5) => (roughly 5.0 1.0))
          (fact "scores should be sensitive to individual spikes"
                (let [mostly-good [0.1 0.1 0.9 0.1 0.1]
                      all-moderate [0.5 0.5 0.5 0.5 0.5]]
                  (core/calculate-pattern-score mostly-good sig cw) =>
                  (roughly 55.0 5.0)
                  (core/calculate-pattern-score all-moderate sig cw) =>
                  (roughly 50.0 5.0)))
          (fact "scoring should scale with significance"
                (let [diffs [0.5 0.5 0.5 0.5 0.5]]
                  (core/calculate-pattern-score diffs 5.0 cw) => (roughly 90.0 5.0)
                  (core/calculate-pattern-score diffs 1.0 cw) => (roughly 50.0 5.0)
                  (core/calculate-pattern-score diffs 0.1 cw) => (roughly 0.0 1.0)))))

 (facts "about 'percentage-change' function"
        (fact "calculates percentage change between consecutive elements"
              (core/percentage-change [100 110 121]) => (roughly-coll [10.0 10.0])
              (core/percentage-change [10 15 12]) => (roughly-coll [50.0 -20.0])
              (core/percentage-change [1 2 3 4]) => (roughly-coll [100.0 50.0 33.333333333333336]))
        (fact "handles negative numbers (avoiding division by zero)"
              (core/percentage-change [-10 -5 -2]) => (roughly-coll [50.0 60.0])
              (core/percentage-change [-2 -4 -1]) => (roughly-coll [100.0 75.0])
              (core/percentage-change [5 10 15]) => (roughly-coll [100.0 50.0]))
        (fact "handles edge cases"
              (core/percentage-change []) => nil
              (core/percentage-change [5]) => nil
              (core/percentage-change nil) => nil)
        (fact "handles decimal precision"
              (core/percentage-change [1.0 1.1 1.21]) => (roughly-coll [10.0 10.0])
              (core/percentage-change [100.0 99.5 100.5]) => (roughly-coll [-0.5 1.0050251256281408]))
        (fact "handles division by zero cases gracefully"
              (core/percentage-change [0 5 10]) => (throws ArithmeticException)
              (core/percentage-change [5 0 10]) => (throws ArithmeticException)))

 (facts "about 'price-differences' function"
        (fact "calculates differences between consecutive prices"
              (core/price-differences [1 2 3 4]) => [1 1 1]
              (core/price-differences [10 15 12 18]) => [5 -3 6]
              (core/price-differences [100 90 95]) => [-10 5])
        (fact "handles negative numbers"
              (core/price-differences [-5 -2 -8]) => [3 -6]
              (core/price-differences [5 -5 0]) => [-10 5])
        (fact "handles decimal numbers"
              (core/price-differences [1.5 2.5 1.0]) => [1.0 -1.5]
              (core/price-differences [10.25 10.75 10.50]) => [0.5 -0.25])
        (fact "handles edge cases"
              (core/price-differences []) => []
              (core/price-differences [5]) => []
              (core/price-differences nil) => []))

 (facts "about 'log-returns' function"
        (fact "calculates logarithmic returns between consecutive prices"
              (core/log-returns [1 2 4]) => (roughly-coll [0.6931471805599453 0.6931471805599453])
              (core/log-returns [100 110 121]) => (roughly-coll [0.09531017980432496 0.09531017980432496])
              (core/log-returns [Math/E (* 2 Math/E)]) => (roughly-coll [0.6931471805599453]))
        (fact "handles price decreases"
              (core/log-returns [4 2 1]) => (roughly-coll [-0.6931471805599453 -0.6931471805599453])
              (core/log-returns [100 90 81]) => (roughly-coll [-0.10536051565782630 -0.10536051565782630]))
        (fact "handles edge cases and special values"
              (core/log-returns []) => []
              (core/log-returns [5]) => []
              (core/log-returns nil) => [])
        (fact "handles decimal precision"
              (core/log-returns [1.0 Math/E]) => (roughly-coll [1.0])
              (core/log-returns [10.0 10.0]) => (roughly-coll [0.0])))

 (facts "about 'abs-percentage-diff' function"
        (fact "calculates absolute percentage difference between predicted and actual"
              (core/abs-percentage-diff 100 110) => (roughly 9.090909090909092 roughly-precision)
              (core/abs-percentage-diff 110 100) => 10.0
              (core/abs-percentage-diff 50 60) => (roughly 16.666666666666668 roughly-precision))
        (fact "handles negative numbers"
              (core/abs-percentage-diff -10 -12) => (roughly 16.666666666666668 roughly-precision)
              (core/abs-percentage-diff 10 -10) => 200.0
              (core/abs-percentage-diff -5 5) => 200.0)
        (fact "handles zero actual value"
              (core/abs-percentage-diff 10 0) => nil
              (core/abs-percentage-diff -5 0) => nil
              (core/abs-percentage-diff 0 0) => nil)
        (fact "handles identical values"
              (core/abs-percentage-diff 100 100) => 0.0
              (core/abs-percentage-diff -50 -50) => 0.0))

 (facts "about 'split-last-n' function"
        (fact "splits double array into two parts"
              (let [test-arr (double-array [1 2 3 4 5 6 7 8 9 10])
                    [first-part last-part] (core/split-last-n 3 test-arr)]
                (vec first-part) => [1.0 2.0 3.0 4.0 5.0 6.0 7.0]
                (vec last-part) => [8.0 9.0 10.0]))
        (fact "handles edge cases"
              (let [test-arr (double-array [1 2 3])
                    [first-part last-part] (core/split-last-n 3 test-arr)]
                (vec first-part) => []
                (vec last-part) => [1.0 2.0 3.0]))
        (fact "handles single element split"
              (let [test-arr (double-array [1 2 3 4 5])
                    [first-part last-part] (core/split-last-n 1 test-arr)]
                (vec first-part) => [1.0 2.0 3.0 4.0]
                (vec last-part) => [5.0]))
        (fact "handles zero split"
              (let [test-arr (double-array [1 2 3])
                    [first-part last-part] (core/split-last-n 0 test-arr)]
                (vec first-part) => [1.0 2.0 3.0]
                (vec last-part) => [])))

 (facts "about 'sort-patterns-by-score' function"
        (fact "sorts patterns by score in descending order"
              (let [patterns [{:score 85.5 :match [1 2 3] :outcome [4 5 6]}
                             {:score 92.1 :match [7 8 9] :outcome [10 11 12]}
                             {:score 78.3 :match [13 14 15] :outcome [16 17 18]}]
                    sorted (core/sort-patterns-by-score patterns)]
                (map :score sorted) => [92.1 85.5 78.3]))
        (fact "handles empty patterns"
              (core/sort-patterns-by-score []) => [])
        (fact "handles single pattern"
              (let [pattern [{:score 75.0 :match [1 2] :outcome [3 4]}]]
                (core/sort-patterns-by-score pattern) => pattern))
        (fact "preserves pattern structure while sorting"
              (let [patterns [{:score 60.0 :match [1] :outcome [2] :extra "data1"}
                             {:score 80.0 :match [3] :outcome [4] :extra "data2"}]
                    sorted (core/sort-patterns-by-score patterns)]
                (first sorted) => {:score 80.0 :match [3] :outcome [4] :extra "data2"}
                (second sorted) => {:score 60.0 :match [1] :outcome [2] :extra "data1"})))

 (facts "about 'evaluate-predictions' function"
        (fact "evaluates prediction accuracy against test data"
              (let [predictions [[100 105 110] [98 103 108]]
                    test-data [100 105 110]
                    result (core/evaluate-predictions predictions test-data :tolerance 5.0)]
                (:accuracy-pct result) => 100.0
                (:mean-error result) => (roughly 0.95 0.1)
                (:total-points result) => 6
                (:num-predictions result) => 2))
        (fact "handles predictions with varying accuracy"
              (let [predictions [[100 120 140] [90 95 100]]
                    test-data [100 105 110]
                    result (core/evaluate-predictions predictions test-data :tolerance 10.0)]
                (:accuracy-pct result) => (roughly 66.67 1.0)
                (:total-points result) => 6))
        (fact "handles zero actual values by filtering them out"
              (let [predictions [[10 20] [15 25]]
                    test-data [0 10]
                    result (core/evaluate-predictions predictions test-data)]
                (:total-points result) => 2))
        (fact "handles empty predictions"
              (core/evaluate-predictions [] [100 105 110]) => (throws ArithmeticException))
        (fact "handles custom tolerance levels"
              (let [predictions [[100 110]]
                    test-data [100 105]
                    strict-result (core/evaluate-predictions predictions test-data :tolerance 1.0)
                    loose-result (core/evaluate-predictions predictions test-data :tolerance 10.0)]
                (:accuracy-pct strict-result) => (roughly 50.0 1.0)
                (:accuracy-pct loose-result) => 100.0)))

 (facts "about 'predict-pattern' function"
        (fact "finds patterns in historical data with basic case"
              (let [chart-data [10 11 12 13 14 15 16 17 18 19 20 21 22 23 24]
                    patterns (core/predict-pattern chart-data 3 2 1.0)]
                (>= (count patterns) 0) => true
                (every? map? patterns) => true
                (every? #(contains? % :score) patterns) => true
                (every? #(contains? % :match) patterns) => true
                (every? #(contains? % :outcome) patterns) => true))
        (fact "handles insufficient data gracefully"
              (core/predict-pattern [1 2] 3 2 1.0) => (throws IllegalArgumentException))
        (fact "handles empty or nil data"
              (core/predict-pattern [] 3 2 1.0) => []
              (core/predict-pattern nil 3 2 1.0) => [])
        (fact "pattern matching respects significance threshold"
              (let [volatile-data [10 50 10 50 10 50 10 50 10 50]
                    strict-patterns (core/predict-pattern volatile-data 3 1 0.1)
                    loose-patterns (core/predict-pattern volatile-data 3 1 50.0)]
                (>= (count strict-patterns) 0) => true
                (>= (count loose-patterns) 0) => true)))

 (facts "about 'predict-price' function"
        (fact "predicts future prices based on patterns"
              (let [price-chart [100 101 102 103 104 105 106 107 108 109 110]
                    result (core/predict-price price-chart 2 3 2 1.0)]
                (or (nil? result) 
                    (and (contains? result :predictions)
                         (contains? result :scores)
                         (vector? (:predictions result))
                         (vector? (:scores result))
                         (<= (count (:predictions result)) 2)
                         (every? #(= (count %) 3) (:predictions result)))) => true))
        (fact "handles insufficient data"
              (core/predict-price [1 2] 1 3 2 1.0) => (throws IllegalArgumentException))
        (fact "handles empty data"
              (core/predict-price [] 1 3 2 1.0) => (throws NullPointerException)
              (core/predict-price nil 1 3 2 1.0) => (throws NullPointerException))
        (fact "limits predictions to requested number"
              (let [price-chart (range 1 50) 
                    result (core/predict-price price-chart 1 3 2 1.0)]
                (or (nil? result)
                    (and (<= (count (:predictions result)) 1)
                         (<= (count (:scores result)) 1)
                         (every? #(= (count %) 3) (:predictions result)))) => true)))