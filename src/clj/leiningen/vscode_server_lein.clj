(ns leiningen.vscode-server-lein
  (:require [leiningen.core.eval :as eval]
            [cemerick.pomegranate :as po]
            [leiningen.core.classpath :refer [get-classpath]]
            [clojure.pprint :refer [pprint]]
            [vscodeclj.symbols :as sym]
            [vscodeclj.core :as srv]
            [clojure.java.io :as io]))

(defn vscode-server-lein
  "I don't do a lot."
  [project & args]
    (doseq [p (get-classpath project)]
      (po/add-classpath p))
    (srv/run {:src-path (first (:source-paths project))}))
