(ns leiningen.lein-vscode-server
  (:require [leiningen.core.eval :as eval]
            [cemerick.pomegranate :as po]
            [leiningen.core.classpath :refer [get-classpath]]
            [clojure.pprint :refer [pprint]]
            [vscodeclj.symbols :as sym]
            [vscodeclj.core :as srv]
            [clojure.java.io :as io]))

(defn lein-vscode-server
  [project & args]
    (doseq [p (get-classpath project)]
      (po/add-classpath p))
    (srv/run {:src-path (first (:source-paths project))}))
