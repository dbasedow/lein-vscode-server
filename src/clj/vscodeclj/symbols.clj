(ns vscodeclj.symbols
    (:require [vscodeclj.symbols.extraction :as ex]
              [clojure.string :as s]))

(defn ns->fname [namespace]
    (-> namespace
        (s/replace #"-" "_")
        (s/replace #"\." "/")
        (str ".clj")))

(def symbols (atom {}))

(defn is-dir?
    [file]
    (.isDirectory file))

(defn is-clj-file? 
    [file]
    (-> (.getName file)
        (.endsWith ".clj")))

(defn clj-files-in-dir [root]
    (->> (clojure.java.io/file root)
         file-seq
         (filter (complement is-dir?))
         (filter is-clj-file?)))

(defn remove-path-prefix [prefix fname]
    (s/replace-first fname prefix ""))

(defn remove-clj-suffix [fname]
    (s/replace-first fname #"\.clj$" ""))

(defn remove-leading-slash [fname]
    (s/replace fname #"^/", ""))

(defn fname->ns [fname]
    (s/replace fname "/" "."))

;Assumption that will not always work
(defn fname->lisp-case [fname]
    (s/replace fname "_" "-"))

(defn find-ws-namespaces [dir]
    (->> (clj-files-in-dir dir)
         (map #(.getPath %))
         (map (partial remove-path-prefix dir))
         (map remove-clj-suffix)
         (map remove-leading-slash)
         (map fname->ns)
         (map fname->lisp-case)))

(defn analyze-ns [namespace]
    (-> (ex/analyze-ns namespace)))