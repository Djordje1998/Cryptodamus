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
        (let [cw 5  ; chunk window size
              sig 1.0]  ; significance threshold
          
          (fact "perfect match should score 100"
                (core/calculate-pattern-score [0 0 0 0 0] 1.0 5) => (roughly 100.0 0.1))
          
          (fact "small differences should score around 65-70"
                (core/calculate-pattern-score [0.1 0.1 0.1 0.1 0.1] 1.0 5) => (roughly 67.0 1.0))
          
          (fact "moderate differences should score around 40-45"
                (core/calculate-pattern-score [0.5 0.5 0.5 0.5 0.5] 1.0 5) => (roughly 13 1.0))
          
          (fact "large differences should score around 20-25"
                (core/calculate-pattern-score [0.8 0.8 0.8 0.8 0.8] 1.0 5) => (roughly 4 1.0))
          
          (fact "differences near significance threshold should score around 15"
                (core/calculate-pattern-score [0.95 0.95 0.95 0.95 0.95] 1.0 5) => (roughly 2 1.0))
          
          (fact "scores should be sensitive to individual spikes"
                (let [mostly-good [0.1 0.1 0.9 0.1 0.1]
                      all-moderate [0.5 0.5 0.5 0.5 0.5]]
                  (core/calculate-pattern-score mostly-good sig cw) => 
                  (roughly 5.0 1.0)  ; Lower score due to the spike
                  (core/calculate-pattern-score all-moderate sig cw) =>
                  (roughly 13.0 1.0))) ; Better score with consistent moderate differences
          
          (fact "scoring should scale with significance"
                (let [diffs [0.5 0.5 0.5 0.5 0.5]]
                  (core/calculate-pattern-score diffs 5.0 cw) => (roughly 20.0 1.0)
                  (core/calculate-pattern-score diffs 1.0 cw) => (roughly 13.5 1.0)
                  (core/calculate-pattern-score diffs 0.1 cw) => (roughly 0.0 1.0)))))