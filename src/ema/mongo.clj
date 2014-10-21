(ns ema.mongo
  (:require [monger.core :refer [connect-via-uri]]
            [liberator.core :refer [resource]]
            [ema.implementation :refer [generate-asts generate-resource-definition custom-inject connection]]
            [ema.mongo.resource :refer [resource-collection-entries resource-item-entries]]))

;; Dictionary
;; + res-def (resource-definition) : map, contains all the information to define a single resource
;; + entry-map : map, the map passed as input to ema itself

(defmethod connection :mongo [res-def]
  (let [{:keys [conn db] :as con-map}
        (connect-via-uri (:uri res-def))]
    (assoc con-map
      :coll (:name res-def))))

(defn collection-entries [res-def]
  (let [{:keys [conn db]} (connect-via-uri (:uri res-def))
        coll (:name res-def)]
    (resource-collection-entries res-def db coll)))

(defn item-entries [res-def id]
  (let [{:keys [conn db]} (connect-via-uri (:uri res-def))
        coll (:name res-def)]
    (resource-item-entries res-def id db coll)))

(defn item-entries-value
  [res-def]
  {:method :any
   :route (:name res-def)
   :params :id
   :resource (fn [{:keys [route-params] :as req}]
               ((resource (item-entries res-def (:id route-params)))
                req))})

(defn collection-entries-value
  [res-def]
  {:method :any
   :route (:name res-def)
   :resource (resource (collection-entries res-def))})

(defn reducing-f [res-def]
  (fn [v {:keys [predicate value]}]
    (if (predicate res-def)
      (conj v (merge res-def (value res-def)))
      v)))

(defn res-def-2-ast [res-def]
  (reduce (reducing-f res-def)
          [] [{:predicate (fn [dm] (contains? dm :item-mth))
               :value item-entries-value}
              {:predicate (fn [dm] (contains? dm :collection-mth))
               :value collection-entries-value}]))

(defn generate-mongo-asts [entry-map]
  (let [definition-maps (generate-resource-definition entry-map)
        asts (map res-def-2-ast definition-maps)]
    (flatten asts)))

(defmethod custom-inject :mongo [coll entry-map]
  (let [with-database (merge
                       (select-keys entry-map [:database])
                       coll)]
    (assoc coll :uri (str (:uri coll) "/" (:database with-database)))))

(defmethod generate-asts :mongo [entry-map]
  {:handler :bidi
   :asts (generate-mongo-asts entry-map)})
