(ns org.parkerici.sample-tracking.pages.kit.edit
  (:require [org.parkerici.sample-tracking.components.header.views :as header]
            [org.parkerici.sample-tracking.components.flash.views :as flash]
            [org.parkerici.sample-tracking.components.kit.edit.views :as kit-edit]))

(defn page
  []
  [:div
   [header/component]
   [:div.page-body
    [:h1 "Edit Kit"]
    [flash/component]
    [kit-edit/component]]])