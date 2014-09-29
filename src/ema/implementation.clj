(ns ema.implementation
  (:require [clojure.core.typed :as t]
            [ema.type :refer [EntryMap ResourceDefinition PreResourceDefinition]]))

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
(defmulti custom-resource-definition
  "This function is suppose to be used as entry point by the custom layers. A custom layer can redefine this function as its own will adding and modify whatever key it need."
  (fn [pre-res entry-map]
    (:key pre-res)))

(defmethod custom-resource-definition :default [res-def entry-map]
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

(defmulti authentication-resource-definition
  "The function will smartly add the authentication definition to a resource." 
  (fn [coll auth]
    (-> auth
        first
        second
        class)))

(defmethod authentication-resource-definition clojure.lang.IPersistentMap
  [coll auth-map]
  (let [sp-auth-map (get auth-map (:authentication coll))
        sp-auth-map (merge (select-keys coll [:uri :database])
                           sp-auth-map)
        sp-auth-map (assoc sp-auth-map
                      :uri (str (:uri sp-auth-map) "/" (:name sp-auth-map)))
        final (assoc coll :authentication
                     sp-auth-map)]
    final))

;;(t/ann inject-basic-key [PreResourceDefinition EntryMap -> ResourceDefinition])
(defn inject-basic-key
  "Inject in the pre-res map the foundamental keys, from the entry-map, that are not already present."
  [coll entry-map]
  (merge (select-keys entry-map [:key :handler :uri]) coll))

;;(t/ann define-resource [EntryMap -> (t/Fn [PreResourceDefinition -> ResourceDefinition])])
(defn define-resource
  "The function is responsible for transform a single pre-res in a resource definition."
  [entry-map]
  (fn [res-pre]
    (-> res-pre
        (inject-basic-key entry-map)
        (authentication-resource-definition (:authentication entry-map))
        (custom-resource-definition entry-map))))

;;(t/ann generate-resource-definition [EntryMap -> (t/Seq ResourceDefinition)])
(defn generate-resource-definition
  "Given an entry-map the function will return a sequence of resource-definition."
  [{:keys [resources] :as entry-map}]
  (let [resources-def (map (define-resource entry-map) resources)]
    resources-def))
