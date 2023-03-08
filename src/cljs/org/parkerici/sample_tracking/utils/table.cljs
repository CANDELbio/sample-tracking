(ns org.parkerici.sample-tracking.utils.table
  (:require [clojure.string :as str]))

(defn- title-case-symbol
  [symbol]
  (let [name (name symbol)
        split-name (str/split name #"-")
        capitalized (map str/capitalize split-name)]
    (str/join " " capitalized)))

(defn- table-header
  [column-order column-names]
  [:thead
   [:tr
    (map (fn [column-symbol] [:th {:key (name column-symbol)}
                              (if-let [column-name (get column-names column-symbol)]
                                column-name
                                (title-case-symbol column-symbol))]) column-order)]])

(defn- table-row
  [column-order row-values row-key display-fns]
  (let [key-value (get row-values row-key)]
    [:tr {:key key-value}
     (map (fn [column-symbol] [:td {:key (str key-value (name column-symbol))}
                               (if-let [column-display-fn (get display-fns column-symbol)]
                                 (column-display-fn row-values)
                                 (get row-values column-symbol))
                               ]) column-order)]))

(defn build-table
  [column-order column-names row-maps row-key row-display-fns]
  [:table {:width "100%"}
   (when column-names (table-header column-order column-names))
   [:tbody (map (fn [row-map] (table-row column-order row-map row-key row-display-fns)) row-maps)]])

(defn map-to-table
  [m]
  (let [key-val-list (map (fn [e] {:key (name e) :value (str (get m e))}) (keys m))]
    (build-table [:key :value] nil key-val-list :key {})))
