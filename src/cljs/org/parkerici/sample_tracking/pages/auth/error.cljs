(ns org.parkerici.sample-tracking.pages.auth.error
  (:require [org.parkerici.sample-tracking.components.header.views :as header]))

(defn page
  []
  [:div
   [header/component]
   [:div.page-body
    [:h1 "Authentication Error"]
    [:p "An authentication error has occurred. Please try logging in again. If this continues to occur contact an administrator."]]])