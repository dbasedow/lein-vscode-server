(defproject lein-vscode-server "0.1.0-SNAPSHOT"
  :description "Visual Studio Code Clojure support"
  :url "https://github.com/dbasedow/lein-vscode-server"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[jline "2.12"]
                 [cljfmt "0.5.3"]
                 [org.clojure/core.async "0.2.385"]
                 [com.taoensso/timbre "4.7.4"]
                 [cheshire "5.6.1"]
                 [org.clojure/tools.analyzer.jvm "0.6.10"]]
  :main ^:skip-aot vscodeclj.core
  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :jvm-opts ["-Xmx1G"]
  :eval-in-leiningen true)
