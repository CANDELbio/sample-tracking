(ns org.parkerici.sample-tracking.components.kit.propose-edits.view.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [clojure.data :as data]
            [org.parkerici.sample-tracking.pages.manifest :as manifest-page]
            [org.parkerici.sample-tracking.components.kit.utils :as kit-utils]
            [org.parkerici.sample-tracking.components.kit.propose-edits.view.db]
            [org.parkerici.sample-tracking.components.kit.propose-edits.view.events]
            [org.parkerici.sample-tracking.components.kit.propose-edits.view.subs]))

(defn stringify-manifest-map-dates
  "The original manifest map is pulled out of the DB and turned into JSON whereas the updated manifest map is stored
  in the DB as a JSON blob. As a result, the date functions have a slightly different format, which causes them to
  be incorrectly flagged as different by clojure.data/diff. This function turns them into strings so that diff
  doesn't incorrectly flag them."
  [manifest-map]
  (let [date-parse-fn (fn [date] (when (some? date) (.toLocaleDateString (js/Date. date))))
        time-parse-fn (fn [time] (when (some? time) (.format (js/dayjs time) "HH:mm")))
        raw-collection-date (:collection-date manifest-map)
        raw-collection-time (:collection-time manifest-map)
        selected-fields (:selected-form-type-fields manifest-map)
        raw-field-values (:form-type-field-values manifest-map)
        updated-field-values (reduce (fn [updated-values field]
                                       (let [field-id (keyword (:field-id field))
                                             field-type (:type field)]
                                         (when-let [field-value (get raw-field-values field-id)]
                                           (if (not= field-type "time")
                                             (assoc updated-values field-id field-value)
                                             (assoc updated-values field-id (time-parse-fn field-value))))))
                                     {} selected-fields)]
    (-> manifest-map
        (assoc :collection-date (date-parse-fn raw-collection-date))
        (assoc :collection-time (time-parse-fn raw-collection-time))
        (assoc :form-type-field-values updated-field-values))))

(defn build-manifest-map
  [proposed-edit-map]
  (let [date-time-display-fn (fn [value] value)
        comments (or (:comments proposed-edit-map) "")]
    (-> proposed-edit-map
        (stringify-manifest-map-dates)
        (assoc :comments comments)                          ;Comment key can be missing if blank. Add it in case it is.
        (dissoc :archived)
        (dissoc :complete)
        (assoc :date-display-fn date-time-display-fn)
        (assoc :time-display-fn date-time-display-fn))))

(defn component
  []
  (fn []
    (let [proposed-edit @(subscribe [:proposed-edit/proposed-edit])
          original-manifest-map (build-manifest-map (:original-map proposed-edit))
          updated-manifest-map (build-manifest-map (:update-map proposed-edit))
          map-diffs (data/diff original-manifest-map updated-manifest-map)
          status (:status proposed-edit)]
      [:<>
       [:table {:width "100%"}
        [:thead
         [:tr
          [:th "Original Map"]
          [:th "Updated Map"]]]
        [:tbody
         [:tr
          [:td (manifest-page/content (assoc original-manifest-map :key-prefix "original") (first map-diffs))]
          [:td (manifest-page/content (assoc updated-manifest-map :key-prefix "updated") (second map-diffs))]]]]
       (when (= status "pending")
         [:<>
          [:button.btn.btn-secondary
           {:type     "button"
            :on-click #(.back js/history)}
           "Cancel"]
          [:div.spacer]
          (kit-utils/confirm-button
            "Are you sure you want to approve this edit?"
            [:proposed-edit/set-status "approved"]
            "Approve")
          [:div.spacer]
          (kit-utils/confirm-button
            "Are you sure you want to deny this edit?"
            [:proposed-edit/set-status "denied"]
            "Deny")])])))