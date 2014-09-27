(ns ema.possible
  (:require [clojure.core.typed :as t]
            [ema.type :refer [EntryMap]]))

(t/ann possible EntryMap)
(def possible
  {:key :mongo
   :handler :bidi
   :uri "mongodb://localhost:27017"
   :database "ema"
   :collections [{:key :mongo
                  :name "user"
                  :authentication :first
                  :item-entries [:get :patch :delete]
                  :collection-entries [:post :get]}
                 {:key :mongo
                  :database "test"
                  :name "session"
                  :item-entries [:put]
                  :collection-entries [:get :post]}]
   :authentication {:second {:key :basic
                             :uri "https://www.auth.com/nvfir"
                             :collection "user"
                             :username "username"
                             :password "password"}
                    :first {:key :mongo-dynamics
                            :database "ema"
                            :collection "user"
                            :username :username
                            :password :password
                            :security :dynamic}}})
