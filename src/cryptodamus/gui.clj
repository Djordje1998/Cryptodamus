(ns cryptodamus.gui
  (:require [seesaw.core :as s]
            [cryptodamus.fetch :as api]
            [cryptodamus.utils :as utils]
            [cryptodamus.core :as core]
            [clojure.java.io :as io])
  (:import [org.jfree.chart ChartPanel ChartFactory]
           [org.jfree.data.xy XYSeries XYSeriesCollection]
           [java.awt Color]
           [javax.swing JFrame]
           [java.util Date]
           [com.toedter.calendar JDateChooser]
           [org.jfree.chart.renderer.xy XYLineAndShapeRenderer]))

;; Define atoms
;; Use java.util.Date for simplicity with JDateChooser
(def selected-coin (atom "bitcoin"))
(def start-date (atom (Date. (- (System/currentTimeMillis) (* 60 24 60 60 1000))))) ; default 60 days ago
(def end-date (atom (Date.))) ; today
;; Toggle to show actual future data over prediction window
(def show-future? (atom false))
;; Parameter atoms with default values
(def cw-param (atom 5))  ; chunk-window
(def sw-param (atom 5))  ; skip-window
(def pw-param (atom 5))  ; predict-window
(def sig-param (atom 1)) ; significance
(def nop-param (atom 5)) ; number of predictions

(defn create-chart-panel []
  (let [series (XYSeries. "Actual")
        dataset (XYSeriesCollection. series)
        chart (ChartFactory/createXYLineChart
               "Crypto Price Chart"
               "Price Points"
               "Price (USD)"
               dataset
               org.jfree.chart.plot.PlotOrientation/VERTICAL
               true true false)
        plot (.getPlot chart)
        ;; Get the renderer and configure tooltips
        renderer (doto (if (instance? org.jfree.chart.renderer.xy.XYLineAndShapeRenderer (.getRenderer plot))
                         (.getRenderer plot)
                         (org.jfree.chart.renderer.xy.XYLineAndShapeRenderer.)))
        ;; Create tooltip generator
        tooltip-gen (reify org.jfree.chart.labels.XYToolTipGenerator
                      (generateToolTip [_ dataset series item]
                        (let [x (.getXValue dataset series item)
                              y (.getYValue dataset series item)
                              series-name (.getSeriesKey dataset series)]
                          (format "%s: Point %.0f - $%.2f" series-name x y))))]

    ;; Make sure shapes are visible for tooltips and configure tooltips
    (doto ^org.jfree.chart.renderer.xy.XYLineAndShapeRenderer renderer
      (.setDefaultShapesVisible true)
      (.setDefaultShapesFilled true)
      (.setDrawSeriesLineAsPath true)
      (.setDefaultShape (java.awt.Rectangle. -3 -3 6 6))
      (.setDefaultToolTipGenerator tooltip-gen))    ;; Set tooltip generator only on renderer

    (.setBackgroundPaint plot Color/WHITE)
    (doto plot
      (.setDomainGridlinePaint (Color. 220 220 220))
      (.setRangeGridlinePaint (Color. 220 220 220))
      (.setRenderer renderer))  ;; Set the modified renderer
    (ChartPanel. chart true)))

(defn- set-series-color!
  "Helper to set series color with optional alpha on the XYLineAndShapeRenderer."
  [^XYLineAndShapeRenderer renderer ^long idx ^Color color]
  (.setSeriesPaint renderer idx color)
  (.setSeriesFillPaint renderer idx color)
  (.setSeriesOutlinePaint renderer idx color))

