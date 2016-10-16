(ns vscodeclj.symbols
    (:require [vscodeclj.symbols.extraction :as ex]
              [clojure.string :as s]))

(defn ns->fname [namespace]
    (-> namespace
        (s/replace #"-" "_")
        (s/replace #"\." "/")
        (str ".clj")))

(def symbols (atom {}))
