(ns vscodeclj.symbols.util
    (:require [clojure.string :as str])
    (:import [java.io BufferedReader StringReader]))

(def symbol-chars (char-array "() "))

(defn symbol-char? [c]
  (not (some #(= c %) symbol-chars)))

(defn find-symbol-start [s pos]
  (if (not (symbol-char? (get s pos)))
      pos
      (recur s (dec pos))))

(defn get-line [doc ln]
  (-> doc
      StringReader.
      BufferedReader.
      line-seq
      (nth ln)))

(defn get-symbol-to-complete [doc ln ch]
  (let [line (get-line doc ln)
        startpos (find-symbol-start line (dec ch))]
    (subs line (inc startpos) ch)))