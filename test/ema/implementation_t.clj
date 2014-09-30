(ns ema.implementation-t
  (:use midje.sweet)
  (:require [ema.implementation :refer [inject-basic-keys auth-inject custom-inject custom-auth-inject authentication]]))

(facts
 "checking inject-basic-key"
 (let [entry-map {:key :mongo :handler :bidi :uri "local"}
       complete {:key :a :handler :b :uri :c :trivial :ok}
       miss-key {:handler :b :uri :c :trivial :ok}
       miss-all {:trivial :ok}]
   (fact
    (inject-basic-keys complete entry-map) => complete)
   (fact
    (inject-basic-keys miss-key entry-map) => {:key :mongo :handler :b :uri :c :trivial :ok})
   (fact
    (inject-basic-keys miss-all entry-map) => (assoc entry-map :trivial :ok))))

(facts
 "checking auth-inject 2 level implementation"
 (let [entry-map {:auth
                  {:a {:key :key
                       :foo :bar}
                   :b {:key :key-b
                       :ninja :turtles}}}]
   (fact
    (auth-inject {:name :name
                  :auth :a}
                 entry-map) => {:name :name
                                :auth {:key :key
                                       :foo :bar}})
   (fact
    (auth-inject {:little :lion
                  :auth :b}
                 entry-map) => {:little :lion
                                :auth {:key :key-b
                                       :ninja :turtles}})))
(facts
 "Checking auth-inject 1 level implementation"
 (fact
  (auth-inject {:foo :bar
                :auth true}
               {:auth
                {:pippo :pluto}}) => {:foo :bar
                                      :auth {:pippo :pluto}})
 (fact
  (auth-inject {:foo :bar
                :auth false} {:auth {:what :ever}}) => {:foo :bar
                                                        :auth false})
 (fact
  (auth-inject {:foo :bar} {:auth {:what :ever}}) => {:foo :bar}))

(facts
 "checking auth-inject 0 level implementation"
 (fact
  (auth-inject {:foo :bar
                :auth true} {:auth false}) => {:foo :bar :auth false})
 (fact
  (auth-inject {:foo :bar
                :auth :ok} {:auth "whatever"}) => {:foo :bar
                                                   :auth "whatever"}))

(facts
 "checking custom-inject"
 (let [entry-map {:foo :bar}]
   (custom-inject {:key :no-key} entry-map) => {:key :no-key}))

(facts
 "checking custom-auth-inject"
 (let [entry-map {:foo :bar}]
   (custom-auth-inject {:key :no-key} entry-map) => {:key :no-key}))

(facts
 "checking authentication"
 (fact
  ( (authentication {}) {}) => false
  (authentication {} {}) => false
  (authentication {} {} :foo) => false))
