(ns org.parkerici.sample-tracking.pages.auth.not-a-user
  (:require [org.parkerici.sample-tracking.components.header.views :as header]))

(defn page
  []
  [:div
   [header/component]
   [:div.page-body
    [:h1 "Email Not In System"]
    [:p "The email you have logged in with is not in the system. Please contact an administrator to have it added."]]])