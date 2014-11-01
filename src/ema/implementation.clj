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
  (fn [key conn id] key))

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
  "Given a name (string) and a seq of resource-definition, find the resource definition with the given name."
  (first (filter #(= (:name %) name) resources-def)))

(defn res-defs-to-link-def [res-defs]
  "Return a map with key the name of a resource and with value a function to return the resource itself, given the id."
  (reduce (fn [m res-def]
            (assoc m
              (:name res-def)
              (get-in res-def [:meta :basic-get])))
          {} res-defs))

;; (defn create-full-link [resource links]
;;   (letfn [(complete-link [r link]
;;             (let [field (:field link)]
;;               (assoc r
;;                 field
;;                 {:resource (:resource link)
;;                  :id (field r)})))]
;;     (reduce (fn [r l]
;;               (complete-link r l)) resource links)))

(defn get-rsr [id fn]
  (println id fn)
  (if id
    (if (sequential? id)
      (map fn id)
      (fn id))
    nil))

(defn create-full-link [resource links link-defs]
  (letfn [(complete-link [r link]
            (let [field (:field link)
                  resource (:resource link)
                  id (field r)
                  link (get-rsr id (link-defs resource))]
              (assoc r
                field link)))]
    (reduce (fn [r l]
              (complete-link r l)) resource links)))

(defn linker [{:keys [links] :as entry-map} link-defs res-def]
  (let [f (fn [res-name rsr]
                (let [link-to-follow (get links res-name)
                      rsr (create-full-link rsr (get links res-name) link-defs)]
                  rsr))]
    (assoc-in res-def
              [:meta :f]
              (partial f (:name res-def)))))

;;(t/ann generate-resource-definition [EntryMap -> (t/Seq ResourceDefinition)])
(defn generate-resource-definition
  "Given an entry-map the function will return a sequence of resource-definition."
  [{:keys [resources links] :as entry-map}]
  (let [res-defs (map (define-resource entry-map) resources)
        link-def (res-defs-to-link-def res-defs)
        a (map #(linker entry-map link-def %) res-defs)]
    (doseq [l a]
      (println l "\n"))
    a))
