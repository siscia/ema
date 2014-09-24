(ns ema.core
  (:use org.httpkit.server)
  (:require [compojure.core :refer [defroutes ANY]]
            [liberator.core :refer [resource]]
            [liberator.dev :refer [wrap-trace]]
            [ema.implementation :refer [generate-handler generate-asts]]
            [ema.mongo]
            [ema.possible :refer [possible]]
            [ema.routes-gen]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]))

(def example
  (generate-handler
   (generate-asts possible)))

(def app
  (-> example
      (wrap-trace :header :ui)
      wrap-multipart-params
      ))
