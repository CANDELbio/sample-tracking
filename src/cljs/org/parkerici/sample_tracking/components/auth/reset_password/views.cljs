(ns org.parkerici.sample-tracking.components.auth.reset-password.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [org.parkerici.sample-tracking.components.auth.set-password.views :as set-password]))

(defn component
  []
  (fn [oob-code]
    (let [submit-enabled @(subscribe [:set-password/valid-password])]
      [:<>
       [set-password/component]
       [:button.btn.btn-secondary
        {:type     "button"
         :disabled (not submit-enabled)
         :on-click #(dispatch [:auth/reset-password oob-code])}
        "Submit"]])))
