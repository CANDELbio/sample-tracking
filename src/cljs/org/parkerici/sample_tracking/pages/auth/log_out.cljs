(ns org.parkerici.sample-tracking.pages.auth.log-out
  (:require [org.parkerici.sample-tracking.components.header.views :as header]
            [org.parkerici.sample-tracking.routes :as routes]))

(defn page
  []
  [:div
   [header/component]
   [:div.page-body
    [:h1 "Log Out"]
    [:p "You have been logged out. If this was an accident you can  " [:a {:href (routes/path-for :log-in)} "log in"] " again."]]])