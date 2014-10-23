(ns ema.implementation
  ;; (:require [clojure.core.typed :as t]
  ;;           [ema.type :refer [EntryMap ResourceDefinition PreResourceDefinition]])
  )

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

(defmulti connection
  "Custom implementation layer are suppose to provide an implementation."
  (fn [res-def] (:key res-def)))

(defmulti basic-get
  "It is used to make connections between the various resource.
  Custom implementation layer are suppose to provide an implementation.
  + key has the same meaning of :key in any other multimethods.
  + id is the id of the resource to connect.
  + conn is a map that comes out the connection multimethod."
  (fn [key id conn] key))

;; (t/ann custom-resource-definition [ResourceDefinition EntryMap -> ResourceDefinition])
(defmulti custom-inject
  "This function is suppose to be used as entry point by the custom layers. A custom layer can redefine this function as its own will adding and modify whatever key it need."
  (fn [res-def entry-map]
    (:key res-def)))

(defmethod custom-inject :default [res-def entry-map]
  res-def)

(defmulti custom-auth-inject
  "
  Custom auth implementation layers are suppose to provide an implemetation.
  Note that the auth map is manipulate after that is been inject in the res-definition
  "
  (fn [res-def entry-map]
    (if-let [auth-map (:auth res-def)]
      (:key auth-map)
      :no-auth)))

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
(defn keys-inject
  "Inject in the pre-res map all the keys, from the entry-map, that are not already present."
  [res-pre entry-map]
  (merge entry-map res-pre))

(defn connection-inject
  [res-pre]
  (assoc-in res-pre
            [:meta :connection]
            (connection res-pre)))

(defn basic-get-inject
  [res-pre]
  {:pre [#(get-in res-pre [:meta :connection])]}
  (assoc-in res-pre
            [:meta :basic-get]
            (partial basic-get
                     (:key res-pre)
                     (get-in res-pre [:meta :connection]))))

;;(t/ann define-resource [EntryMap -> (t/Fn [PreResourceDefinition -> ResourceDefinition])])
(defn define-resource
  "
  The function is responsible for transform a single pre-res in a resource definition.
  The auth map is modified when is already inject in the res-definition.
  "
  [entry-map]
  (fn [res-pre]
    (-> res-pre
        (keys-inject entry-map)
        (auth-inject entry-map)
        (custom-inject entry-map)
        (custom-auth-inject entry-map)
        connection-inject
        basic-get-inject)))

(defn find-resorces-def [name resources-def]
  (first (filter #(= (:name %) name) resources-def)))

(defn links-creator [conns res-defs]
  (let [create-link
        (fn [conn res]
          (if-let [id (get res (:field conn))]
            (let [res-conn (-> conns
                               :resource
                               (find-resorces-def res-defs))
                  basic-get (get-in res-conn [:meta :basic-get])]
              (assoc res (:field conn) (basic-get id)))))]
    
    (map (fn [conn] (partial create-link conn)) conns)))

(defn add-connections [res-defs {:keys [connections] :as entry-map}]
  (map (fn [res-def]
         (let [conns (get connections (:name res-def))
               links-creator (links-creator conns)]
           (assoc-in res-def
                     [:meta :links-creator]
                     links-creator))) res-defs))

;;(t/ann generate-resource-definition [EntryMap -> (t/Seq ResourceDefinition)])
(defn generate-resource-definition
  "Given an entry-map the function will return a sequence of resource-definition."
  [{:keys [resources] :as entry-map}]
  (let [resources-def (map (define-resource entry-map) resources)]
    resources-def))
