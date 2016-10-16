(ns vscodeclj.diff
  (:require [clojure.string :as str] [cheshire.core :as json])
  (:import [name.fraser.neil.plaintext diff_match_patch diff_match_patch$Operation]))


(defn calc-end [{:keys [line character]} diff]
    (let [lines (str/split (.text diff) #"\n" -1)
          line-delta (- (count lines) 1)
          end-line (+ line line-delta)
          char-delta (count (last lines))
          end-char (if (> line-delta 0) char-delta (+ char-delta character))]
        {:line end-line :character end-char}))

(defn build-edit
    "return vector of [TextEdit lineToContinue charToContinue]"
    [start end diff]
    (condp = (.operation diff)
        diff_match_patch$Operation/DELETE [{:range {:start start :end end} :newText ""} (:line end) (:character end)]
        diff_match_patch$Operation/INSERT [{:range {:start start :end start} :newText (.text diff)} (:line start) (:character start)]
        diff_match_patch$Operation/EQUAL [nil (:line end) (:character end)]))

(defn get-text-edits [diffs]
    (loop [diffs diffs
           line 0
           character 0
           edits []]
        (if-let [diff (first diffs)]
            (let [start {:line line :character character}
                  end (calc-end start diff)
                  [edit line character] (build-edit start end diff)]
                (recur  (rest diffs)
                        line
                        character
                        (conj edits edit)))
            edits)))

(defn diff [old new]
    (let [differ (diff_match_patch.)]
        (->> (.diff_main differ old new)
             (get-text-edits)
             (filter (complement nil?)))))
