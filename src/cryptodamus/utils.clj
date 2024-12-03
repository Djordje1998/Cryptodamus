(ns cryptodamus.utils
  (:require  [clojure.java.io :as io]
             [clojure.edn :as edn]))


(defn load-env-file
  "Load environment variables from a .edn file"
  [filename]
  (let [env-file (io/resource filename)]
    (when env-file
      (edn/read-string (slurp env-file)))))