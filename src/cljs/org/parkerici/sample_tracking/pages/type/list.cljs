(ns org.parkerici.sample-tracking.pages.type.list
  (:require [org.parkerici.sample-tracking.components.header.views :as header]
            [org.parkerici.sample-tracking.components.flash.views :as flash]
            [org.parkerici.sample-tracking.components.type.list.views :as type-list]))

(defn page
  []
  [:div
   [header/component]
   [:div.page-body
    [:h1 "Types"]
    [flash/component]
    [:div.type-list-form
     [type-list/component]]]])