(ns ema.mongo-t
  (:use midje.sweet)
  (:require [ema.implementation :refer [connection]]
            [monger.core :refer [connect-via-uri]]
            [ema.mongo]))

(facts
 "check connections"
 (fact
  (connection  {:key :mongo
                :uri ..uri..
                :name ..name..}) => {:coll ..name..
                                     :conn :conn
                                     :db :db}
                (provided (connect-via-uri ..uri..) => {:conn :conn
                                                        :db :db})))

