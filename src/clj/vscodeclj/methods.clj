(ns vscodeclj.methods
  (:require [clojure.string :as str]
            [cljfmt.core :as fmt]
            [rewrite-clj.parser :as p]
            [vscodeclj.diff :as diff]
            [vscodeclj.io :as io]
            [vscodeclj.validate :as vali]
            [cheshire.core :as json]
            [clojure.core.async :refer [go >!! <!! close! chan pub alts! sub unsub timeout]]))

(def documents (atom {}))

(defn initialize-method [params]
    {:capabilities {
            :textDocumentSync 1
            :documentFormattingProvider true
            :definitionProvider true
        }})

(defn document-did-load-method [msg]
    (let [uri (get-in msg [:params :textDocument :uri])
          content (get-in msg [:params :textDocument :text])]
        (swap! documents assoc uri content))
    nil)

(defn document-changed-method [msg]
    (let [uri (get-in msg [:params :textDocument :uri])
          content (get-in msg [:params :contentChanges 0 :text])]
        (swap! documents assoc uri content))
    nil)

(defn document-format [msg]
    (let [uri (get-in msg [:params :textDocument :uri])
          content (get @documents uri)
          formatted (fmt/reformat-string content)
          changes (diff/diff content formatted)]
    changes))

(defn publish-diagnostics [uri]
    (let [content (get @documents uri)
          parse-diags (vali/parse-error? content)]
        (>!! io/out-chan
        {:method "textDocument/publishDiagnostics"
         :params {
            :uri uri
            :diagnostics (if parse-diags [parse-diags] [])
          }})))

(defn document-did-save [msg]
    (let [uri (get-in msg [:params :textDocument :uri])
          content (get @documents uri)]
        (go
            (publish-diagnostics uri)))
    nil)

(defn goto-definition [msg]
    {:uri "file:///Users/danielbasedow/.m2/repository/cljs-tooling/cljs-tooling/0.1.3/cljs-tooling-0.1.3.jar"
     :range {
        :start {:line 1 :character 1}
        :end {:line 2 :character 1}}})