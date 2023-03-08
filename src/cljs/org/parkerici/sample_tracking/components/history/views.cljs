(ns org.parkerici.sample-tracking.components.history.views
  (:require [re-frame.core :refer [subscribe]]
            [clojure.data :as data]
            [clojure.edn :as edn]
            [org.parkerici.sample-tracking.utils.map :as map-utils]
            [org.parkerici.sample-tracking.utils.table :as table]
            [org.parkerici.sample-tracking.utils.time :as time]
            [org.parkerici.sample-tracking.components.history.db]
            [org.parkerici.sample-tracking.components.history.events]
            [org.parkerici.sample-tracking.components.history.subs]
            [org.parkerici.sample-tracking.routes :as routes]))

(defn component
  []
  (fn []
    (let [history @(subscribe [:history/histories])
          history-with-diffs (map (fn [m]
                                    (let [old-map (edn/read-string (:old-value m))
                                          new-map (edn/read-string (:new-value m))
                                          diff (data/diff old-map new-map)]
                                      (-> m
                                          (assoc :old-value (map-utils/flatten-map (first diff)))
                                          (assoc :new-value (map-utils/flatten-map (second diff)))))) history)
          column-order [:agent-email :entity-type :entity-uuid :time :old-value :new-value]
          column-names {:time "Edit Timestamp" :old-value "Pre-Update Values" :new-value "Post-Update Values"}
          display-fns {:entity-uuid
                       (fn [row-map]
                         [:a {:href (routes/path-for :entity-history :uuid (:entity-uuid row-map))} (:entity-uuid row-map)])
                       :time
                       (fn [row] (time/timestamp-string-to-formatted-string (:time row)))
                       :old-value
                       (fn [row] (table/map-to-table (:old-value row)))
                       :new-value
                       (fn [row] (table/map-to-table (:new-value row)))}]
      (table/build-table column-order column-names history-with-diffs :uuid display-fns))))
