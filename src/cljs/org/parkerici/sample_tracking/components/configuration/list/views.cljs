(ns org.parkerici.sample-tracking.components.configuration.list.views
  (:require [re-frame.core :refer [subscribe]]
            [org.parkerici.sample-tracking.utils.table :as table]
            [org.parkerici.sample-tracking.components.configuration.list.db]
            [org.parkerici.sample-tracking.components.configuration.list.events]
            [org.parkerici.sample-tracking.components.configuration.list.subs]))

(defn component
  []
  (fn []
    (let [config-map @(subscribe [:configuration-list/configuration-map])
          column-order [:key :entry]
          column-names {:key "Configuration Key"}
          row-maps (map (fn [key] {:key key :entry (str (get config-map key))}) (keys config-map))]
      (table/build-table column-order column-names row-maps :key {}))))