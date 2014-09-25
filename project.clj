(defproject ema "0.0.1"
  :description "A simple interface to write REST"
  :url "https://github.com/siscia/ema"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [liberator "0.12.2"]
                 [http-kit "2.1.16"]
                 [org.clojure/core.typed "0.2.68"]
                 [cheshire "5.3.1"]
                 [ring "1.3.0"]
                 [ring/ring-defaults "0.1.1"]
                 [ring/ring-json "0.3.1"]
                 [com.novemberain/monger "2.0.0"]
                 [compojure "1.1.8"]
                 [bidi "1.10.4"]]
  :main ema.core
  :ring {:handler ema.core/app})
