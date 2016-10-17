(ns leiningen.vscode-server-lein
  (:require [vscodeclj.core :as server]
            [vscodeclj.symbols :as sym]
            [clojure.pprint :refer [pprint]]))

(defn vscode-server-lein
  "I don't do a lot."
  [project & args]
  (->> project
       :source-paths
       first
       sym/find-ws-namespaces
       (map sym/analyze-ns)
       pprint))
  ;(server/run))
