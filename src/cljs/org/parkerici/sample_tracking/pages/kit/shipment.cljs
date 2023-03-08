(ns org.parkerici.sample-tracking.pages.kit.shipment
  (:require [org.parkerici.sample-tracking.components.header.views :as header]
            [org.parkerici.sample-tracking.components.flash.views :as flash]
            [org.parkerici.sample-tracking.components.kit.shipment.views :as kit-shipment]))

(defn page
  []
  [:div
   [header/component]
   [:div.page-body
    [:h1 "Kit Shipment Form"]
    [flash/component]
    [:div.kit-shipment
     [kit-shipment/component]]]])