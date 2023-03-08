(ns org.parkerici.sample-tracking.pages.auth.log-in
  (:require [re-frame.core :refer [subscribe]]
            [org.parkerici.sample-tracking.components.header.views :as header]
            [org.parkerici.sample-tracking.components.auth.log-in.views :as log-in]
            [org.parkerici.sample-tracking.components.flash.views :as flash]))

(defn page
  []
  (let [firebase-initialized @(subscribe [:firebase-initialized])]
    [:div
     [header/component]
     [:div.page-body
      [:h1 "Log In or Create a New Account"]
      [flash/component]
      (when firebase-initialized
        [log-in/component])]]))