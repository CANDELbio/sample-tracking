(ns org.parkerici.sample-tracking.components.kit.propose-edits.list.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [clojure.string :as str]
            [org.parkerici.sample-tracking.routes :as routes]
            [org.parkerici.sample-tracking.utils.table :as table]
            [org.parkerici.sample-tracking.utils.time :as time]
            [org.parkerici.sample-tracking.components.kit.propose-edits.list.db]
            [org.parkerici.sample-tracking.components.kit.propose-edits.list.events]
            [org.parkerici.sample-tracking.components.kit.propose-edits.list.subs]))

(defn status-select-input
  []
  [:select {:on-change (fn [evt]
                         (dispatch [:proposed-edits-list/set-filter (-> evt .-target .-value)]))}
   [:option {:value "pending"} "Pending Proposals"]
   [:option {:value "approved"} "Approved Proposals"]
   [:option {:value "denied"} "Denied Proposals"]
   [:option {:value ""} "All Proposals"]])

(defn component
  []
  (fn []
    (let [proposed-edits @(subscribe [:proposed-edits-list/proposed-edits])
          column-order [:kit-id :kit-type-name :participant-id :timepoints :collection-timestamp :kit-email
                        :email :status :time :view]
          column-names {:kit-type-name "Kit Type" :kit-email "Manifest Submission Email"
                        :email         "Editing Email" :time "Edit Submission Time"}
          display-fns {:kit-id
                       (fn [row]
                         [:a {:href (routes/path-for :kit-view :uuid (:kit-uuid row))} (:kit-id row)])
                       :timepoints
                       (fn [row]
                         (str/join ", " (map :timepoint-name (:timepoints row))))
                       :time                 (fn [row] (time/timestamp-string-to-formatted-string (:time row)))
                       :collection-timestamp (fn [row] (time/timestamp-string-to-formatted-string (:collection-timestamp row)))
                       :view
                       (fn [row]
                         [:a {:href (routes/path-for :proposed-kit-edit-view :uuid (:uuid row))} "View"])}]
      [:<>
       (status-select-input)
       (table/build-table column-order column-names proposed-edits :uuid display-fns)])))