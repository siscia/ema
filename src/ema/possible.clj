(ns ema.possible
  (:require [clojure.core.typed :as t]
            [ema.type :refer [EntryMap]]))

(t/ann possible EntryMap)
(def possible
  {:key :mongo
   :handler :bidi
   :uri "mongodb://localhost:27017"
   :collections [{:key :mongo
                  :name "user"
                  :authentication :second
                  :item-entries [:get :patch :delete]
                  :collection-entries [:post :get]}
                 {:key :mongo
                  :name "sessione"
                  :item-entries [:put]
                  :collection-entries [:get :post]}]
   :authentication {:second {:key :basic
                             :uri "https://www.auth.com/nvfir"
                             :collection "user"
                             :username "username"
                             :password "password"}}})
