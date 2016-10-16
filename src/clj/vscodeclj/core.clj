(ns vscodeclj.core
  (:require [clojure.string :as str]
            [vscodeclj.methods :as methods]
            [vscodeclj.io :as io]
            [cheshire.core :as json]
            [taoensso.timbre :as l]
            [taoensso.timbre.appenders.core :as appenders]
            [clojure.core.async :refer [go-loop >!! <! close! chan pub alts! sub unsub timeout]])
  (:import [jline.console ConsoleReader])
  (:gen-class))

(l/merge-config!
  {:appenders {:println {:enabled? false}}})

(l/merge-config!
  {:appenders {:spit (appenders/spit-appender {:fname "server.log"})}})

(defn dispatch [method params]
  (case method
      "initialize" (methods/initialize-method params)
      "textDocument/didChange" (methods/document-changed-method params)
      "textDocument/didOpen" (methods/document-did-load-method params)
      "textDocument/formatting" (methods/document-format params)
      "textDocument/didSave" (methods/document-did-save params)
      "textDocument/definition" (methods/goto-definition params)
      nil))

(defn make-response [body id]
  {:id id
   :result body})

(defn handle-msg [enc]
  (let [{:keys [id method params] :as msg} (json/parse-string enc keyword)]
    (l/error msg)
    (some-> (dispatch method params)
            (make-response id))))

(defn run [& args]
  (loop []
    (let [headers (io/read-headers)
          payload (io/read-payload headers)]
          (some->> (handle-msg payload)
                   (>!! io/out-chan))
    (recur))))
