(ns org.parkerici.sample-tracking.pages.blank
  (:require [org.parkerici.sample-tracking.components.header.views :as header]))

(defn page
  []
  [:div
   [header/component]])