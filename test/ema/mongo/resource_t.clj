(ns ema.mongo.resource-t
  (:use midje.sweet)
  (:require [ema.mongo.resource :refer [parse-json-malformed not-valid-id? resource-collection-entries]]
            [ema.implementation :refer [authentication]]))

(facts
 "Checking json malformed."
 (fact
  "nil for not json request"
  (parse-json-malformed
   {:request {:request-method :get}} nil) => nil)
 (fact
  "Check no body error"
  (parse-json-malformed
   {:request
    {:request-method :post
     :body nil}} nil) => [true {:ema.mongo.resource/malformed-message "No body"}])
 (fact
  "Check for malformed JSON"
  (parse-json-malformed
   {:request
    {:request-method :put
     :body "Wrong JSON"}} nil) => [true {:ema.mongo.resource/malformed-message "Malformed JSON"}])
 (fact
  "Check good JSON"
  (parse-json-malformed
   {:request
    {:request-method :patch
     :body "nil"}} :key) => [false {:key {"a" "b"}}]
     (provided
      (slurp "nil") => "{\"a\" : \"b\"}")))

(facts
 "Checking validit of id"
 (fact
  "Check wrong id"
  (not-valid-id? "random") => [true {:ema.mongo.resource/malformed-message "ID: \"random\" not valid. Please check: http://api.mongodb.org/java/2.0/org/bson/types/ObjectId.html"}])
 (fact
  "Check rigth id"
  (not-valid-id? "5434f6a7c830d077e497782e") => [false {}]))

(facts
 (let [definition-map {:public-collection-mth [:post :patch]
                       :collection-mth [:get]
                       :auth ..auth..} 
       resource-map (resource-collection-entries
                     definition-map
                     :db :coll)
       rm resource-map]
   (fact
    "check allowed-methods"
    (some #{:put} (:allowed-methods rm)) => falsey
    (some #{:get} (:allowed-methods rm)) => truthy
    (some #{:post} (:allowed-methods rm)) => truthy)
   (fact
    "Check post-redirect? value"
    (-> rm :post-redirect?) => false)
   (fact
    "Check authentication public"
    ((:allowed? rm) {:request {:request-method :post}}) => true)
   (fact
    "Checking true authentication not public"
    (let [ctx {:request {:request-method :get}}]
      ((:allowed? rm) ctx) => true
      (provided
       (authentication ..auth.. ctx) => true)))
   (fact
    "Checking false authentication not public"
    (let [ctx {:request {:request-method :put}}]
      ((:allowed? rm) ctx) => false
      (provided
       (authentication ..auth.. ctx) => false)))))
