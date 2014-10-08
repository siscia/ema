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

  (:use ring.adapter.jetty))

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

(defn -main [yaml-file]
  (let [resource-definition (-> yaml-file slurp edn/read-string)
        app1 (ema resource-definition)]
    (run-server app1 {:port 8000})))
