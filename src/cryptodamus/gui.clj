(ns cryptodamus.gui
  (:require [seesaw.core :as s])
  (:import [org.jfree.chart ChartPanel ChartFactory]
           [org.jfree.chart.plot XYPlot PlotOrientation]
           [org.jfree.data.xy XYSeries XYSeriesCollection]
           [java.awt Color Taskbar]
           [javax.swing JFrame]
           [javax.imageio ImageIO]
           [java.time LocalDate ZoneId]
           [java.util Date]
           [com.toedter.calendar JDateChooser]
           [java.time.temporal ChronoUnit]
           [org.jfree.chart.renderer.xy XYLineAndShapeRenderer]
           [org.jfree.chart.labels XYToolTipGenerator]))

;; Define atoms
(def selected-coin (atom "bitcoin"))
(def start-date (atom (LocalDate/now)))
(def end-date (atom (.plusDays (LocalDate/now) 10)))

(defn generate-dummy-data [start end]
  (let [days (.between ChronoUnit/DAYS start end)]
    {"bitcoin" (vec (for [i (range days)]
                      [i (+ 30000 (* 5000 (Math/sin (/ i 10.0))) (rand-int 2000))]))
     "ethereum" (vec (for [i (range days)]
                       [i (+ 2500 (* 500 (Math/sin (/ i 8.0))) (rand-int 200))]))
     "monero" (vec (for [i (range days)]
                     [i (+ 150 (* 30 (Math/cos (/ i 12.0))) (rand-int 20))]))}))

(defn create-chart-panel []
  (let [series (XYSeries. "Price")
        dataset (XYSeriesCollection. series)
        chart (ChartFactory/createXYLineChart
               "Crypto Price Chart"
               "Days"
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
        tooltip-gen (proxy [org.jfree.chart.labels.XYToolTipGenerator] []
                     (generateToolTip [dataset series item]
                       (let [x (.getXValue dataset series item)
                             y (.getYValue dataset series item)]
                         (format "%s: Day %.0f - $%.2f" 
                                 (.getSeriesKey dataset series)
                                 x
                                 y))))]
    
    ;; Make sure shapes are visible for tooltips and configure tooltips
    (doto ^org.jfree.chart.renderer.xy.XYLineAndShapeRenderer renderer
      (.setSeriesShapesVisible 0 true)
      (.setSeriesShapesFilled 0 true)
      (.setDrawSeriesLineAsPath true)
      (.setSeriesShape 0 (java.awt.Rectangle. -4 -4 8 8))
      (.setDefaultToolTipGenerator tooltip-gen))    ;; Set tooltip generator only on renderer
    
    (.setBackgroundPaint plot Color/WHITE)
    (doto plot
      (.setDomainGridlinePaint (Color. 220 220 220))
      (.setRangeGridlinePaint (Color. 220 220 220))
      (.setRenderer renderer))  ;; Set the modified renderer
    (ChartPanel. chart true)))

(defn update-chart [chart-panel coin start end]
  (let [chart (.getChart chart-panel)
        dataset (.getDataset (.getPlot chart))
        new-series (XYSeries. coin)
        data (get (generate-dummy-data start end) coin)]
    
    (doseq [[day price] data]
      (.add new-series day price))
    
    (.removeAllSeries ^XYSeriesCollection dataset)
    (.addSeries ^XYSeriesCollection dataset new-series)
    (.fireChartChanged chart)))

(defn create-controls [chart-panel]
  (let [start-picker (JDateChooser.)
        end-picker (JDateChooser.)
        update-fn (fn [_]
                    (let [start (.getDate start-picker)
                          end (.getDate end-picker)]
                      (when (and start end)
                        (reset! start-date (.toLocalDate
                                            (.atZone (.toInstant start)
                                                     (ZoneId/systemDefault))))
                        (reset! end-date (.toLocalDate
                                          (.atZone (.toInstant end)
                                                   (ZoneId/systemDefault))))
                        (update-chart chart-panel @selected-coin @start-date @end-date))))]
    
    ;; Set initial dates
    (.setDate start-picker (java.util.Date/from
                            (.toInstant
                             (.atZone (.atStartOfDay @start-date)
                                      (ZoneId/systemDefault)))))
    (.setDate end-picker (java.util.Date/from
                          (.toInstant
                           (.atZone (.atStartOfDay @end-date)
                                    (ZoneId/systemDefault)))))
    
    ;; Add listeners
    (.addPropertyChangeListener start-picker "date"
      (reify java.beans.PropertyChangeListener
        (propertyChange [this evt] (update-fn nil))))
    (.addPropertyChangeListener end-picker "date"
      (reify java.beans.PropertyChangeListener
        (propertyChange [this evt] (update-fn nil))))
    
    (s/horizontal-panel
     :items ["Start: " start-picker
             "End: " end-picker
             "Coin: " (s/combobox :model ["bitcoin" "ethereum" "monero"]
                                  :listen [:selection update-fn])])))

(defn show-window []
  (s/native!)
  (let [chart-panel (create-chart-panel)
        logo (javax.imageio.ImageIO/read (clojure.java.io/file "resources/img/logo-v2.png"))
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