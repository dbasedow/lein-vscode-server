(ns vscodeclj.symbols.extraction
  (:require [clojure.zip :as zip]
            [clojure.tools.analyzer.jvm :as analyzer]
            [clojure.pprint :refer [pprint]]
            [clojure.tools.analyzer.passes :as passes]
            [vscodeclj.symbols.records :as s]))
(declare analyze-ns)

(def not-nil? (complement nil?))

(defmulti ast-branch? :op)

(defmethod ast-branch? :default [x]
    (vector? x))

(defmethod ast-branch? :const [x]
    false)

(defmethod ast-branch? :def [x]
    false)

(defmethod ast-branch? :fn [x]
    (some not-nil? [(:methods x)]))

(defmethod ast-branch? :fn-method [x]
    (some not-nil? [(:body x)]))

(defmethod ast-branch? :do [x]
    (some not-nil? [(:statements x) (:ret x)]))

(defmethod ast-branch? :if [x]
    (some not-nil? [(:test x) (:then x) (:else x)]))

(defmethod ast-branch? :instance-call [x]
    (some not-nil? [(:args x) (:instance x)]))

(defmethod ast-branch? :invoke [x]
    (some not-nil? [(:fn x) (:args x)]))

(defmethod ast-branch? :quote [x]
    false)

(defmethod ast-branch? :static-call [x]
    (not-nil? (:args x)))

(defmethod ast-branch? :try [x]
    (some not-nil? [(:body x) (:catches x) (:finally x)]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti ast-children :op)

(defmethod ast-children :fn [node]
    (:methods node))

(defmethod ast-children :fn-method [node]
    [(:body node)])

(defmethod ast-children :do [node]
    (conj (:statements node) (:ret node)))

(defmethod ast-children :if [x]
    (filter not-nil? [(:test x) (:then x) (:else x)]))

(defmethod ast-children :instance-call [x]
    (conj (:args x) (:instance x)))

(defmethod ast-children :invoke [node]
    (conj (:args node) (:fn node)))

(defmethod ast-children :quote [node]
    [(:expr node)])

(defmethod ast-children :static-call [x] 
    (:args x))

(defmethod ast-children :try [node]
    (conj (:catches node) (:body node)))

(defmethod ast-children :default [node]
    node)


(defn ast-zipper [m]
  (zip/zipper 
    ast-branch?
    ast-children
    (fn [x children] 
      (if (map? x)
        (into {} children) 
        (assoc x 1 (into {} children))))
    m))

(defn invocation? [node] (= (:op node) :invoke))

(defn is-require? [node]
    (and
        (invocation? node)
        (= (get-in node [:fn :op]) :var))
        (= (get-in node [:fn :var]) #'clojure.core/require))

(defn is-refer? [node]
    (and
        (invocation? node)
;        (= (get-in node [:fn :op]) :var))
        (= (get-in node [:fn :var]) #'clojure.core/refer)))

(defn is-definition? [node]
    (= (:op node) :def))

(defn get-position [node]
    (let [meta (get-in node [:meta :env])]
        (s/->Position (:file meta) (:line meta) (:column meta))))

(defmulti handle-req-opts first)

(defmethod handle-req-opts :as
    [[_ alias]]
    [[(str alias) (s/->Symbol false nil nil)]])

(defn- handle-req-opt-refer [alias]
    [(str alias) (s/->Symbol false nil nil)])

(defmethod handle-req-opts :refer
    [[_ symbols]]
    (map handle-req-opt-refer symbols))

(defn handle-req-entry [[target-ns & opts]]
    (let [cnt (count opts)]
        (cond
            (= 0 cnt) [[(str target-ns) (s/->Symbol false nil nil)]]
            (even? cnt) (mapcat handle-req-opts (partition 2 opts)))))

(defn handle-require [node]
    (if-let [rs (:args node)]
        (->> rs
             (map #(get-in % [:expr :val]))
             (mapcat handle-req-entry))))

(defn handle-refer [node]
    (if-let [rs (:args node)]
        (->> rs
             (map #(get-in % [:expr :val]))
             (mapcat analyze-ns))))

(defn handle-definition [node]
    (let [name (:name node)
          private (get-in node [:meta :val :private] false)]
        [(str name) (s/->Symbol (not private) (get-position node) nil)]))

(defn append-symbols [symbols node]
    (cond
        (is-require? node) (into symbols (handle-require node))
        (is-refer? node) (handle-refer node)
        (is-definition? node) (conj symbols (handle-definition node))
        :else symbols))

(defn find-symbols [zipper]
    (loop [z zipper
           symstream []]
        (if (zip/end? z)
            symstream
            (recur (zip/next z) (append-symbols symstream (zip/node z))))))

(defn analyze-ns [namespace]
    (binding [analyzer/run-passes identity]
        (try
            (->> (analyzer/analyze-ns namespace)
                 ast-zipper
                 find-symbols)
            (catch Exception e
                (println e)))))

