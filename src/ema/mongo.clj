(ns ema.mongo
  (:require [monger.core :refer [connect-via-uri]]
            [liberator.core :refer [resource]]
            [ema.implementation :refer [generate-asts generate-resource-definition custom-inject]]
            [ema.mongo.resource :refer [resource-collection-entries resource-item-entries]]))

(defn collection-entries [definition-map]
  (let [{:keys [conn db]} (connect-via-uri (:uri definition-map))
        coll (:name definition-map)]
    (resource-collection-entries definition-map db coll)))

(defn item-entries [m id]
  (let [{:keys [conn db]} (connect-via-uri (:uri m))
        coll (:name m)]
    (resource-item-entries m id db coll)))

(defn item-entries-value
  [definition-map]
  {:method :any
   :route (:name definition-map)
   :params :id
   :resource (fn [{:keys [route-params] :as req}]
               ((resource (item-entries definition-map (:id route-params)))
                req))})

(defn collection-entries-value
  [definition-map]
  {:method :any
   :route (:name definition-map)
   :resource (resource (collection-entries definition-map))})

(defn reducing-f [definition-map]
  (fn [v {:keys [predicate value]}]
    (if (predicate definition-map)
      (conj v (merge definition-map (value definition-map)))
      v)))

(defn definition-map-2-ast [definition-map]
  (reduce (reducing-f definition-map)
          [] [{:predicate (fn [dm] (contains? dm :item-mth))
               :value item-entries-value}
              {:predicate (fn [dm] (contains? dm :collection-mth))
               :value collection-entries-value}]))

(defn generate-mongo-asts [m]
  (let [definition-maps (generate-resource-definition m)
        asts (map definition-map-2-ast definition-maps)]
    (flatten asts)))

(defmethod custom-inject :mongo [coll entry-map]
  (let [with-database (merge
                       (select-keys entry-map [:database])
                       coll)]
    (assoc coll :uri (str (:uri coll) "/" (:database with-database)))))

(defmethod generate-asts :mongo [m]
  {:handler :bidi
   :asts (generate-mongo-asts m)})
