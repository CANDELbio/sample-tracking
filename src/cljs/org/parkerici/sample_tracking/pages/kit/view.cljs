(ns org.parkerici.sample-tracking.pages.kit.view
  (:require [org.parkerici.sample-tracking.components.header.views :as header]
            [org.parkerici.sample-tracking.components.kit.view.views :as kit-view]))

(defn page
  []
  [:div
   [header/component]
   [:div.page-body
    [:h1 "View Kit"]
    [kit-view/component {:show-admin-fields true}]]])