(ns cryptodamus.gui
  (:require [seesaw.core :as s]
            [cryptodamus.fetch :as api]
            [cryptodamus.utils :as utils]
            [cryptodamus.core :as core]
            [clojure.java.io :as io])
  (:import [org.jfree.chart ChartPanel ChartFactory]
           [org.jfree.data.xy XYSeries XYSeriesCollection]
           [java.awt Color Dimension]
           [javax.swing JFrame JSlider JLabel SwingConstants JDialog JProgressBar]
           [java.util Date]
           [com.toedter.calendar JDateChooser]
           [org.jfree.chart.renderer.xy XYLineAndShapeRenderer]
           [javax.swing.event ChangeListener]))

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
(def comparator-param (atom "delta-avg")) ; comparator function name

;; Progress dialog for optimization
(defn create-progress-dialog [parent title message]
  (let [dialog (JDialog. parent title true) ; modal dialog
        progress-bar (doto (JProgressBar.)
                       (.setIndeterminate true)
                       (.setStringPainted true)
                       (.setString "Optimizing..."))
        panel (s/vertical-panel
               :items [(s/label :text message)
                       (s/label :text " ")
                       progress-bar
                       (s/label :text " ")
                       (s/label :text "Please wait, this may take several minutes...")])
        content-panel (s/border-panel
                       :center panel
                       :border 20)]

    (doto dialog
      (.setContentPane content-panel)
      (.setSize 400 150)
      (.setLocationRelativeTo parent)
      (.setDefaultCloseOperation JDialog/DO_NOTHING_ON_CLOSE)) ; prevent closing

    {:dialog dialog
     :show #(.setVisible dialog true)
     :hide #(.setVisible dialog false)
     :dispose #(.dispose dialog)}))

;; Single value slider component
(defn create-single-slider [label min-val max-val current-val]
  (let [slider (doto (JSlider. min-val max-val current-val)
                 (.setOrientation SwingConstants/HORIZONTAL)
                 (.setPreferredSize (Dimension. 200 50))
                 (.setPaintTicks true)
                 (.setPaintLabels true)
                 (.setMajorTickSpacing (max 1 (/ (- max-val min-val) 5))))
        value-label (doto (JLabel. (str current-val))
                      (.setPreferredSize (Dimension. 40 20))
                      (.setHorizontalAlignment SwingConstants/CENTER))

        update-label (fn []
                       (.setText value-label (str (.getValue slider))))

        listener (reify ChangeListener
                   (stateChanged [_ _] (update-label)))]

    (.addChangeListener slider listener)

    {:panel (s/border-panel
             :border (javax.swing.BorderFactory/createTitledBorder label)
             :center (s/vertical-panel
                      :items [(s/label :text " ")
                              (s/horizontal-panel
                               :items [(s/label :text "Value: ")
                                       (s/config! value-label :preferred-size [50 :by 25])])
                              (s/label :text " ")
                              slider
                              (s/label :text " ")]))
     :slider slider
     :get-value #(.getValue slider)}))

;; Range slider component for dual-handle min/max selection
(defn create-range-slider [label min-val max-val current-min current-max]
  (let [min-slider (doto (JSlider. min-val max-val current-min)
                     (.setOrientation SwingConstants/HORIZONTAL)
                     (.setPreferredSize (Dimension. 200 50))
                     (.setPaintTicks true)
                     (.setPaintLabels true)
                     (.setMajorTickSpacing (max 1 (/ (- max-val min-val) 5))))
        max-slider (doto (JSlider. min-val max-val current-max)
                     (.setOrientation SwingConstants/HORIZONTAL)
                     (.setPreferredSize (Dimension. 200 50))
                     (.setPaintTicks true)
                     (.setPaintLabels true)
                     (.setMajorTickSpacing (max 1 (/ (- max-val min-val) 5))))
        min-label (doto (JLabel. (str current-min))
                    (.setPreferredSize (Dimension. 40 20))
                    (.setHorizontalAlignment SwingConstants/CENTER))
        max-label (doto (JLabel. (str current-max))
                    (.setPreferredSize (Dimension. 40 20))
                    (.setHorizontalAlignment SwingConstants/CENTER))

        ;; Ensure min <= max constraint
        update-labels (fn []
                        (let [min-val (.getValue min-slider)
                              max-val (.getValue max-slider)]
                          (when (> min-val max-val)
                            (.setValue max-slider min-val))
                          (when (< max-val min-val)
                            (.setValue min-slider max-val))
                          (.setText min-label (str (.getValue min-slider)))
                          (.setText max-label (str (.getValue max-slider)))))

        min-listener (reify ChangeListener
                       (stateChanged [_ _] (update-labels)))
        max-listener (reify ChangeListener
                       (stateChanged [_ _] (update-labels)))]

    (.addChangeListener min-slider min-listener)
    (.addChangeListener max-slider max-listener)

    {:panel (s/border-panel
             :border (javax.swing.BorderFactory/createTitledBorder label)
             :center (s/vertical-panel
                      :items [(s/label :text " ")
                              (s/horizontal-panel
                               :items [(s/label :text "Min: ")
                                       (s/config! min-label :preferred-size [50 :by 25])
                                       (s/label :text "    Max: ")
                                       (s/config! max-label :preferred-size [50 :by 25])])
                              (s/label :text " ")
                              (s/label :text "Min:")
                              min-slider
                              (s/label :text "Max:")
                              max-slider
                              (s/label :text " ")]))
     :min-slider min-slider
     :max-slider max-slider
     :get-min-value #(.getValue min-slider)
     :get-max-value #(.getValue max-slider)}))

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
                            (let [comparator-fn (case @comparator-param
                                                  "delta-avg" core/delta-avg
                                                  "percentage-change" core/percentage-change
                                                  "log-returns" core/log-returns
                                                  "price-differences" core/price-differences
                                                  "relative-percent-change" core/relative-percent-change
                                                  "zero-anchoring" core/zero-anchoring
                                                  core/delta-avg)]
                              (core/predict-price prices nop cw sw pw sig comparator-fn)))
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
        (s/alert (str "Error updating chart: " (.getMessage ex)))))))

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
        ;; Comparator function selection dropdown
        comparator-box (s/combobox :model ["delta-avg" "percentage-change" "log-returns"
                                           "price-differences" "relative-percent-change" "zero-anchoring"]
                                   :selected-item @comparator-param)
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
                      (let [comparator (s/selection comparator-box)]
                        (when comparator (reset! comparator-param comparator)))
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
        ;; Optimize function with popup dialog using range sliders
        optimize-fn (fn [_]
                      (let [;; Create range sliders for parameters
                            cw-slider (create-range-slider "Chunk Window Range" 3 30 5 10)
                            sw-slider (create-range-slider "Skip Window Range" 3 30 5 10)
                            pw-slider (create-single-slider "Predict Window" 5 500 50)
                            sig-slider (create-range-slider "Significance Range (x10)" 1 30 5 15) ; scaled by 10: 0.1-3.0
                            nop-field-opt (s/text :text "3" :columns 3)

                            ;; Comparator function selection checkboxes
                            comparator-checkboxes {"delta-avg" (s/checkbox :text "Delta Average" :selected? true)
                                                   "percentage-change" (s/checkbox :text "Percentage Change" :selected? true)
                                                   "log-returns" (s/checkbox :text "Log Returns" :selected? false)
                                                   "price-differences" (s/checkbox :text "Price Differences" :selected? false)
                                                   "relative-percent-change" (s/checkbox :text "Relative Percent Change" :selected? false)
                                                   "zero-anchoring" (s/checkbox :text "Zero Anchoring" :selected? false)
                                                   "moving-average" (s/checkbox :text "Moving Average" :selected? false)}

                            comparator-panel (s/border-panel
                                              :border (javax.swing.BorderFactory/createTitledBorder "Comparator Functions")
                                              :center (s/vertical-panel
                                                       :items [(s/label :text " ")
                                                               (s/horizontal-panel
                                                                :items [(get comparator-checkboxes "delta-avg")
                                                                        (s/label :text "  ")
                                                                        (get comparator-checkboxes "percentage-change")])
                                                               (s/horizontal-panel
                                                                :items [(get comparator-checkboxes "log-returns")
                                                                        (s/label :text "  ")
                                                                        (get comparator-checkboxes "price-differences")])
                                                               (s/horizontal-panel
                                                                :items [(get comparator-checkboxes "relative-percent-change")
                                                                        (s/label :text "  ")
                                                                        (get comparator-checkboxes "zero-anchoring")])
                                                               (s/label :text " ")]))

                            ;; Configuration count calculator
                            config-count-label (s/label :text "Configurations to test: 0")

                            ;; Function to calculate total configurations
                            update-config-count (fn []
                                                  (let [cw-count (inc (- ((:get-max-value cw-slider)) ((:get-min-value cw-slider))))
                                                        sw-count (inc (- ((:get-max-value sw-slider)) ((:get-min-value sw-slider))))
                                                        pw-count 1 ; single value, not a range
                                                        sig-count (inc (- ((:get-max-value sig-slider)) ((:get-min-value sig-slider))))
                                                        comparator-count (count (filter #(s/selection %) (vals comparator-checkboxes)))
                                                        total-configs (* cw-count sw-count pw-count sig-count comparator-count)
                                                        estimated-time (/ total-configs 50)] ; rough estimate: 50 configs per second
                                                    (s/text! config-count-label
                                                             (str "Configurations to test: " total-configs
                                                                  " (Est. time: " (int estimated-time) "s)"))))

                            ;; Add listeners to update count when sliders change
                            _ (do
                                (.addChangeListener (:min-slider cw-slider)
                                                    (reify ChangeListener (stateChanged [_ _] (update-config-count))))
                                (.addChangeListener (:max-slider cw-slider)
                                                    (reify ChangeListener (stateChanged [_ _] (update-config-count))))
                                (.addChangeListener (:min-slider sw-slider)
                                                    (reify ChangeListener (stateChanged [_ _] (update-config-count))))
                                (.addChangeListener (:max-slider sw-slider)
                                                    (reify ChangeListener (stateChanged [_ _] (update-config-count))))
                                (.addChangeListener (:slider pw-slider)
                                                    (reify ChangeListener (stateChanged [_ _] (update-config-count))))
                                (.addChangeListener (:min-slider sig-slider)
                                                    (reify ChangeListener (stateChanged [_ _] (update-config-count))))
                                (.addChangeListener (:max-slider sig-slider)
                                                    (reify ChangeListener (stateChanged [_ _] (update-config-count))))
                                ;; Add listeners for checkboxes
                                (doseq [checkbox (vals comparator-checkboxes)]
                                  (s/listen checkbox :action (fn [_] (update-config-count))))
                                ;; Initial calculation
                                (update-config-count))

                            ;; Create dialog content with 2-column layout
                            dialog-content (s/vertical-panel
                                            :items [(s/label :text "Optimization Parameter Configuration")
                                                    (s/label :text "  ")
                                                    ;; First row - CW and SW
                                                    (s/horizontal-panel
                                                     :items [(:panel cw-slider)
                                                             (s/label :text "    ")
                                                             (:panel sw-slider)])
                                                    (s/label :text " ")
                                                    ;; Second row - PW and Sig
                                                    (s/horizontal-panel
                                                     :items [(:panel pw-slider)
                                                             (s/label :text "    ")
                                                             (:panel sig-slider)])
                                                    (s/label :text " ")
                                                    ;; Third row - Comparator selection
                                                    comparator-panel
                                                    (s/label :text " ")
                                                    ;; Fourth row - NOP and Config count
                                                    (s/horizontal-panel
                                                     :items [(s/vertical-panel
                                                              :items [(s/label :text "Number of Predictions:")
                                                                      (s/label :text " ")
                                                                      nop-field-opt])
                                                             (s/label :text "    ")
                                                             (s/vertical-panel
                                                              :items [(s/label :text "Optimization Info:")
                                                                      (s/label :text " ")
                                                                      config-count-label])])
                                                    (s/label :text "  ")
                                                    (s/label :text "Note: Larger ranges will take longer to optimize")])

                            ;; Show dialog and get result
                            result (javax.swing.JOptionPane/showConfirmDialog
                                    nil
                                    (.getContentPane (doto (javax.swing.JFrame.)
                                                       (.add dialog-content)))
                                    "Optimization Configuration"
                                    javax.swing.JOptionPane/OK_CANCEL_OPTION
                                    javax.swing.JOptionPane/PLAIN_MESSAGE)]

                        (when (= result javax.swing.JOptionPane/OK_OPTION)
                          (try
                            (let [cw-min ((:get-min-value cw-slider))
                                  cw-max ((:get-max-value cw-slider))
                                  sw-min ((:get-min-value sw-slider))
                                  sw-max ((:get-max-value sw-slider))
                                  pw-val ((:get-value pw-slider))
                                  sig-min (/ ((:get-min-value sig-slider)) 10.0) ; convert back from scaled values
                                  sig-max (/ ((:get-max-value sig-slider)) 10.0)
                                  nop-val (Integer/parseInt (s/text nop-field-opt))
                                  selected-comparators (filter (fn [[_ v]] (s/selection v)) comparator-checkboxes)
                                  parent-frame (javax.swing.SwingUtilities/getWindowAncestor (.getParent dialog-content))
                                  progress-dialog (create-progress-dialog parent-frame
                                                                          "Optimization Running"
                                                                          "Testing parameter configurations...")]
                                (if (and (< cw-min cw-max) (< sw-min sw-max) (< sig-min sig-max)
                                         (pos? cw-min) (pos? sw-min) (pos? pw-val) (pos? sig-min) (pos? nop-val)
                                         (seq selected-comparators))
                                  (do
                                    ;; Run optimization in background thread FIRST to avoid blocking by modal dialog
                                    (future
                                      (try
                                        (println "Starting optimization future...")
                                        ;; Show the progress dialog on the EDT (modal) after future has started
                                        (javax.swing.SwingUtilities/invokeLater
                                         (fn []
                                           (println "Showing progress dialog...")
                                           ((:show progress-dialog))))
                                        (let [prices-arr (api/get-price @selected-coin
                                                                        (utils/->unix-timestamp @start-date)
                                                                        (utils/->unix-timestamp @end-date))
                                              prices (vec (seq prices-arr))
                                              _ (println "Fetched" (count prices) "price points")
                                              cw-range (range cw-min (inc cw-max))
                                              sw-range (range sw-min (inc sw-max))
                                              pw-range [pw-val] ; single value, not a range
                                              sig-range (map #(/ % 10.0) (range (* 10 sig-min) (inc (* 10 sig-max))))
                                              selected-comp-fns (->> selected-comparators
                                                                     (map (fn [[k _]] [k (get core/comparator-fns k)]))
                                                                     (filter (fn [[_ f]] (some? f))))
                                              _ (println "Selected comparators:" (map first selected-comparators))
                                              _ (println "Selected comp fns (filtered):" (map first selected-comp-fns))
                                              optimal-configs (core/optimize-config prices
                                                                                    {:cw-range cw-range
                                                                                     :sw-range sw-range
                                                                                     :pw-range pw-range
                                                                                     :sig-range sig-range
                                                                                     :comparator-fns selected-comp-fns}
                                                                                    nop-val)]
                                          ;; Update UI on EDT thread
                                          (javax.swing.SwingUtilities/invokeLater
                                           (fn []
                                             ((:hide progress-dialog))
                                             (if (seq optimal-configs)
                                               (let [best-config (first optimal-configs)]
                                                 (reset! cw-param (:cw best-config))
                                                 (reset! sw-param (:sw best-config))
                                                 (reset! pw-param (:pw best-config))
                                                 (reset! sig-param (:sig best-config))
                                                 (reset! comparator-param (:comparator-name best-config))
                                                 (s/text! cw-field (str (:cw best-config)))
                                                 (s/text! sw-field (str (:sw best-config)))
                                                 (s/text! pw-field (str (:pw best-config)))
                                                 (s/text! sig-field (str (:sig best-config)))
                                                 (s/selection! comparator-box (:comparator-name best-config))
                                                 (update-chart chart-panel @selected-coin @start-date @end-date)
                                                 (s/alert (str "Optimization complete! Best config: CW=" (:cw best-config)
                                                               " SW=" (:sw best-config) " PW=" (:pw best-config) " Sig=" (:sig best-config)
                                                               " Comparator=" (:comparator-name best-config)
                                                               " Score=" (format "%.2f" (:score best-config)) "%")))
                                               (s/alert "No valid optimal configuration found for the selected ranges.")))))
                                        (catch Exception e
                                          (javax.swing.SwingUtilities/invokeLater
                                           (fn []
                                             ((:hide progress-dialog))
                                             (s/alert (str "Optimization failed: " (.getMessage e)))))))))
                                  (s/alert "Please ensure all minimum values are less than maximum values and all values are positive.")))
                            (catch NumberFormatException _
                              (s/alert "Please enter valid numeric values for all ranges."))
                            (catch Exception e
                              (s/alert (str "Optimization failed: " (.getMessage e))))))))
        ;; Reset to default function  
        reset-fn (fn [_]
                   (reset! cw-param 5)
                   (reset! sw-param 5)
                   (reset! pw-param 5)
                   (reset! sig-param 1)
                   (reset! nop-param 5)
                   (reset! comparator-param "delta-avg")
                   (s/text! cw-field "5")
                   (s/text! sw-field "5")
                   (s/text! pw-field "5")
                   (s/text! sig-field "1")
                   (s/text! nop-field "5")
                   (s/selection! comparator-box "delta-avg")
                   (update-chart chart-panel @selected-coin @start-date @end-date)
                   (s/alert "Configuration reset to default values!"))
        ]
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
    (s/listen comparator-box :selection update-fn)
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
              :items [;; Date Selection Row with better spacing
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
                               nop-field
                               (s/label :text "   Comparator:")
                               (s/label :text " ")
                               comparator-box])

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

(defn -main [& _args]
  (show-window))

(comment
  (show-window))