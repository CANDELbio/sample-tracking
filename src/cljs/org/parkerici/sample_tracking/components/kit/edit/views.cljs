(ns org.parkerici.sample-tracking.components.kit.edit.views
  (:require [re-frame.core :refer [subscribe]]
            [org.parkerici.sample-tracking.components.kit.form.views :as kit-form]
            [org.parkerici.sample-tracking.components.kit.utils :as kit-utils]
            [org.parkerici.sample-tracking.components.kit.edit.events]))

(defn extra-buttons
  []
  [:<>
   (kit-utils/cancel-button)
   [:div.spacer]
   (if (:form-values/archived @(subscribe [:kit-form/form-values]))
     (kit-utils/confirm-button "Are you sure you want to unarchive this kit?" [:kit-edit/set-kit-archived false] "Unarchive Kit")
     (kit-utils/confirm-button "Are you sure you want to archive this kit?" [:kit-edit/set-kit-archived true] "Archive Kit"))])

(defn component
  []
  (fn []
    [kit-form/component {:submit-event       [:kit-edit/submit]
                         :submit-button-text "Save Changes"
                         :show-admin-fields  true
                         :extra-buttons      (extra-buttons)}]))