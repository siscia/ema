(ns ema.routes-gen
  (:require [liberator.core :refer [resource]]
            [bidi.bidi :refer [make-handler]]))

(defn generate-single-route [ast]
  (if (:params ast)
    {[(str (:route ast) "/") (:params ast)] (:resource ast)}
    {(:route ast) (:resource ast)}))

(defn generate-bidi-routes [asts-list]
  ["/" (first (map (fn [asts]
                     (apply merge (map generate-single-route asts)))
                   asts-list))])

(defn generate-bidi-handler [asts-list]
  (make-handler (generate-bidi-routes asts-list)))
