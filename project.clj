(defproject ema "0.0.9"
  :description "A simple interface to write REST"
  :url "https://github.com/siscia/ema"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [liberator "0.12.2"]
                 [http-kit "2.1.19"]
                 ;; [org.clojure/core.typed "0.2.68" :exclusions [org.clojure/core.unify]]
                 [cheshire "5.3.1"]
                 [ring "1.3.1" :exclusions [org.clojure/java.classpath
                                            hiccup
                                            joda-time]]
                 [com.novemberain/monger "2.0.0"]
                 [bidi "1.10.5" :exclusions [com.google.guava/guava
                                             org.clojure/data.json
                                             joda-time
                                             org.clojure/tools.reader]]
                 [ring-basic-authentication "1.0.5"]
                 [org.mindrot/jbcrypt "0.3m"]]
  :profiles {:dev {:dependencies [[midje "1.6.3"]]}}
  :main ema.core
  :aot [ema.core])
