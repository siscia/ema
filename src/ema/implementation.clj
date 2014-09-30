(ns ema.implementation
  (:require [clojure.core.typed :as t]
            ;;[ema.type :refer [EntryMap ResourceDefinition PreResourceDefinition]]
            ))

;; Dictionary:
;; + entry-map, the map that the user provides. An example is ema.possible/possible

;; + resources-def(inition), a map which contains all the information necessary to serve a resource.

;; + resource-pre-definition, in the code as `res-pre`, are an incomplete definition of a resource, such maps will be manipulated and integrated with the values in the entry-map to yield a resource-definition.


;; (t/ann generate-handler [t/Any -> t/Any]) 
(defmulti generate-handler
  "Custom implementation layer are suppose to provide an implementation."
  :handler)

;; (t/ann generate-asts [t/Any -> t/Any])
(defmulti generate-asts
  "Custom implementation layer are suppose to provide an implementation."
  :key)

;; (t/ann custom-resource-definition [ResourceDefinition EntryMap -> ResourceDefinition])
(defmulti custom-inject
  "This function is suppose to be used as entry point by the custom layers. A custom layer can redefine this function as its own will adding and modify whatever key it need."
  (fn [pre-res entry-map]
    (:key pre-res)))

(defmethod custom-inject :default [res-def entry-map]
  res-def)

(defmulti custom-auth-inject
  "Custom auth implementation layers are suppose to provide an implemetation. "
  :key)

(defmethod custom-auth-inject :default [res-def auth-map]
  res-def)

(defmulti authentication
  "This function is called whenever is necessary to determinate if a request is authorized or not.
Custom authentication layer are suppose to provide an implementation."
  (fn
    ([m] (:key m))
    ([m _ctx_] (:key m))
    ([m _ctx_ _k_] (:key m))))

(defmethod authentication :default
  ([m]
     (partial authentication m))
  ([m _ctx_]
     false)
  ([m _ctx_ _k_]
     false))

(defmulti auth-inject
  "The function will smartly add the authentication definition to a resource." 
  (fn [coll entry-map]
    (let [auth-map (:auth entry-map)]
      (loop [level 0
             m auth-map]
        (if (map? m)
          (recur (inc level) (second (first m)))
          level)))))

(defmethod auth-inject 2
  [res-pre entry-map]
  (let [auth-map (:auth entry-map)
        sp-auth-map (get auth-map (:auth res-pre))
        final (assoc res-pre :auth
                     sp-auth-map)]
    final))

(defmethod auth-inject 1
  [res-pre entry-map]
  (if (:auth res-pre)
    (assoc res-pre :auth
           (:auth entry-map))
    res-pre))

(defmethod auth-inject 0
  [res-pre entry-map]
  (assoc res-pre :auth (:auth entry-map)))

;;(t/ann inject-basic-key [PreResourceDefinition EntryMap -> ResourceDefinition])
(defn inject-basic-keys
  "Inject in the pre-res map the foundamental keys, from the entry-map, that are not already present."
  [res-pre entry-map]
  (merge (select-keys entry-map [:key :handler :uri]) res-pre))

;;(t/ann define-resource [EntryMap -> (t/Fn [PreResourceDefinition -> ResourceDefinition])])
(defn define-resource
  "The function is responsible for transform a single pre-res in a resource definition."
  [entry-map]
  (fn [res-pre]
    (-> res-pre
        (inject-basic-keys entry-map)
        (auth-inject entry-map)
        (custom-inject entry-map)
        (custom-auth-inject entry-map))))

;;(t/ann generate-resource-definition [EntryMap -> (t/Seq ResourceDefinition)])
(defn generate-resource-definition
  "Given an entry-map the function will return a sequence of resource-definition."
  [{:keys [resources] :as entry-map}]
  (let [resources-def (map (define-resource entry-map) resources)]
    resources-def))
