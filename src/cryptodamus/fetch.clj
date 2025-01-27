(ns cryptodamus.fetch
  (:require [clj-http.client :as client]
            [cheshire.core :as json]
            [cryptodamus.utils :as utils]))

(def api-key-coingecko (-> "config.edn"
                          utils/load-env-file
                          :api-keys
                          :coingecko))


(defn get-chart-data
  "Fetches historical chart data for a cryptocurrency from CoinGecko API.
   Parameters:
   - coin: cryptocurrency id (e.g., 'bitcoin')
   - from: start timestamp (UNIX)
   - to: end timestamp (UNIX)
   - precision: decimal places for price values (optional, defaults to '2')
   - currency: target currency (optional, defaults to 'usd')"
  ([^String coin ^Integer from ^Integer to ^String precision ^String currency]
   {:pre [(string? coin) (integer? from) (integer? to) (string? precision) (string? currency)]}
   (try 
     (client/get (str "https://api.coingecko.com/api/v3/coins/" coin "/market_chart/range")
                 {:headers {"x-cg-demo-api-key" api-key-coingecko}
                  :query-params {:vs_currency currency
                               :from (str from)         ;1704067200
                               :to (str to)             ;1711843200
                               :precision precision}
                  :accept
                  :json})
     (catch Exception e
       (throw (ex-info "API request failed" 
                      {:coin coin :from from :to to} e)))))
  ([^String coin ^Integer from ^Integer to]
   {:pre [(string? coin) (integer? from) (integer? to)]}
   (get-chart-data coin from to "2" "usd")))

(defn get-prices-raw [^String coin ^Integer to ^Integer from]
  {:pre [(string? coin) (integer? from) (integer? to)]}
  (let [response (get-chart-data coin from to)]
    (or (:prices (json/parse-string (:body response) true))
        (throw (ex-info "No price data in response" 
                       {:coin coin :from from :to to})))))

(defn get-prices [^String coin ^Integer from ^Integer to]
  {:pre [(string? coin) (integer? from) (integer? to)]}
  (double-array (map #(second %) (get-prices-raw coin from to))))

(def price (get-prices "bitcoin" 1704067200 1704153600))

(def price2 (get-prices "bitcoin" (utils/days-ago 1) (utils/days-ago 10)))

(seq price2)
 
