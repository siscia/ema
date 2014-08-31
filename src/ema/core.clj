(ns ema.core
  (:use org.httpkit.server)
  (:require [compojure.core :refer [defroutes ANY]]
            [liberator.core :refer [resource]]
            [liberator.dev :refer [wrap-trace]]
            [ema.mongo :as m]
            [ema.possible :refer [possible]]
            [ema.routes-gen :refer :all]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
   ))

(def example
  (generate-bidi-handler (m/generate-asts possible)))

(def app
  (-> example
      (wrap-trace :header :ui)
      wrap-multipart-params
      ))
