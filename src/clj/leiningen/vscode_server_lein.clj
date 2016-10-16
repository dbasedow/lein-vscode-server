(ns leiningen.vscode-server-lein
  (:require [vscodeclj.core :as server]))

(defn vscode-server-lein
  "I don't do a lot."
  [project & args]
  (server/run))