(defn update-chart [chart-panel coin ^Date start ^Date end & {:keys [cw sw pw sig nop] :or {cw @cw-param sw @sw-param pw @pw-param sig @sig-param nop @nop-param}}]
  (let [chart (.getChart chart-panel)
        plot (.getPlot chart)
        ^XYSeriesCollection dataset (.getDataset plot)
        ^XYLineAndShapeRenderer renderer (.getRenderer plot)]
    (try
      (when (or (nil? start) (nil? end))
        (throw (ex-info "Start and end dates must be selected" {})))
      (let [from (utils/->unix-timestamp start)
            to (utils/->unix-timestamp end)]
        (when (>= from to)
          (throw (ex-info "Start date must be before end date" {:from from :to to})))
        (let [prices-arr (api/get-price coin from to)
              prices (vec (seq prices-arr))
              n (count prices)
              ;; Build actual series
              actual (XYSeries. (str coin " Actual"))
              ;; Predictions (may be empty if not enough data)
              pred-result (when (>= n (max 2 cw))
                            (core/predict-price prices nop cw sw pw sig))
              predictions (:predictions pred-result)
              scores (:scores pred-result)
              max-pred-len (if (seq predictions)
                             (apply max (map count predictions))
                             0)]
          ;; populate actual series
          (doseq [i (range n)]
            (.add actual (double i) (double (nth prices i))))
          ;; Reset dataset
          (.removeAllSeries dataset)
          ;; Add actual series first
          (let [idx0 (.getSeriesCount dataset)]
            (.addSeries dataset actual)
            (set-series-color! renderer idx0 (Color. 33 150 243)) ; blue
            (.setSeriesStroke renderer idx0 (java.awt.BasicStroke. 2.0)))
          ;; Add prediction series with different colors and scores in legend
          (when (seq predictions)
            (let [pred-colors [Color/RED (Color. 255 100 100) (Color. 200 0 0) 
                              (Color. 255 150 150) (Color. 150 0 0)]]
              (doseq [[i prediction score] (map vector (range) predictions scores)]
                (let [pred-series (XYSeries. (str "Prediction " (inc i) " (Score: " (format "%.1f" score) ")"))]
                  (doseq [j (range (count prediction))]
                    (.add pred-series (double (+ n j)) (double (nth prediction j))))
                  (.addSeries dataset pred-series)
                  ;; Set different prediction colors
                  (set-series-color! renderer (+ 1 i) (nth pred-colors i (Color. 255 0 0)))
                  (.setSeriesStroke renderer (+ 1 i) (java.awt.BasicStroke. 2.0))))))

          ;; Optionally overlay actual data from end date to current date
          (when @show-future?
            (let [today (Date.)
                  millis-per-day (* 24 60 60 1000)
                  days-diff (Math/ceil (/ (- (.getTime today) (.getTime ^Date end)) millis-per-day))]
              (when (pos? days-diff)
                (let [from-future (utils/->unix-timestamp end)
                      to-future (utils/->unix-timestamp today)
                      fut-arr (api/get-price coin from-future to-future)
                      fut (vec (seq fut-arr))
                      fut-len (count fut)
                      start-x (dec n)
                      fut-series (XYSeries. "Actual (from end date)")]
                  (doseq [j (range fut-len)]
                    (.add fut-series (double (+ start-x (inc j))) (double (nth fut j))))
                  (let [idx (.getSeriesCount dataset)]
                    (.addSeries dataset fut-series)
                    (set-series-color! renderer idx (Color. 46 204 113)) ; green
                    (.setSeriesStroke renderer idx (java.awt.BasicStroke. 2.5)))))))

          (.fireChartChanged chart)
          {:max-pred-len max-pred-len}))
      (catch Throwable ex
        (s/alert (str "Error updating chart: " (.getMessage ex)))))
  )
)
  
  (defn create-controls [chart-panel]
  (let [start-picker (doto (JDateChooser.)
                       (.setPreferredSize (java.awt.Dimension. 120 25)))
        end-picker (doto (JDateChooser.)
                     (.setPreferredSize (java.awt.Dimension. 120 25)))
        coin-box (s/combobox :model (sort (seq api/supported-cryptocurrencies)))
        future-toggle (s/checkbox :text "Show actual future" :selected? @show-future?)
        ;; Parameter input fields with very compact widths
        cw-field (doto (s/text :text (str @cw-param) :columns 1)
                   (s/config! :preferred-size [30 :by 20]))
        sw-field (doto (s/text :text (str @sw-param) :columns 1)
                   (s/config! :preferred-size [30 :by 20]))
        pw-field (doto (s/text :text (str @pw-param) :columns 1)
                   (s/config! :preferred-size [30 :by 20]))
        sig-field (doto (s/text :text (str @sig-param) :columns 1)
                    (s/config! :preferred-size [40 :by 20]))
        nop-field (doto (s/text :text (str @nop-param) :columns 1)
                    (s/config! :preferred-size [30 :by 20]))
        ;; Generate prediction button
        predict-btn (s/button :text "Generate New Prediction")
        ;; Optimize button
        optimize-btn (s/button :text "Optimize Config")
        ;; Reset to default button
        reset-btn (s/button :text "Reset to Default")
        ;; Future toggle is always enabled - shows actual prices from end date to current date
        enable-future-toggle (fn [_ _]
                               ;; Always keep the toggle enabled
                               (s/config! future-toggle :enabled? true))
        update-fn (fn [_]
                    (let [start (.getDate start-picker)
                          end (.getDate end-picker)
                          coin (s/selection coin-box)]
                      (when coin (reset! selected-coin coin))
                      (when (and start end)
                        (reset! start-date start)
                        (reset! end-date end)
                        (let [{:keys [max-pred-len]} (update-chart chart-panel @selected-coin @start-date @end-date)]
                          (enable-future-toggle end max-pred-len)))))
        predict-fn (fn [_]
                     (try
                       (let [cw-val (Integer/parseInt (s/text cw-field))
                             sw-val (Integer/parseInt (s/text sw-field))
                             pw-val (Integer/parseInt (s/text pw-field))
                             sig-val (Double/parseDouble (s/text sig-field))
                             nop-val (Integer/parseInt (s/text nop-field))]
                         (when (and (pos? cw-val) (pos? sw-val) (pos? pw-val) (pos? sig-val) (pos? nop-val))
                           (reset! cw-param cw-val)
                           (reset! sw-param sw-val)
                           (reset! pw-param pw-val)
                           (reset! sig-param sig-val)
                           (reset! nop-param nop-val)
                           (let [{:keys [max-pred-len]} (update-chart chart-panel @selected-coin @start-date @end-date
                                                                      :cw cw-val :sw sw-val :pw pw-val :sig sig-val :nop nop-val)]
                             (enable-future-toggle @end-date max-pred-len))))
                       (catch NumberFormatException _
                         (s/alert "Please enter valid numeric values for all parameters"))))
        ;; Optimize function
        optimize-fn (fn [_]
                      (try
                        (s/alert "Optimizing configuration... This may take a moment.")
                        (let [prices-arr (api/get-price @selected-coin 
                                                       (utils/->unix-timestamp @start-date)
                                                       (utils/->unix-timestamp @end-date))
                              prices (vec (seq prices-arr))
                              optimal-configs (core/optimize-config prices 
                                                                   {:cw-range [3 4 5 6 7]
                                                                    :sw-range [2 3 4 5]
                                                                    :sig-range [0.5 1.0 1.5 2.0]})]
                          (when (seq optimal-configs)
                            (let [best-config (first optimal-configs)]
                              (reset! cw-param (:cw best-config))
                              (reset! sw-param (:sw best-config))
                              (reset! sig-param (:sig best-config))
                              (s/text! cw-field (str (:cw best-config)))
                              (s/text! sw-field (str (:sw best-config)))
                              (s/text! sig-field (str (:sig best-config)))
                              (update-chart chart-panel @selected-coin @start-date @end-date)
                              (s/alert (str "Optimization complete! Best config: CW=" (:cw best-config) 
                                           " SW=" (:sw best-config) " Sig=" (:sig best-config)
                                           " Score=" (format "%.2f" (:score best-config)) "%")))))
                        (catch Exception e
                          (s/alert (str "Optimization failed: " (.getMessage e))))))
        ;; Reset to default function  
        reset-fn (fn [_]
                   (reset! cw-param 5)
                   (reset! sw-param 5) 
                   (reset! pw-param 5)
                   (reset! sig-param 1)
                   (reset! nop-param 5)
                   (s/text! cw-field "5")
                   (s/text! sw-field "5")
                   (s/text! pw-field "5")
                   (s/text! sig-field "1")
                   (s/text! nop-field "5")
                   (update-chart chart-panel @selected-coin @start-date @end-date)
                   (s/alert "Configuration reset to default values!"))]
    ;; Set initial values
    (.setDate start-picker ^java.util.Date @start-date)
    (.setDate end-picker ^java.util.Date @end-date)
    (s/selection! coin-box @selected-coin)
    ;; Listeners
      (.addPropertyChangeListener start-picker "date"
                                  (reify java.beans.PropertyChangeListener
                                    (propertyChange [_ _] (update-fn nil))))
      (.addPropertyChangeListener end-picker "date"
                                  (reify java.beans.PropertyChangeListener
                                    (propertyChange [_ _] (update-fn nil))))
      (s/listen coin-box :selection update-fn)
    (s/listen future-toggle :selection (fn [e]
                                         (reset! show-future? (s/selection e))
                                         (update-fn nil)))
    (s/listen predict-btn :action predict-fn)
    (s/listen optimize-btn :action optimize-fn)
    (s/listen reset-btn :action reset-fn)
    ;; Single consolidated Control Panel with improved spacing
    (s/border-panel
     :border (javax.swing.BorderFactory/createTitledBorder "Control Panel")
     :center (s/vertical-panel
              :items [
                ;; Date Selection Row with better spacing
                (s/horizontal-panel
                 :items [(s/label :text "Start:")
                         (s/label :text " ")
                         start-picker
                         (s/label :text "    End:")
                         (s/label :text " ")
                         end-picker
                         (s/label :text "    Coin:")
                         (s/label :text " ")
                         coin-box
                         (s/label :text "      ")
                         future-toggle])
                
                (s/label :text " ")  ; Vertical spacing
                
                ;; Configuration Parameters Row with better spacing
                (s/horizontal-panel
                 :items [(s/label :text "CW:")
                         (s/label :text " ")
                         cw-field
                         (s/label :text "   SW:")
                         (s/label :text " ")
                         sw-field
                         (s/label :text "   PW:")
                         (s/label :text " ")
                         pw-field
                         (s/label :text "   Sig:")
                         (s/label :text " ")
                         sig-field
                         (s/label :text "   NOP:")
                         (s/label :text " ")
                         nop-field])
                
                (s/label :text " ")  ; Vertical spacing
                
                ;; Action Buttons Row with better spacing
                (s/horizontal-panel
                 :items [predict-btn
                         (s/label :text "     ")
                         optimize-btn
                         (s/label :text "     ")
                         reset-btn])]))))

  (defn show-window []
    (s/native!)
    (let [chart-panel (create-chart-panel)
          logo (javax.imageio.ImageIO/read (io/file "resources/img/logo-v2.png"))
          frame (doto (s/frame :title "Cryptodamus Price Chart"
                               :size [1500 :by 800]
                               :on-close :exit)
                  (.setIconImage logo))
          controls (create-controls chart-panel)]

      ;; Set taskbar/dock icon for the application
      (JFrame/setDefaultLookAndFeelDecorated true)
      (when-let [taskbar (try (java.awt.Taskbar/getTaskbar) (catch Exception _ nil))]
        (try
          (.setIconImage taskbar logo)
          (catch Exception _ nil)))

      (update-chart chart-panel @selected-coin @start-date @end-date)
      (-> frame
          (s/config! :content (s/border-panel
                               :north controls
                               :center chart-panel))
          s/pack!
          s/show!)))

  (defn -main [& args]
    (show-window))

  (comment
    (show-window))