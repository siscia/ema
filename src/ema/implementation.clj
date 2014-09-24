(ns ema.implementation
  (:require [clojure.core.typed :as t]
            [ema.type :refer [EntryMap ResourceDefinition PreResourceDefinition]]))

;; (t/ann generate-handler [t/Any -> t/Any]) 
(defmulti generate-handler :handler)

;; (t/ann generate-asts [t/Any -> t/Any])
(defmulti generate-asts :key)

;; (t/ann specific-resource-definition [ResourceDefinition EntryMap -> ResourceDefinition])
(defmulti custom-resource-definition (fn [pre-res entry-map]
                                         (:key pre-res)))
(defmethod custom-resource-definition :default [coll entry-map]
  coll)

;; (t/ann authentication-resource-definition
;;        (t/All [x y] [(t/Map x y) (t/Map x y) -> (t/Map x y)]))
(defn authentication-resource-definition [coll auth-map]
  (assoc coll
    :authentication
    (get auth-map (:authentication coll))))

;; (t/ann inject-basic-key
;;        (t/All [x y] [(t/Map x y) (t/Map x y) -> (t/Map x y)]))
(defn inject-basic-key [coll entry-map]
  (merge (select-keys entry-map [:key :handler :uri]) coll))

;;(t/ann define-resource [EntryMap -> (t/Fn [PreResourceDefinition -> ResourceDefinition])])
(defn define-resource [entry-map]
  (fn [coll]
    (-> coll
        (inject-basic-key entry-map)
        (authentication-resource-definition (:authentication entry-map))
        (custom-resource-definition entry-map))))

;;(t/ann generate-resource-definition [EntryMap -> (t/Seq ResourceDefinition)])
(defn generate-resource-definition [{:keys [collections authentication] :as entry-map}]
  (map (define-resource entry-map) collections))
