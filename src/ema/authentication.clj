(ns ema.authentication
  (:require [ema.implementation :refer [authentication]]
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

(defmethod authentication :mongo-dynamics [m]
  (let [{:keys [conn db]} (mg/connect-via-uri (:uri m))
        coll (:name m)]
    (if (and (-> m :security :dynamic)
             (not (mc/find db coll))) ;; empty db
      true
      (fn [ctx]
        (let [{:keys [username password]} (-> ctx :request :basic-authentication)
              user (mc/find-one-as-map
                    db coll {(:username m) username})]
          (if (check-password password ((:password m) username))
            [true user]
            false))))))
