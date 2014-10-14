(ns ema.mongo
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [cheshire.core :refer [parse-string generate-string]]
            [cheshire.generate :refer [add-encoder encode-str]]
            [clojure.stacktrace :refer [e]]
            [liberator.core :refer [resource]]
            [ema.implementation :refer [generate-asts generate-resource-definition custom-inject authentication]])
  (:import org.bson.types.ObjectId))

(add-encoder org.bson.types.ObjectId encode-str)

(defn parse-json-malformed [ctx k]
  (when (#{:put :post :patch} (get-in ctx [:request :request-method]))
    (if-let [body (get-in ctx [:request :body])]
      (try
        [false {k (parse-string (slurp body))}]
        (catch Exception e
          [true {::malformed-message "Malformed JSON"}]))
      [true {::malformed-message "No body"}])))

(defn not-valid-id? [id]
    (if (org.bson.types.ObjectId/isValid id)
      [false {}]
      [true {::malformed-message (str "ID: \"" id "\" not valid. Please check: http://api.mongodb.org/java/2.0/org/bson/types/ObjectId.html")}]))

(defn map-collection-entries [m conn db coll]
  {:allowed-methods (concat (:collection-mth m) (:public-collection-mth m))
   :available-media-types ["text/plain" "application/json"]
   :allowed? (fn [ctx]
               (if (some #{(-> ctx :request :request-method)}
                         (:public-collection-mth m))
                 true
                 (authentication (:auth m) ctx)))
   :malformed? #(parse-json-malformed % ::data)
   :post! (fn [ctx] {::new (mc/insert-and-return db coll (::data ctx))})
   :post-redirect? false
   :new? #(boolean (::new %))
   :handle-created (fn [ctx]
                     (generate-string (::new ctx)))
   :handle-ok (fn [ctx]
                (let [query (get-in ctx [:request :query-params])
                      _ (println query)]
                  (generate-string {:data (mc/find-maps db coll query)})))})

(defn collection-entries [definition-map]
  (let [{:keys [conn db]} (mg/connect-via-uri (:uri definition-map))
        coll (:name definition-map)]
    (map-collection-entries definition-map conn db coll)))

(defn map-item-entries [m id conn db coll]
  {:allowed-methods (concat (:item-mth m) (:public-item-mth m))
   :available-media-types ["text/plain" "application/json"]
   :malformed? (fn [ctx]
                 (let [id-check (not-valid-id? id)]
                   (if-not (first id-check)
                     (parse-json-malformed ctx ::data)
                     id-check)))
   :allowed? (fn [ctx]
               (if (some #{(-> ctx :request :request-method)}
                         (:public-item-mth m))
                 true
                 (authentication (:auth m) ctx)))
   :handle-malformed #(generate-string (::malformed-message %))
   :exists? (fn [ctx]
              (let [resource (mc/find-map-by-id db coll (ObjectId. id))]
                (if-not (empty? resource)
                  [true {::resource resource}]
                  [false {::not-found-message (str "The resource with id \"" id "\" doesn't exist.")}])))
   :handle-not-found #(generate-string (::not-found-message %))
   :multiple-reppresentation? false
   :delete! #(mc/remove-by-id db coll (ObjectId. id))
   :delete-enacted? true
   :patch! (fn [ctx]
             {::patched (mc/save-and-return db coll (merge-with merge
                                                                (::resource ctx)
                                                                (::data ctx)))})
   :handle-exception (fn [ctx]
                       (throw (:exception ctx)))
   :respond-with-entity? true
   :can-put-to-missing? true
   :conflict? false
   :put! (fn [ctx]
           (let [putted (mc/save-and-return db coll (merge-with merge
                                                                (::resource ctx)
                                                                (::data ctx)))]
             {::putted putted}))
   :new? #(boolean (not (::resource %)))
   :handle-created #(generate-string (::putted %))
   :handle-ok (fn [ctx]
                (case (get-in ctx [:request :request-method])
                  :get (generate-string (::resource ctx))
                  :put (generate-string (::putted ctx))
                  :patch (generate-string (::patched ctx))
                  :delete {:message "Eliminated resource"
                           :resource (generate-string (::resource ctx))}))})

(defn item-entries [m id]
  (let [{:keys [conn db]} (mg/connect-via-uri (:uri m))
        coll (:name m)]
    (map-item-entries m id conn db coll)))

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
  (let [definition-maps (generate-resource-definition m)]
    (flatten (map definition-map-2-ast definition-maps))))

(defmethod custom-inject :mongo [coll entry-map]
  (let [with-database (merge
                       (select-keys entry-map [:database])
                       coll)]
    (assoc coll :uri (str (:uri coll) "/" (:database with-database)))))

(defmethod generate-asts :mongo [m]
  {:handler :bidi
   :asts (generate-mongo-asts m)})
