(ns ema.core
  (:use org.httpkit.server)
  (:require [clojure.edn :as edn]
            [liberator.dev :refer [wrap-trace]]
            [ema.implementation :refer [generate-handler generate-asts]]
            [ema.possible :refer [possible]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.basic-authentication :refer [wrap-basic-authentication]]
            [ema.mongo]
            [ema.authentication]
            [ema.routes-gen])
  (:gen-class))

(defn ema [m]
  (-> m
      generate-asts
      generate-handler
      wrap-multipart-params
      (wrap-basic-authentication (fn [u p]
                                   {:username u
                                    :password p}))
      (wrap-trace :header :ui)))

(def app (ema possible))

(defn -main
  ([edn-file]
     (let [resource-definition (-> edn-file slurp edn/read-string)
           app (ema resource-definition)]
       (run-server app {:port 8000}))))
