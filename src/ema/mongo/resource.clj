(ns ema.mongo.resource
  (:require [monger.collection :as mc]
            [cheshire.core :refer [parse-string generate-string]]
            [cheshire.generate :refer [add-encoder encode-str]]

            [ema.implementation :refer [authentication]])
  (:import org.bson.types.ObjectId))

;; Dictionary:

;; + m : map, rappresent the resource
;; + db : ObjectMongo, necessary to connect to mongo and execute the queries
;; + coll : string, the collection at which the resource belong
;; + id : string, id of the resource

(defn not-valid-id? [id]
    (if (org.bson.types.ObjectId/isValid id)
      [false {}]
      [true {::malformed-message (str "ID: \"" id "\" not valid. Please check: http://api.mongodb.org/java/2.0/org/bson/types/ObjectId.html")}]))

(add-encoder org.bson.types.ObjectId encode-str)

(defn parse-json-malformed [ctx k]
  (when (#{:put :post :patch} (get-in ctx [:request :request-method]))
    (if-let [body (get-in ctx [:request :body])]
      (try
        [false {k (parse-string (slurp body))}]
        (catch Exception e
          [true {::malformed-message "Malformed JSON"}]))
      [true {::malformed-message "No body"}])))


(defn resource-collection-entries [m {:keys [db coll]}]
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
                (let [query (get-in ctx [:request :query-params])]
                  (generate-string {:data (mc/find-maps db coll query)})))})


(defn resource-item-entries [m id {:keys [db coll]}]
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

