(ns org.parkerici.sample-tracking.pages.user.list
  (:require [org.parkerici.sample-tracking.components.user.list.views :as user-list]
            [org.parkerici.sample-tracking.components.flash.views :as flash]
            [org.parkerici.sample-tracking.components.header.views :as header]))

(defn page
  []
  [:div
   [header/component]
   [:div.page-body
    [:h1 "User Listing"]
    [flash/component]
    [:div.user-list
     [user-list/component]]]])