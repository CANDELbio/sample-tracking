(ns org.parkerici.sample-tracking.pages.auth.reset-password
  (:require [re-frame.core :refer [subscribe]]
            [org.parkerici.sample-tracking.components.header.views :as header]
            [org.parkerici.sample-tracking.components.flash.views :as flash]
            [org.parkerici.sample-tracking.components.auth.reset-password.views :as reset-password]))

(defn page
  []
  (let [oob-code (:oob-code @(subscribe [:route-params]))]
    [:div
     [header/component]
     [:div.page-body
      [:h1 "Reset Password"]
      [flash/component]
      [reset-password/component oob-code]]]))