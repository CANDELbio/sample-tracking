(ns org.parkerici.sample-tracking.pages.type.upload
  (:require [re-frame.core :refer [subscribe]]
            [modal]                                         ;React component https://www.npmjs.com/package/reactstrap
            [modal-header]                                  ;React component https://www.npmjs.com/package/reactstrap
            [modal-body]                                    ;React component https://www.npmjs.com/package/reactstrap
            [org.parkerici.sample-tracking.components.header.views :as header]
            [org.parkerici.sample-tracking.components.flash.views :as flash]
            [org.parkerici.sample-tracking.components.type.upload.views :as upload]))

(defn uploading-modal
  []
  (let [uploading @(subscribe [:type-upload/uploading])]
    [:> modal
     {:is-open uploading}
     [:> modal-header "Types are Uploading"]
     [:> modal-body "Please wait..."]]))

(defn page
  []
  [:div
   [header/component]
   (uploading-modal)
   [:div.page-body
    [:h1 "Upload Types"]
    [flash/component]
    [:h2 "Upload Kit Types"]
    [:div.upload-kit-types-form
     [upload/component "kit-type"]]
    [:h2 "Upload Sites"]
    [:div.upload-sites-form
     [upload/component "site"]]
    [:h2 "Upload Studies"]
    [:div.upload-studies-form
     [upload/component "study"]]
    [:h2 "Upload Form Types"]
    [:div.upload-form-types-form
     [upload/component "form-type"]]]])