(ns org.parkerici.sample-tracking.pages.kit.manifest
  (:require [org.parkerici.sample-tracking.components.kit.manifest.views :as kit-manifest]))

(defn page
  []
  [:div.manifest.page-body
   [:div.manifest.header
    [:div.content
     [:img {:src "/images/logo_light.png"}]
     [:div.title
      [:h1 "Kit Shipment Manifest"]]]
    [:div.bars
     [:img {:src "/images/bars.png"}]]]
   [:div.kit-manifest
    [kit-manifest/component]]])