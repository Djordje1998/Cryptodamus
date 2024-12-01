(ns cryptodamus.fetch
  (:require [clj-http.client :as client]
            [cheshire.core :as json]
            [environ.core :refer [env]]))

(println "API Key loaded from env: " (env :COINGECKO_API_KEY))

(def api-key (env :COINGECKO_API_KEY))
api-key

(defn get-bitcoin-data [] (client/get "https://api.coingecko.com/api/v3/coins/bitcoin/market_chart/range"
                                      {:headers {"x-cg-demo-api-key" api-key}
                                       :query-params {:vs_currency "usd"
                                                      :from "1704067200" ;1704067200
                                                      :to "1704153600"   ;1711843200
                                                      :precision "2"}
                                       :accept :json}))



(def btc-prices (:prices (json/parse-string (:body (get-bitcoin-data)) true)))
btc-prices

(json/generate-string btc-prices)