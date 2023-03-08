(ns org.parkerici.sample-tracking.pages.auth.unauthorized
  (:require [org.parkerici.sample-tracking.components.header.views :as header]))

(defn page
  []
  [:div
   [header/component]
   [:div.page-body
    [:h1 "Unauthorized"]
    [:p "You are not authorized to view this page. Please contact an administrator if you believe you are seeing this in error."]]])