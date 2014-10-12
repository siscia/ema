(ns ema.authentication
  (:require [ema.implementation :refer [authentication custom-auth-inject]]
            [monger.collection :as mc]
            [monger.core :as mg]
            [ring.middleware.basic-authentication :refer [wrap-basic-authentication]])
  (:import org.mindrot.jbcrypt.BCrypt))

(defn wrapper-mongo []
  (fn [req]
    (wrap-basic-authentication
     req
     (fn [u p] {:username u
                :password p}))))

(defn check-password [plain hash]
  (try (BCrypt/checkpw plain hash)
       (catch Exception e
         false)))

(defn hash-password
  ([password salt]
     (BCrypt/hashpw password (BCrypt/gensalt salt)))
  ([password]
     (BCrypt/hashpw password (BCrypt/gensalt))))

(defn empty-collection? [db coll]
  "Check if a collection in the database is empty."
  (not (seq (mc/find-maps db coll))))

(defmethod custom-auth-inject :mongo-dynamics
  [res-pre entry-map]
  (let [auth-map (:auth res-pre)
        auth-map (merge (select-keys entry-map [:database :uri])
                        auth-map)
        auth-map (assoc auth-map :uri
                        (str (:uri auth-map) "/" (:name auth-map)))]
    (assoc res-pre :auth auth-map)))

(defmethod authentication :mongo-dynamics
  ([m]
     (partial authentication m))
  ([m ctx]
     (authentication m ctx :user))
  ([m ctx k]
     (let [{:keys [conn db]} (mg/connect-via-uri (:uri m))
           coll (:name m)]
       (if (and (= (:security m) :dynamic)
                (empty-collection? db coll))
         true
         (let [{:keys [username password]}
               (-> ctx :request :basic-authentication)
               user (mc/find-one-as-map
                     db coll {(:username m) username})]
           (if (check-password password ((:password m) username))
             [true {k user}]
             false))))))
