(ns ema.mongo-t
  (:use midje.sweet)
  (:require [ema.mongo :refer [parse-json-malformed not-valid-id? collection-entries]]))

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
     :body nil}} nil) => [true {:ema.mongo/malformed-message "No body"}])
 (fact
  "Check for malformed JSON"
  (parse-json-malformed
   {:request
    {:request-method :put
     :body "Wrong JSON"}} nil) => [true {:ema.mongo/malformed-message "Malformed JSON"}])
 (fact
  "Check good JSON"
  (parse-json-malformed
   {:request
    {:request-method :patch
     :body ..sluper..}} :key) => [false {:key {"a" "b"}}]
     (provided
      (slurp ..sluper..) => "{\"a\" : \"b\"}")))

(facts
 "Checking validit of id"
 (fact
  "Check wrong id"
  (not-valid-id? "random") => [true {:ema.mongo/malformed-message "ID: \"random\" not valid. Please check: http://api.mongodb.org/java/2.0/org/bson/types/ObjectId.html"}])
 (fact
  "Check rigth id"
  (not-valid-id? "5434f6a7c830d077e497782e") => [false {}]))

(facts
 (let [ce collection-entries]
   "Testing collection entries, please note that I haven't find a better way to test this."
   (fact
    "Check post-redirct? value"
    (-> {} ce :post-redirect?) => false
    (provided
     (monger.core/connect-via-uri nil) => {}))
   (fact
    "Check allowed?"
    (-> {:public-collection-mth [:post]}
        ce :allowed?
        {:request {:request-method :post}}) => true
    (provided
     (monger.core/connect-via-uri nil) => {}))))

