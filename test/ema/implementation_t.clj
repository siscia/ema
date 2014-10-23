(ns ema.implementation-t
  (:use midje.sweet)
  (:require [ema.implementation :refer [keys-inject auth-inject custom-inject custom-auth-inject authentication generate-resource-definition connection connection-inject basic-get basic-get-inject find-resorces-def]]))

(facts
 "checking inject-basic-key"
 (let [entry-map {:key :mongo :handler :bidi :uri "local"}
       complete {:key :a :handler :b :uri :c :trivial :ok}
       miss-key {:handler :b :uri :c :trivial :ok}
       miss-all {:trivial :ok}]
   (fact
    (keys-inject complete entry-map) => complete)
   (fact
    (keys-inject miss-key entry-map) => {:key :mongo :handler :b :uri :c :trivial :ok})
   (fact
    (keys-inject miss-all entry-map) => (assoc entry-map :trivial :ok))))

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
 "checking connection-inject"
 (fact
  (connection-inject {:foo :bar}) => {:foo :bar
                                      :meta {:connection 'conn}}
  (provided
   (connection {:foo :bar}) => 'conn)))

(facts
 "checking basic-get-inject"
 (let [res-pre {:key :test
                :foo :bar
                :meta {:connection ..conn..}}
       res-post (basic-get-inject res-pre)]
   (fact
    (isa? (class (-> res-post :meta :basic-get)) clojure.lang.IFn))))

(facts
 "checking authentication"
 (fact
  ( (authentication {}) {}) => false
  (authentication {} {}) => false
  (authentication {} {} :foo) => false))

(facts
 "test find-resource-def"
 (let [res-defs [{:name "a"
                  :key :foo}
                 {:name "b"
                  :key :bar}]]
   (fact
    (find-resorces-def "a" res-defs) => {:name "a" :key :foo})
   (fact
    (find-resorces-def "c" res-defs) => nil)))
