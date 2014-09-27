(ns ema.core
  (:use org.httpkit.server)
  (:require [compojure.core :refer [defroutes ANY]]
            [liberator.core :refer [resource]]
            [liberator.dev :refer [wrap-trace]]
            [ema.implementation :refer [generate-handler generate-asts]]
            [ema.mongo]
            [ema.possible :refer [possible]]
            [ema.routes-gen]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.basic-authentication :refer [wrap-basic-authentication]]))

(def example
  (generate-handler
   (generate-asts possible)))

(def app
  (-> example
      (wrap-trace :header :ui)
      wrap-multipart-params
      ))

(defn ema [m]
  (-> m
      generate-asts
      generate-handler
      wrap-multipart-params
      (wrap-basic-authentication identity (fn [u p] {:username u
                                                     :password p}))
      ))
