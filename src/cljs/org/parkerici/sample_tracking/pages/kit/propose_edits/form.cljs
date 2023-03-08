(ns org.parkerici.sample-tracking.pages.kit.propose-edits.form
  (:require [org.parkerici.sample-tracking.components.header.views :as header]
            [org.parkerici.sample-tracking.components.flash.views :as flash]
            [org.parkerici.sample-tracking.components.kit.propose-edits.form.views :as propose-edits]))

(defn page
  []
  [:div
   [header/component]
   [:div.page-body
    [:h1 "Propose Kit Edits"]
    [flash/component]
    [propose-edits/component]]])