(ns org.parkerici.sample-tracking.components.kit.list.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [clojure.string :as str]
            [org.parkerici.sample-tracking.routes :as routes]
            [org.parkerici.sample-tracking.utils.user :as user-utils]
            [org.parkerici.sample-tracking.utils.table :as table]
            [org.parkerici.sample-tracking.utils.time :as time]
            [org.parkerici.sample-tracking.components.kit.list.db]
            [org.parkerici.sample-tracking.components.kit.list.events]
            [org.parkerici.sample-tracking.components.kit.list.subs]))

(defn complete-select-input
  []
  [:select {:on-change (fn [evt]
                         (dispatch [:kit-list/set-kit-filter (-> evt .-target .-value)]))}
   [:option {:value "complete"} "Complete kits"]
   [:option {:value "incomplete"} "Incomplete kits"]
   [:option {:value "archived"} "Archived kits"]
   [:option {:value "all"} "All kits"]])

(defn component
  []
  (fn []
    (let [kits @(subscribe [:kit-list/kits])
          filter @(subscribe [:kit-list/filter])
          user @(subscribe [:user])
          base-export-url "/api/sample/export"
          export-url (case filter
                       "complete" (str base-export-url "?complete=true&archived=false")
                       "incomplete" (str base-export-url "?complete=false&archived=false")
                       "archived" (str base-export-url "?archived=true")
                       base-export-url)

          user-can-edit (or (user-utils/user-is-admin user) (user-utils/user-is-editor user))
          site-user (user-utils/user-is-site-user user)
          column-order (cond-> [:kit-id :kit-type-name :participant-id :timepoints :collection-timestamp
                                :completing-email]
                               site-user (conj :pending-edit-email)
                               site-user (conj :pending-edit-timestamp)
                               (not site-user) (conj :edit-timestamp)
                               site-user (conj :propose-edits)
                               (not site-user) (conj :view)
                               user-can-edit (conj :edit)
                               (not site-user) (conj :history))
          column-names {:completing-email "Submitting Email" :kit-type-name "Kit Type"}
          display-fns {:timepoints
                       (fn [row]
                         (str/join ", " (map :timepoint-name (:timepoints row))))
                       :collection-timestamp
                       (fn [row] (time/timestamp-string-to-formatted-string (:collection-timestamp row)))
                       :pending-edit-email
                       (fn [row] (:email (first (:pending-edits row))))
                       :pending-edit-timestamp
                       (fn [row] (time/timestamp-string-to-formatted-string (:time (first (:pending-edits row)))))
                       :edit-timestamp
                       (fn [row] (time/timestamp-string-to-formatted-string (:time (first (:history row)))))
                       :propose-edits
                       (fn [row]
                         [:a {:href (routes/path-for :propose-kit-edits :uuid (:uuid row))} "Propose Edits for Kit"])
                       :view
                       (fn [row]
                         [:a {:href (routes/path-for :kit-view :uuid (:uuid row))} "View Kit"])
                       :edit
                       (fn [row]
                         (if-let [pending-edit (first (:pending-edits row))]
                           [:a {:href (routes/path-for :proposed-kit-edit-view :uuid (:uuid pending-edit))} "View Pending Edit"]
                           [:a {:href (routes/path-for :kit-edit :uuid (:uuid row))} "Edit Kit"]))
                       :history
                       (fn [row]
                         [:a {:href (routes/path-for :entity-history :uuid (:uuid row))} "History"])}
          ]
      [:<>
       (when-not site-user
         [:div
          (complete-select-input)
          [:div.spacer]
          [:a {:href export-url :download "" :target "_blank" :rel "noopener noreferrer"} "Export to CSV"]])
       (table/build-table column-order column-names kits :uuid display-fns)])))
