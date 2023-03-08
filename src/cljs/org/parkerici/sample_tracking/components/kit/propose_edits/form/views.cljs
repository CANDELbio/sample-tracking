(ns org.parkerici.sample-tracking.components.kit.propose-edits.form.views
  (:require [org.parkerici.sample-tracking.components.kit.form.views :as kit-form]
            [org.parkerici.sample-tracking.components.kit.utils :as kit-utils]
            [org.parkerici.sample-tracking.components.kit.propose-edits.form.events]))

(defn component
  []
  (fn []
    [kit-form/component {:submit-event       [:propose-edits/submit]
                         :submit-button-text "Save Changes"
                         :allow-pending-edits true
                         :extra-buttons      (kit-utils/cancel-button)}]))
