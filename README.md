
# ema

![ema](https://raw.githubusercontent.com/siscia/ema/master/resources/logo/ema.jpeg)

`Ema` provides a simple and easy abstraction to build REST service.

`Ema` is a very little library built on top of [Liberator](http://clojure-liberator.github.io/liberator/).

The main idea is too provide a simple and yet powerful abstraction over Liberator itself.

## Dependencies

[![Clojars Project](http://clojars.org/ema/latest-version.svg)](http://clojars.org/ema)

## Maturity

The project has just started thus it is not mature enough for any use.

### Implemented

* MongoDB layer
* bidi (routing) layer
* Authentication

### To Be Implemented

* Manage Middleware
* GET search
* SQL layer

## Usage

The project is still unusable for any non-trivial project.

However you can already have an idea of the way to use `Ema`

Create a new clojure project

```clojure
lein new awesome-name
```

Make the project.clj look like this.

```clojure

(defproject awesome-name "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [ema "0.0.7"]         ;; added
                 [http-kit "2.1.19"]]  ;; added
  :main awesome-name.core)             ;; added

```
Note that I have added the dependecies.

At this point we need to generate our handler.

```clojure
(ns awesome-name.core
  (:require [ema.core :refer [ema]]
            [org.httpkit.server :refer [run-server]]))

(def setting
  {:key :mongo
   :handler :bidi
   :uri "mongodb://localhost:27017"
   :database "ema"
   :resources [{:key :mongo
                :name "user"
                :auth :first
                :item-mth [:patch :delete :post] ;; no `put`
                :collection-mth [:post]
                :public-item-mth [:get]
                :public-collection-mth [:get :post]}
               {:key :mongo
                :auth :first
                :database "test" 
                :name "session"
                :item-mth [:put] ;; no `get`, `patch` nor `delete`
                :collection-mth [:post]
                :public-collection-mth [:get]}]
   :auth {:first {:key :mongo-dynamics
                  :database "ema"
                  :name "user"
                  :username :username
                  :password :password
                  :security :dynamic}}})

(def app
  (ema setting))

(defn -main []
  (run-server app {:port 8000}))

```

Now you need to run an instance of MongoDB in your machine, or just change the `uri` key in something more appropriate, like the url of a cloud instance of MongoDB.

Now run the following in your console

``` bash
cd
cd /awesome-name
lein run
```

and you should be ready to go.

Now, if everything went smootly you are serving two REST resource, `user` and `session`.

Please note the configuration: you won't be able to `put` on `user` nor you will be able to `get`, `patch` and `delete` a single `session`.

Of course you can change that.

Also, `user` and `session` are not in the same database (in the mongodb meaning).

`user` is in the "ema" database while `session` is in the "session" database.

If you add other collections, let say `author` and `book`

```clojure
{
 :name "authors"
 :collection-entries [:post :get]
 :list-entries [:get :put :patch]
}
{
 :name "book"
 :database "another-database"
 :collection-entries [:post :get]
 :list-entries [:get :put :patch]
} 
```

`author` will be under the "ema" database while `book` will write in the "another-database".

```bash
simo@simo:~/ema$ curl -X POST -d "{\"siscia\" : \"pass\"}" http://localhost:3000/user
{"siscia":"pass","_id":"5423934fc8300b83ff02e5fe"}
simo@simo:~/ema$ curl  http://localhost:3000/user
{"data":[{"_id":"5423934fc8300b83ff02e5fe","siscia":"pass"}]}
```

## Docker

I also have packed ema itself as a Docker.

You still need a configuration file in .edn format (a clojure map).

Suppose you have save the configuration file in `/ema/config.edn` to run the docker you need to run the following.

```bash
sudo docker run --net="host" -v ~/ema/:/ema siscia/ema:lastest "/ema/config.edn" 
```

And you should be running.

## Issues

For any problem, if you don't find the doc clear or for really anything you can just open an issue here.

## Contributing

As always contributing is very much welcome. Just send a Pull Request or Open an Issue.

## License

Copyright © 2014 Simone Mosciatti

Distributed under the Eclipse Public License either version 1.0

## Thanks

Thanks to Emanuela Furfaro for being my inspiration.
