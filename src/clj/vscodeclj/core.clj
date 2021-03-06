(ns vscodeclj.core
  (:require [clojure.string :as str]
            [clojure.tools.nrepl.server :refer [start-server stop-server]]
            [vscodeclj.methods :as methods]
            [vscodeclj.globals :refer [*src-path*]]
            [vscodeclj.io :as io]
            [clojure.java.io]
            [clojure.pprint :refer [pprint]]
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
      "textDocument/completion" (methods/completion params)
      "custom/getJarContent" (methods/get-jar-content params)
      nil))

(defn make-response [body id]
  {:id id
   :result body})

(defn handle-msg [enc]
  (let [{:keys [id method params] :as msg} (json/parse-string enc keyword)]
    (some-> (dispatch method params)
            (make-response id))))

(defn run [project]
  (binding [*src-path* (:src-path project)]
      (loop []
        (let [headers (io/read-headers)
          payload (io/read-payload headers)]
;          (spit "cmds.in" (str "Content-Length: " (get headers "Content-Length") "\r\n\r\n") :append true)
;          (spit "cmds.in" (str payload) :append true)
          (some->> (handle-msg payload)
                   (>!! io/out-chan))
          (recur)))))

(defn -main [& args]
  (run {:src-path (.getAbsolutePath (clojure.java.io/file "src/clj"))}))
