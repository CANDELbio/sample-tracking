(ns org.parkerici.sample-tracking.pages.history
  (:require [org.parkerici.sample-tracking.components.header.views :as header]
            [org.parkerici.sample-tracking.components.flash.views :as flash]
            [org.parkerici.sample-tracking.components.history.views :as history]))

(defn page
  []
  [:div
   [header/component]
   [:div.list-body
    [:h1 "History"]
    [flash/component]
    [:div.history
     [history/component]]]])