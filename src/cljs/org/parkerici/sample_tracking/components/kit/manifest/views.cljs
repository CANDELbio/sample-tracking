(ns org.parkerici.sample-tracking.components.kit.manifest.views
  (:require [re-frame.core :refer [dispatch-sync]]
            [org.parkerici.sample-tracking.components.kit.view.views :as kit-view]
            [org.parkerici.sample-tracking.utils.js :as js-utils]
            [org.parkerici.sample-tracking.components.kit.manifest.events]))

(defn component
  []
  (fn []
    ; Hacky method to set the background color of the manifest page to white.
    ; Use this because we're changing the color of the body element, which is set to black for all other pages and is
    ; outside of the control of React
    (js-utils/set-body-bg-color "white")
    [:div
     [kit-view/component {:add-empty-field-lines true
                          :add-signature-fields true}]
     [:button.btn.btn-secondary..d-print-none
      {:on-click (fn [] (. js/window print))}
      "Print Page"]
     [:div.spacer]
     [:button.btn.btn-secondary..d-print-none
      {:on-click (fn []
                   (js-utils/set-body-bg-color "black")
                   (dispatch-sync [:kit-manifest/clear-fields-and-redirect]))}
      "Complete Another Form"]]))