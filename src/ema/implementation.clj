(ns ema.implementation)

(defmulti generate-handler :handler)

(defmulti generate-asts :key)
