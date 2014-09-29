(ns ema.core
  (:use org.httpkit.server)
  (:require [compojure.core :refer [defroutes ANY]]
            [liberator.core :refer [resource]]
            [liberator.dev :refer [wrap-trace]]
            [ema.implementation :refer [generate-handler generate-asts]]
            [ema.possible :refer [possible]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.basic-authentication :refer [wrap-basic-authentication]]
            [ema.mongo]
            [ema.authentication]
            [ema.routes-gen]))

;; (def example
;;   (generate-handler
;;    (generate-asts possible)))

(def app
  (-> possible
      generate-asts
      generate-handler
      (wrap-trace :header :ui)
      wrap-multipart-params))

(defn ema [m]
  (-> m
      generate-asts
      generate-handler
      wrap-multipart-params
      (wrap-basic-authentication (fn [u p]
                                   {:username u
                                    :password p}))
      ))

(def app (ema possible))
