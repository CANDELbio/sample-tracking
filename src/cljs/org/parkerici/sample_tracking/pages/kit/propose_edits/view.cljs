(ns org.parkerici.sample-tracking.pages.kit.propose-edits.view
  (:require [org.parkerici.sample-tracking.components.header.views :as header]
            [org.parkerici.sample-tracking.components.flash.views :as flash]
            [org.parkerici.sample-tracking.components.kit.propose-edits.view.views :as view-proposed-edit]))

(defn page
  []
  [:div
   [header/component]
   [:div.page-body
    [:h1 "View Proposed Kit Edit"]
    [flash/component]
    [view-proposed-edit/component]]])