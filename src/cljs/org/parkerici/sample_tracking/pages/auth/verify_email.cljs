(ns org.parkerici.sample-tracking.pages.auth.verify-email
  (:require [re-frame.core :refer [subscribe]]
            [org.parkerici.sample-tracking.components.auth.verify-email-result.views :as verify-results]
            [org.parkerici.sample-tracking.components.header.views :as header]))

(defn page
  []
  (let [status (:status @(subscribe [:route-params]))]
    [:div
     [header/component]
     [:div.page-body
      [verify-results/component status]]]))