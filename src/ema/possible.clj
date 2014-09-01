(ns ema.possible)

(def possible
  {:key :mongo
   :handler :bidi
   :uri "mongodb://localhost:27017"
   :collections [{:name "user"
                  :authentication :second
                  :item-entries [:get :put :patch :delete]
                  :collection-entries [:post :get]}
                 {:name "sessione"
                  :collection-entries [:get :post]}]
   :authentication {:firts {:key :rest-call
                            :uri "https://www.auth.com/nvfir"}
                    :second {:key :mongo-auth
                             }}})
