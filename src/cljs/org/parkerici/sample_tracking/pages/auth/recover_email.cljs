(ns org.parkerici.sample-tracking.pages.auth.recover-email
  (:require [re-frame.core :refer [subscribe dispatch-sync]]
            [org.parkerici.sample-tracking.components.header.views :as header]
            [org.parkerici.sample-tracking.components.flash.views :as flash]))

(defn success-component
  []
  [:<>
   [:p "Your email was successfully recovered."]
   [:p "We have sent a password reset link to your email in case your password was compromised."]])

(defn failure-component
  []
  [:p "Your email was not recovered. Please contact an administrator for assistance."])

(defn error-component
  []
  [:<>
   [:p "We encountered an error trying to recover your email."]
   [:p "Your email was recovered, but we were unable to send a password reset link."]
   [:p "Please contact an administrator for assistance."]])

(defn page
  []
  (let [status (:status @(subscribe [:route-params]))]
    [:div
     [header/component]
     [:div.page-body
      [:h1 "Recover Email"]
      [flash/component]
      (case status
        "success" (success-component)
        "failure" (failure-component)
        "error" (error-component)
        (dispatch-sync [:redirect :not-found]))]]))