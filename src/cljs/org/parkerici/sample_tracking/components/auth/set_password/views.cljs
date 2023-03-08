(ns org.parkerici.sample-tracking.components.auth.set-password.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [org.parkerici.sample-tracking.components.auth.set-password.events]
            [org.parkerici.sample-tracking.components.auth.set-password.subs]
            [org.parkerici.sample-tracking.components.auth.set-password.db]))

(defn on-input-change
  [field]
  (fn [evt]
    (let [new-value (-> evt .-target .-value)]
      (dispatch [:set-password/set-value field new-value]))))

(defn requirement-style
  [valid]
  (if-not valid
    {:style {:color "white"}}
    {:style {:color "green"}}))

(defn password-requirements
  [validity-map]
  (let [{:keys [lowercase-valid uppercase-valid digits-valid
                non-alphas-valid length-valid passwords-match]} validity-map]
    [:<>
     [:p "Your new password must meet the following requirements:"]
     [:p (requirement-style length-valid) "At least 8 characters long"]
     [:p (requirement-style uppercase-valid) "At least one upper case letter."]
     [:p (requirement-style lowercase-valid) "At least one lower case letter."]
     [:p (requirement-style digits-valid) "At least one number."]
     [:p (requirement-style non-alphas-valid) "At least one special character (e.g. !, #, &, %, *, @)."]
     [:p (requirement-style passwords-match) "Passwords-match"]]))

(defn component
  []
  (fn []
    (let [new-password-value @(subscribe [:set-password/new-password])
          confirm-password-value @(subscribe [:set-password/confirm-password])
          validity-map @(subscribe [:set-password/validity-map])]
      [:<>
       [:table {:width "100%"}
        [:tbody
         [:tr
          [:td [:label "New Password"]]
          [:td [:input.form-control {:type      "password"
                                     :value     new-password-value
                                     :on-change (on-input-change :set-password/new-password)}]]]
         [:tr
          [:td [:label "Confirm New Password"]]
          [:td [:input.form-control {:type      "password"
                                     :value     confirm-password-value
                                     :on-change (on-input-change :set-password/confirm-password)}]]]]]
       (password-requirements validity-map)])))
