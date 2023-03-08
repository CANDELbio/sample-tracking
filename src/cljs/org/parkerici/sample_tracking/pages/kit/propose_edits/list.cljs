(ns org.parkerici.sample-tracking.pages.kit.propose-edits.list
  (:require [org.parkerici.sample-tracking.components.header.views :as header]
            [org.parkerici.sample-tracking.components.flash.views :as flash]
            [org.parkerici.sample-tracking.components.kit.propose-edits.list.views :as list-proposed-edits]))

(defn page
  []
  [:div
   [header/component]
   [:div.list-body
    [:h1 "Proposed Kit Edit Listing"]
    [flash/component]
    [list-proposed-edits/component]]])