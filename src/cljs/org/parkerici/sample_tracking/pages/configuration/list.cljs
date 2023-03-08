(ns org.parkerici.sample-tracking.pages.configuration.list
  (:require [org.parkerici.sample-tracking.components.configuration.list.views :as config-list]
            [org.parkerici.sample-tracking.components.flash.views :as flash]
            [org.parkerici.sample-tracking.components.header.views :as header]))

(defn page
  []
  [:div
   [header/component]
   [:div.list-body
    [:h1 "Configuration List"]
    [flash/component]
    [:div.user-list
     [config-list/component]]]])