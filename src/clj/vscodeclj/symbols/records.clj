(ns vscodeclj.symbols.records)

(defrecord Position [file line character])

(defrecord SymbolDetails [docstr])

(defrecord Symbol [exported? position details])