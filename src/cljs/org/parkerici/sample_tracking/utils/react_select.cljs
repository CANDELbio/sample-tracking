(ns org.parkerici.sample-tracking.utils.react-select
  (:require [org.parkerici.sample-tracking.utils.collection :as coll]))

(defn format-select-options
  [raw-options value-key label-key]
  (into [] (for [option raw-options]
             {:value (get option value-key) :label (get option label-key)})))

(defn format-selected-option
  [selected-ids options]
  (filter #(coll/in? selected-ids (:value %)) options))

(defn select-theme-fn
  [theme]
  (let [clj-theme (js->clj theme)]
    (clj->js (assoc clj-theme "colors" (merge (get clj-theme "colors") {"primary"   "#685bc7"
                                                                        "primary75" "#857ad6"
                                                                        "primary50" "#978ce6"
                                                                        "primary25" "#b8aefc"})))))