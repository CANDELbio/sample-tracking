(ns org.parkerici.sample-tracking.pages.not-found
  (:require [org.parkerici.sample-tracking.components.header.views :as header]))

(defn page
  []
  [:div
   [header/component]
   [:div.page-body 
    [:h1 "Not Found"]]])