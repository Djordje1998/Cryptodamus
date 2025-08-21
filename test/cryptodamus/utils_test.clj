(ns cryptodamus.utils-test
  (:require [midje.sweet :refer [facts fact => throws roughly]]
            [cryptodamus.utils :as utils]
            [clojure.java.io :as io])
  (:import [java.util Date]))

(def roughly-precision 1e-10)

(facts "about 'load-env-file' function"
       (fact "loads existing EDN configuration file from resources/config.edn.example"
             (let [expected-config {:api-keys {:coingecko "your-coingecko-api-key-here"}}]
               (utils/load-env-file "config.edn.example") => expected-config))
       
       (fact "returns nil for non-existent files"
             (utils/load-env-file "non-existent-file.edn") => nil))

(facts "about '->unix-timestamp' function"
       (fact "converts Date to Unix timestamp correctly"
             (let [test-date (Date. 1640995200000)]
               (utils/->unix-timestamp test-date) => 1640995200))
       
       (fact "handles epoch date (1970-01-01)"
             (let [epoch-date (Date. 0)]
               (utils/->unix-timestamp epoch-date) => 0))
       
       (fact "handles recent dates"
             (let [recent-date (Date. 1704067200000)]
               (utils/->unix-timestamp recent-date) => 1704067200))
       
       (fact "handles dates with milliseconds precision"
             (let [date-with-millis (Date. 1640995200500)]
               (utils/->unix-timestamp date-with-millis) => 1640995200)))

(facts "about 'unix-timestamp->date' function"
       (fact "converts Unix timestamp to Date correctly"
             (let [timestamp 1640995200
                   converted-date (utils/unix-timestamp->date timestamp)]
               (.getTime converted-date) => 1640995200000))
       
       (fact "handles epoch timestamp (0)"
             (let [converted-date (utils/unix-timestamp->date 0)]
               (.getTime converted-date) => 0))
       
       (fact "handles recent timestamps"
             (let [timestamp 1704067200
                   converted-date (utils/unix-timestamp->date timestamp)]
               (.getTime converted-date) => 1704067200000))
       
       (fact "roundtrip conversion preserves timestamp"
             (let [original-timestamp 1640995200
                   date (utils/unix-timestamp->date original-timestamp)
                   back-to-timestamp (utils/->unix-timestamp date)]
               back-to-timestamp => original-timestamp)))

(facts "about 'days-ago' function"
       (fact "calculates timestamp for days in the past"
             (let [current-time (System/currentTimeMillis)
                   one-day-ago (utils/days-ago 1)
                   expected-time (- current-time (* 1 24 60 60 1000))
                   expected-timestamp (quot expected-time 1000)]
               one-day-ago => (roughly expected-timestamp 2)))
       
       (fact "handles zero days (today)"
             (let [current-timestamp (quot (System/currentTimeMillis) 1000)
                   today (utils/days-ago 0)]
               today => (roughly current-timestamp 2)))
       
       (fact "handles multiple days ago"
             (let [seven-days-ago (utils/days-ago 7)
                   one-day-ago (utils/days-ago 1)
                   expected-diff (* 6 24 60 60)]
               (- one-day-ago seven-days-ago) => (roughly expected-diff 2)))
       
       (fact "returns integer timestamp"
             (integer? (utils/days-ago 5)) => true) 
       
       (fact "handles large number of days"
             (let [year-ago (utils/days-ago 365)]
               (integer? year-ago) => true
               (< year-ago (quot (System/currentTimeMillis) 1000)) => true)))

(facts "about timestamp conversion consistency"
       (fact "->unix-timestamp and unix-timestamp->date are inverse operations"
             (let [original-date (Date. 1640995200000)
                   timestamp (utils/->unix-timestamp original-date)
                   converted-back (utils/unix-timestamp->date timestamp)]
               (quot (.getTime converted-back) 1000) => (quot (.getTime original-date) 1000)))
       
       (fact "days-ago returns valid timestamps that can be converted to dates"
             (let [timestamp (utils/days-ago 10)
                   date (utils/unix-timestamp->date timestamp)]
               (instance? Date date) => true
               (< (.getTime date) (System/currentTimeMillis)) => true)))

(facts "about edge cases and error handling"
       (fact "handles very old dates"
             (let [old-date (Date. 0)] ; Unix epoch
               (utils/->unix-timestamp old-date) => 0))
       
       (fact "handles future dates"
             (let [future-date (Date. (+ (System/currentTimeMillis) (* 365 24 60 60 1000)))
                   timestamp (utils/->unix-timestamp future-date)]
               (> timestamp (quot (System/currentTimeMillis) 1000)) => true))
       
       (fact "days-ago with negative values gives future timestamps"
             (let [future-timestamp (utils/days-ago -1)
                   current-timestamp (quot (System/currentTimeMillis) 1000)]
               (> future-timestamp current-timestamp) => true)))
