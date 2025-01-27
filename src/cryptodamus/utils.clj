(ns cryptodamus.utils
  (:require  [clojure.java.io :as io]
             [clojure.edn :as edn]))


(defn load-env-file
  "Load environment variables from a .edn file"
  [filename]
  (let [env-file (io/resource filename)]
    (when env-file
      (edn/read-string (slurp env-file)))))

(defn ->unix-timestamp
  "Convert a java.util.Date to Unix timestamp"
  [^java.util.Date date]
  (quot (.getTime date) 1000))

(defn unix-timestamp->date
  "Convert Unix timestamp to java.util.Date"
  [timestamp]
  (java.util.Date. (* timestamp 1000)))

(defn days-ago
  "Get Unix timestamp for n days ago"
  [n]
  (->unix-timestamp 
    (java.util.Date. 
      (- (System/currentTimeMillis) 
         (* n 24 60 60 1000)))))