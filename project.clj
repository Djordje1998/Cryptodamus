(defproject cryptodamus "0.1.0-SNAPSHOT"
  :description "Predict price of crypto base on historical data"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [midje "1.10.10"]
                 [clj-http "3.13.0"]
                 [cheshire "5.13.0"]
                 [seesaw "1.5.0"]
                 [org.jfree/jfreechart "1.5.3"]
                 [com.toedter/jcalendar "1.4"]
                 [criterium "0.4.6"]]
  :plugins [[lein-midje "3.2.1"]]
  :main cryptodamus.gui
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
