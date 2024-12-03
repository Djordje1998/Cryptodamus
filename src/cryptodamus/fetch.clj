(ns cryptodamus.fetch
  (:require [clj-http.client :as client]
            [cheshire.core :as json]
            [cryptodamus.utils :refer :all]))

(def api-key-cointgecko (:coingecko (:api-keys (load-env-file "config.edn"))))

(defn get-coin-data [^String c ^Integer from ^Integer to]
  {:pre [(string? c) (integer? from) (integer? to)]}
  (client/get (str "https://api.coingecko.com/api/v3/coins/" c "/market_chart/range")
              {:headers {"x-cg-demo-api-key" api-key-cointgecko}
               :query-params {:vs_currency "usd"
                              :from (str from)         ;1704067200
                              :to (str to)             ;1711843200
                              :precision "2"}
               :accept :json}))

(def btc-prices
  (:prices (json/parse-string
            (:body (get-coin-data "bitcoin" 1704067200 1704153600)) true)))

(json/generate-string btc-prices)