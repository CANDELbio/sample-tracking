(ns org.parkerici.sample-tracking.components.auth.verify-email-result.views
  (:require [org.parkerici.sample-tracking.routes :as routes]
            [re-frame.core :refer [subscribe dispatch-sync]]))

; User with unverified email components below
(defn resend-verification-fn
  []
  (fn []
    ((try
       (.sendEmailVerification (. (.auth js/firebase) -currentUser))
       (. js/window alert "Verification email has been resent. Please check your spam folder if you are still unable to find it.")
       (catch :default _e
         (. js/window alert "There was an error sending a verification email. Please contact an administrator."))
       (finally
         (.remove (.getElementById js/document "resend-el")))))))

(defn verification-error-component
  []
  [:div
   [:p "There was an error verifying your email."]
   [:p {:id "resend-el"} "Please click the following link to " [:a {:on-click (resend-verification-fn) :id "resend-link"} "send another verification email"] "."]])

(defn unverified-component
  []
  [:div
   [:p "Your email address has not been verified. Please check your email for a verification link and try again."]
   [:p {:id "resend-el"} "If you cannot find the verification email please click the following link to " [:a {:on-click (resend-verification-fn) :id "resend-link"} "resend the verification email"] "."]])

(defn failure-component
  [status is-a-user email-verified]
  (when (and is-a-user email-verified)
    (dispatch-sync [:redirect :console]))
  [:<>
   [:h1 "Email Not Verified"]
   (case status
     "error" (verification-error-component)
     "unverified" (unverified-component)
     (dispatch-sync [:redirect :not-found]))])

; Verified component parts below.
(defn logged-out-verified
  []
  [:p "You may now " [:a {:href (routes/path-for :log-in)} "log in"] " and use the application."])

(defn logged-in-verified
  []
  [:p "Please " [:a {:href (routes/path-for :log-out)} "log out"] " and log back in to use the application"])

(defn success-component
  [email-verified]
  [:<>
   [:h1 "Email Verified"]
   [:p "Your email was successfully verified."]
   (if (some? email-verified)
     (logged-in-verified)
     (logged-out-verified))])

(defn component
  [status]
  (fn []
    (let [user @(subscribe [:user])
          {:keys [is-a-user email-verified]} user]
      (if (= status "success")
        (success-component email-verified)
        (failure-component status is-a-user email-verified)))))