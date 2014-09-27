(ns ema.authentication
  (:require [ema.implementation :refer [authentication]]
            [monger.collection :as mc]
            [monger.core :as mg]
            [ring.middleware.basic-authentication :refer [wrap-basic-authentication]])
  (:import org.mindrot.jbcrypt.BCrypt))

(defn wrapper-mongo [req]
  (wrap-basic-authentication
   req identity (fn [u p] {:username u
                           :password p})))

(defn check-password [plain hash]
  (BCrypt/checkpw plain hash))

(defn hash-password
  ([password salt]
     (BCrypt/hashpw password (BCrypt/gensalt salt)))
  ([password]
     (BCrypt/hashpw password (BCrypt/gensalt))))

(defmethod authentication :mongo-dynamics [m]
  (println "YEEEEEEEEEEEEEAAH")
  (let [{:keys [conn db]} (mg/connect-via-uri (:uri m))
        coll (:name m)]
    (if (and (-> m :security :dynamic)
             (not (mc/find db coll))) ;; empty db
      true
      (fn [ctx]
        (let [{:keys [user password]} (:basic-authentication ctx)
              user (mc/find-one-as-map
                    db coll {((:username m) ctx) user})
              _ (println "ok")]
          (if (check-password ((:password m) ctx) ((:password m) user))
            [true user]
            false))))))
