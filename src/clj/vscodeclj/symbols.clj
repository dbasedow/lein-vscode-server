(ns vscodeclj.symbols
    (:require [vscodeclj.symbols.extraction :as ex]
              [vscodeclj.globals :refer [*src-path*]]
              [vscodeclj.symbols.util :as util]
              [clojure.pprint :refer [pprint]]
              [taoensso.timbre :as l]
              [clojure.tools.analyzer.jvm :as analyzer]
              [clojure.tools.analyzer.env :as env :refer [*env*]]
              [clojure.java.io :as io]
              [clojure.string :as s])
    (:import [java.net.URI]))

(def symbols (atom {}))

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

(defn to-inner-map [res [sym details]]
    (assoc res sym details))

(defn to-map [res [ns symbols]]
    (->> symbols
         (reduce to-inner-map {})
         (assoc res ns)))

(defn initialize [dir]
    (->> (find-ws-namespaces dir)
         (map ex/analyze-ns)
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

(defn find-matches [partial-symbol ns]
  (l/error "-" partial-symbol "-")
  (env/ensure (analyzer/global-env)
    (-> (env/deref-env)
        (get-in [:namespaces (symbol ns) :mappings])
        seq
        (->>
            (filter #(s/starts-with? (str (first %)) partial-symbol))))))

(defn get-completion-items [uri doc ln ch]
  (try
      (-> (util/get-symbol-to-complete doc ln ch)
          (find-matches (uri->ns uri *src-path*))
          (->> (take 10)))
  (catch Exception e (l/error e))))
