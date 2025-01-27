(ns cryptodamus.fetch
  (:require [clj-http.client :as client]
            [cheshire.core :as json]
            [cryptodamus.utils :as utils]
            [clojure.string :as str]))

(def api-key-coingecko (-> "config.edn"
                           utils/load-env-file
                           :api-keys
                           :coingecko))
(def demo-api true)
(def supported-cryptocurrencies
  #{"bitcoin" "ethereum" "monero"})

(defn- validate-timestamp [^Integer timestamp]
  (let [current-time (quot (System/currentTimeMillis) 1000)
        min-timestamp (if demo-api (- current-time (* 365 24 60 60)) 1367174841)
        max-timestamp current-time]
    (when-not (<= min-timestamp timestamp max-timestamp)
      (throw (ex-info (str "Invalid timestamp. Must be between " min-timestamp
                           (if demo-api " (last 365 days)" " (Apr 2013)") " and current time")
                      {:timestamp timestamp
                       :min-allowed min-timestamp
                       :max-allowed max-timestamp})))))

(defn- validate-time-range [^Integer from ^Integer to]
  (validate-timestamp from)
  (validate-timestamp to)
  (when (>= from to)
    (throw (ex-info "Invalid time range: 'from' must be less than 'to'"
                    {:from from :to to}))))

(defn- validate-coin [^String coin]
  (when-not (contains? supported-cryptocurrencies coin)
    (throw (ex-info (str "Unsupported cryptocurrency: " coin "\n"
                         "Supported coins: " (str/join ", " supported-cryptocurrencies))
                    {:coin coin
                     :supported supported-cryptocurrencies}))))

(defn get-chart
  "Fetches historical chart data for a cryptocurrency from CoinGecko API.
   Parameters:
   - coin: cryptocurrency id (e.g., 'bitcoin')
   - from: start timestamp (UNIX)
   - to: end timestamp (UNIX)
   - precision: decimal places for price values (optional, defaults to '2')
   - currency: target currency (optional, defaults to 'usd')"
  ([^String coin ^Integer from ^Integer to ^String precision ^String currency]
   {:pre [(string? coin) (integer? from) (integer? to) (string? precision) (string? currency)]}
   (validate-coin coin)
   (validate-time-range from to)
   (try
     (client/get (str "https://api.coingecko.com/api/v3/coins/" coin "/market_chart/range")
                 {:headers {"x-cg-demo-api-key" api-key-coingecko}
                  :query-params {:vs_currency currency
                                 :from (str from)         ;1704067200
                                 :to (str to)             ;1711843200
                                 :precision precision}
                  :accept :json})
     (catch clojure.lang.ExceptionInfo e
       (let [status (:status (ex-data e))
             body (json/parse-string (:body (ex-data e)) true)
             error (or (get-in body [:error])
                       (get-in body [:status :error_message])
                       "Unknown error")]
         (if (#{401 403} status)
           (throw (ex-info (str "AUTHENTICATION ERROR: API authentication failed.\n"
                                "Please check your API key in config.edn\n"
                                "Status: " status "\n"
                                "Error: " error)
                           {:type :auth-error
                            :status status
                            :coin coin}))
           (throw (ex-info (str "API REQUEST ERROR: Request failed with status " status "\n"
                                "Error: " error)
                           {:coin coin :from from :to to}
                           e)))))
     (catch Exception e
       (throw (ex-info "UNEXPECTED ERROR: Error during API request"
                       {:coin coin :from from :to to}
                       e)))))
  ([^String coin ^Integer from ^Integer to]
   {:pre [(string? coin) (integer? from) (integer? to)]}
   (get-chart coin from to "2" "usd")))

(defn get-price-timeline [^String coin ^Integer to ^Integer from]
  {:pre [(string? coin) (integer? from) (integer? to)]}
  (let [response (get-chart coin from to)]
    (or (:prices (json/parse-string (:body response) true))
        (throw (ex-info "No price data in response"
                        {:coin coin :from from :to to})))))

(defn get-price [^String coin ^Integer from ^Integer to]
  {:pre [(string? coin) (integer? from) (integer? to)]}
  (double-array (mapv second (get-price-timeline coin from to))))

(def price2 (get-price "bitcoin" (utils/days-ago 1) (utils/days-ago 10)))

(seq price2)
 
