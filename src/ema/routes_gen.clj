(ns ema.routes-gen
  (:require [ema.implementation :refer [generate-handler]]
            [liberator.core :refer [resource]]
            [bidi.bidi :refer [make-handler]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]))

(defn generate-single-route [ast]
  (if (:params ast)
    {[(str (:route ast) "/") (:params ast)] (:resource ast)}
    {(:route ast) (:resource ast)}))

(defn generate-bidi-routes [asts-list]
  ["/" (apply merge (map generate-single-route asts-list))])

(defn generate-bidi-handler [asts-list]
  (-> asts-list
      generate-bidi-routes
      make-handler
      wrap-multipart-params))

(defmethod generate-handler :bidi [asts-list]
  (generate-bidi-handler (:asts asts-list)))
