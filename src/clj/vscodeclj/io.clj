(ns vscodeclj.io
  (:require [clojure.string :as str]
            [cheshire.core :as json]
            [taoensso.timbre :as l]
            [clojure.core.async :refer [go-loop >!! <! close! chan pub alts! sub unsub timeout]])
  (:import [jline.console ConsoleReader]))

(def out-chan (chan 20))

(defn parse-header-line [line]
  (str/split line #": "))

(defn read-line-unbuf []
  (let [cr (ConsoleReader.)]
    (loop [content ""]
      (let [c (.readCharacter cr)]
        (if (= c 10)
          (str/trimr content)
          (recur (str content (char c))))))))

(defn read-headers []
  (loop [headers {}]
    (let [line (read-line-unbuf)]
      (if (empty? line)
        headers
        (let [[header value] (parse-header-line line)]
          (recur (assoc headers header value)))))))

(defn read-payload [headers]
  (let [length (Integer. (get headers "Content-Length" 0))
        cr (ConsoleReader.)]
    (loop [content "" left length]
      (if (= left 0)
        content
        (let [c (char (.readCharacter cr))]
          (recur (str content c) (dec left)))))))

(defn add-content-length [resp]
  (let [length (count resp)]
    (str "Content-Length: " length "\r\n" "\r\n" resp)))

(defn log-and-ret [msg]
    (l/error msg)
    msg)

(go-loop []
    (->>  (<! out-chan)
            log-and-ret
            json/generate-string
            add-content-length
            print)
    (flush)
    (recur))
