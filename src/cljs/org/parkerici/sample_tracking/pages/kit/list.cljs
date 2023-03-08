(ns org.parkerici.sample-tracking.pages.kit.list
  (:require [org.parkerici.sample-tracking.components.kit.list.views :as kit-list]
            [org.parkerici.sample-tracking.components.flash.views :as flash]
            [org.parkerici.sample-tracking.components.header.views :as header]))

(defn page
  []
  [:div
   [header/component]
   [:div.list-body
    [:h1 "Kit Listing"]
    [flash/component]
    [:div.kit-list
     [kit-list/component]]]])