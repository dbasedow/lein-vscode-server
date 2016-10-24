(ns vscodeclj.methods
  (:require [clojure.string :as str]
            [cljfmt.core :as fmt]
            [rewrite-clj.parser :as p]
            [vscodeclj.diff :as diff]
            [taoensso.timbre :as l]
            [vscodeclj.io :as io]
            [vscodeclj.symbols :as sym]
            [vscodeclj.symbols.util :as sym-util]
            [clojure.java.io]
            [vscodeclj.validate :as vali]
            [cheshire.core :as json]
            [clojure.core.async :refer [go >!! <!! close! chan pub alts! sub unsub timeout]]))

(def documents (atom {}))

(defn initialize-method [params]
    {:capabilities {
            :textDocumentSync 1
            :documentFormattingProvider true
            :definitionProvider true
            :completionProvider true
        }})

(defn document-did-load-method [msg]
    (let [uri (get-in msg [:textDocument :uri])
          content (get-in msg [:textDocument :text])]
        (swap! documents assoc uri content)
        (sym/update-symbols-for-uri! uri))
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
    (let [uri (get-in msg [:textDocument :uri])
          content (get @documents uri)]
        (go
            (l/error "should update symbol table")
            (publish-diagnostics uri)))
    nil)

(defn goto-definition [msg]
    {:uri ""
     :range {
        :start {:line 1 :character 1}
        :end {:line 2 :character 1}}})

(defn completion [msg]
  (let [uri (get-in msg [:textDocument :uri])
        content (get @documents uri)
        {:keys [line character]} (:position msg)]
    (l/error (sym-util/get-symbol-to-complete content line character))))
