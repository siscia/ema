(ns ema.possible
  (:require [clojure.core.typed :as t]
            [ema.type :refer [EntryMap]]))

(t/ann possible EntryMap)
(def possible
  {:key :mongo
   :handler :bidi
   :uri "mongodb://localhost:27017"
   :database "ema"
   :resources [{:key :mongo
                :name "user"
                :authentication :first
                :item-mth [:put :patch :delete :post]
                :collection-mth [:post]
                :public-item-mth [:get]
                :public-collection-mth [:get]}
               {:key :mongo
                :authentication :first
                :database "test"
                :name "session"
                :item-mth [:put]
                :collection-mth [:post]
                :public-collection-mth [:get]}]
   :authentication {:first {:key :mongo-dynamics
                            :database "ema"
                            :name "user"
                            :username :username
                            :password :password
                            :security :dynamic}}})
