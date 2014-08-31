(ns ema.mongo
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [cheshire.core :refer [parse-string generate-string]]
            [cheshire.generate :refer [add-encoder encode-str]]
            [clojure.stacktrace :refer [e]]
            [liberator.core :refer [resource]])
  (:import org.bson.types.ObjectId))

(add-encoder org.bson.types.ObjectId encode-str)

(defn parse-json-malformed [ctx k]
  (when (#{:put :post :patch} (get-in ctx [:request :request-method]))
    (if-let [body (slurp (get-in ctx [:request :body]))]
      (do
        (try
          [false {k (parse-string body)}]
          (catch Exception e
            [true {::malformed-message "Malformed JSON"}])))
      [true {::malformed-message "No body"}])))

(defn not-valid-id? [id]
    (if (org.bson.types.ObjectId/isValid id)
      [false {}]
      [true {::malformed-message (str "ID: \"" id "\" not valid. Please check: http://api.mongodb.org/java/2.0/org/bson/types/ObjectId.html")}]))

(defn collection-entries [m]
  (let [{:keys [conn db]} (mg/connect-via-uri (:uri m))
        coll (:name m)]
    {:allowed-methods (:collection-entries m)
     :available-media-types ["text/plain" "application/json"]
     :malformed? #(parse-json-malformed % ::data)
     :post! (fn [ctx] {::new (mc/insert-and-return db coll (::data ctx))})
     :post-redirect? false
     :new? #(boolean (::new %))
     :handle-created (fn [ctx]
                       (generate-string (::new ctx)))
     :handle-ok (fn [ctx]
                  (generate-string {:data (mc/find-maps db coll)}))}))

(defn item-entries [m id]
  (let [{:keys [conn db]} (mg/connect-via-uri (:uri m))
        coll (:name m)]
     {:allowed-methods (:item-entries m)
      :available-media-types ["text/plain" "application/json"]
      :malformed? (fn [ctx]
                    (let [id-check (not-valid-id? id)]
                      (if-not (first id-check)
                        (parse-json-malformed ctx ::data)
                        id-check)))
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
                {::patched (mc/save-and-return db coll (merge-with merge (::resource ctx) (::data ctx)))})
      :handle-exception (fn [ctx]
                          (println (:exception ctx))
                          (throw (:exception ctx)))
      :respond-with-entity? true
      :can-put-to-missing? true
      :conflict? false
      :put! (fn [ctx]
              {::putted (mc/save-and-return db coll (merge-with merge (::resource ctx)
                                                                (assoc (::data ctx) :_id (ObjectId. id))))})
      :new? #(boolean (::resource %))
      :handle-ok (fn [ctx]
                   (case (get-in ctx [:request :request-method])
                     :get (generate-string (::resource ctx))
                     :put (generate-string (::putted ctx))
                     :patch (generate-string (::patched ctx))
                     :delete {:message "Eliminated resource"
                              :resource (generate-string (::resource ctx))}))}))

(defn generate-definition-map [m]
  (map (fn [collection-map]
         (merge (dissoc m :collections) collection-map)) (:collections m)))

(defn generate-asts [m]
  (let [definition-maps (generate-definition-map m)]
    (map (fn [definition-map]
           [{:method :any
             :route (:name definition-map)
             :resource
             ;; (fn [r] {:status 200
             ;;          :headers {"Content-Type" "text/plain"}
             ;;          :body (do (println "ok")
             ;;                   (str r))})} 
             (resource (collection-entries definition-map))}
            {:method :any
             :route (:name definition-map)
             :params :id
             :resource (fn [{:keys [route-params] :as req}]
                         (println req)
                         (
                          (resource (item-entries definition-map (:id params)))
                          req))}])
         definition-maps)))

(defn generete-routes [asts]
  ())
