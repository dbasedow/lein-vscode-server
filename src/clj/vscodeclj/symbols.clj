(ns vscodeclj.symbols
    (:require [vscodeclj.symbols.extraction :as ex]
              [vscodeclj.globals :refer [*src-path*]]
              [clojure.pprint :refer [pprint]]
              [taoensso.timbre :as l]
              [clojure.java.io :as io]
              [clojure.string :as s])
    (:import [java.net.URI]))

(def symbols (atom {}))

;(add-watch symbols :dbg-watcher (fn [key obj old new] (l/info new)))

(defn ns->fname [namespace]
    (-> namespace
        (s/replace #"-" "_")
        (s/replace #"\." "/")
        (str ".clj")))

(defn is-dir?
    [file]
    (.isDirectory file))

(defn is-clj-file? 
    [file]
    (-> (.getName file)
        (.endsWith ".clj")))

(defn clj-files-in-dir [root]
    (->> (io/file root)
         file-seq
         (filter (complement is-dir?))
         (filter is-clj-file?)))

(defn remove-path-prefix [prefix fname]
    (s/replace-first fname prefix ""))

(defn remove-clj-suffix [fname]
    (s/replace-first fname #"\.clj$" ""))

(defn remove-leading-slash [fname]
    (s/replace fname #"^/", ""))

(defn slashes->dots [fname]
    (s/replace fname "/" "."))

;Assumption that will not always work
(defn fname->lisp-case [fname]
    (s/replace fname "_" "-"))

(defn fname->ns [fname src-root]
    (-> (remove-path-prefix src-root fname)
        remove-clj-suffix
        remove-leading-slash
        slashes->dots
        fname->lisp-case))

(defn find-ws-namespaces [dir]
    (->> (clj-files-in-dir dir)
         (map #(.getPath %))
         (map (partial remove-path-prefix dir))
         (map remove-clj-suffix)
         (map remove-leading-slash)
         (map slashes->dots)
         (map fname->lisp-case)))

(defn analyze-file [fname]
    [fname (ex/analyze-file fname)])

(defn to-inner-map [res [sym details]]
    (assoc res sym details))

(defn to-map [res [ns symbols]]
    (->> symbols
         (reduce to-inner-map {})
         (assoc res ns)))

(defn initialize [dir]
    (->> ;(clj-files-in-dir dir)
         (find-ws-namespaces dir)
         ;(map analyze-file)
         (map ex/analyze-ns)
         ;(reduce to-map {})
         pprint))

(defn uri->ns [uri srcroot]
    (-> uri
        java.net.URI.
        .getPath
        (fname->ns srcroot)))

(defn update-symbols! [ns syms]
    (swap! symbols assoc ns syms))

(defn update-symbols-for-uri! [uri]
  (try
    (let [ns (uri->ns uri *src-path*)
          symbols (ex/analyze-ns ns)]
        (update-symbols! ns symbols))
    (catch Exception e (l/error e))))
