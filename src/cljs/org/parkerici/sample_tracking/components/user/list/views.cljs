(ns org.parkerici.sample-tracking.components.user.list.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [react-select]                                  ;React component https://www.npmjs.com/package/react-select
            [org.parkerici.sample-tracking.utils.react-select :as rs-utils]
            [org.parkerici.sample-tracking.components.user.list.db]
            [org.parkerici.sample-tracking.components.user.list.events]
            [org.parkerici.sample-tracking.components.user.list.subs]))

(defn user-deactivate-button
  [user disabled]
  (let [email (:email user)]
    [:button.btn.btn-danger
     {:type     "button"
      :disabled disabled
      :on-click (fn [] (when (js/confirm (str "Are you sure you want to deactivate " email "?"))
                         (dispatch [:user-list/delete-user (:email user)])))}
     "Deactivate"]))

(defn user-role-select
  [user roles]
  (let [formatted-options (rs-utils/format-select-options roles :name :name)
        formatted-selection (rs-utils/format-selected-option (map :name (:roles user)) formatted-options)
        user-email (:email user)]
    [:> react-select
     {:value             formatted-selection
      :on-change         (fn [selected]
                           (let [selected-value (map #(.-value %) selected)]
                             (dispatch [:user-list/update-roles user-email selected-value])))
      :is-clearable      true
      :is-multi          true
      :options           formatted-options
      :class-name        "react-select-container"
      :class-name-prefix "react-select"
      :theme             rs-utils/select-theme-fn}]))

(defn user-row
  [user roles current-user]
  [:tr {:key (:uuid user)}
   [:td (:email user)]
   [:td (user-role-select user roles)]
   [:td (user-deactivate-button user (= (:email user) (:email current-user)))]])

(defn user-table
  []
  (let [roles @(subscribe [:user-list/roles])
        users @(subscribe [:user-list/users])
        active-users (filter #(not (:deactivated %)) users)
        current-user @(subscribe [:user])]
    [:table {:width "100%"}
     [:thead
      [:tr
       [:th "Username"]
       [:th "Roles"]
       [:th "Delete"]]]
     [:tbody
      (map #(user-row % roles current-user) active-users)]]))

(defn add-user-form
  []
  (let [new-user-email @(subscribe [:user-list/new-user-email])]
    [:<>
     [:h3 "Add User"]
     [:form {:on-submit (fn [evt]
                          (.preventDefault evt)
                          (dispatch [:user-list/create-new-user]))}
      [:div
       {:style {:display "inline-block"}}
       [:input.form-control
        {:type      "email"
         :required  true
         :value     new-user-email
         :on-change (fn [evt]
                      (let [new-value (-> evt .-target .-value)]
                        (dispatch [:user-list/set-new-user-email new-value])))}]]
      [:div.spacer]
      [:div
       {:style {:display "inline-block"}}
       [:button.btn.btn-secondary
        {:type "submit"} "Add"]]]]))

(defn component
  []
  (fn []
    [:<>
     (user-table)
     (add-user-form)]))
