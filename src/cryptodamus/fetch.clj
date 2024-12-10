(ns cryptodamus.fetch
  (:require [clj-http.client :as client]
            [cheshire.core :as json]
            [cryptodamus.utils :refer :all]))

(def api-key-cointgecko (-> "config.edn"
                            load-env-file
                            :api-keys
                            :coingecko))


(defn get-chart-data
  ([^String coin ^Integer from ^Integer to ^String precision ^String currency]
   {:pre [(string? coin) (integer? from) (integer? to) (string? precision) (string? currency)]}
   (client/get (str "https://api.coingecko.com/api/v3/coins/" coin "/market_chart/range")
               {:headers {"x-cg-demo-api-key" api-key-cointgecko}
                :query-params {:vs_currency currency
                               :from (str from)         ;1704067200
                               :to (str to)             ;1711843200
                               :precision precision}
                :accept
                :json}))
  ([^String coin ^Integer from ^Integer to]
   {:pre [(string? coin) (integer? from) (integer? to)]}
   (get-chart-data coin from to "2" "usd")))

(defn get-prices-raw [^String coin ^Integer from ^Integer to]
  {:pre [(string? coin) (integer? from) (integer? to)]}
  (:prices (json/parse-string
            (:body (get-chart-data coin from to)) true)))

(defn get-prices [^String coin ^Integer from ^Integer to]
  {:pre [(string? coin) (integer? from) (integer? to)]}
  (double-array (map #(second %) (get-prices-raw coin from to))))

(def price (get-prices "bitcoin" 1704067200 1704153600))

(seq price)
 
