
# ema

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

### To Be Implemented

* Authentication
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
                 [ema "0.0.3"]] ;; added
  :ring {:handler awesome-name.core/app}) ;;added
```
  
Note that I have added the dependecies and a ring handler.

At this point we need to generate our handler.

```clojure
(ns awesome-name.core
  (:require [ema.core :refer [ema]]))

(def setting
  {:handler :bidi
   :uri "mongodb://localhost:27017"
   :database "ema"
   :collections [{:key :mongo
                  :name "user"
                  :authentication :second
                  :item-entries [:get :patch :delete] ;; miss put
                  :collection-entries [:post :get]}
                 {:key :mongo
				  :database "session" ;; attention here
                  :name "session"
                  :item-entries [:put] ;; miss get patch delete
                  :collection-entries [:get :post]}]
   :authentication {:second {:key :basic
                             :uri "https://www.auth.com/awesome-app"
                             :collection "user"
                             :username "username"
                             :password "password"}}})

(def app
  (ema setting))

```

Now you need to run an instance of MongoDB in your machine, or just change the `uri` key in something more appropriate, like the url of a cloud instance of MongoDB.

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

## Contributing

As always contributing is very much welcome. Just send a Pull Request or Open an Issue.

## License

Copyright © 2014 Simone Mosciatti

Distributed under the Eclipse Public License either version 1.0

## Thanks

Thanks to Emanuela Furfaro for being my muse.
