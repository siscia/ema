(ns ema.possible)

(def possible
  {:key :mongo
   :uri "mongodb://localhost:27017/a"
   :collections [{:name "user"
                  :authentication :second
                  :item-entries [:get :put :patch :delete]
                  :collection-entries [:post :get]}
                 {:name "sessione"
                  :item-entries [:get :post]}]
   :authentication {:firts {:key :rest-call
                            :uri "https://www.auth.com/nvfir"}
                    :second {:key :mongo-auth
                             }}})
