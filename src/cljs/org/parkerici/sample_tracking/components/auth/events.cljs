(ns org.parkerici.sample-tracking.components.auth.events
  (:require [re-frame.core :refer [reg-event-fx dispatch-sync]]
            [org.parkerici.sample-tracking.events :as events]
            [org.parkerici.sample-tracking.db :as db]
            [ajax.core :as ajax]))

(reg-event-fx
  :auth/log-in
  events/default-interceptors
  (fn [{:keys [_db]} [firebase-jwt]]
    {:http-xhrio {:method          :post
                  :uri             "/api/log-in"
                  :timeout         15000
                  :params          {:firebase-jwt firebase-jwt}
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:initialize-user-info]
                  :on-failure      [:do-nothing]}}))

(reg-event-fx
  :auth/log-out
  events/default-interceptors
  (fn [{:keys [_db]} []]
    (.signOut (.auth js/firebase))
    {:dispatch   [:clear-user-info]
     :http-xhrio {:method          :post
                  :uri             "/api/log-out"
                  :timeout         15000
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:redirect :log-out]
                  :on-failure      [:do-nothing]}}))

(reg-event-fx
  :auth/email-handler
  events/default-interceptors
  (fn [{:keys [db]} []]
    (let [query-params (::db/query-params db)
          {:keys [mode oobCode]} query-params]
      (case mode
        "resetPassword" {:dispatch [:redirect :reset-password {:oob-code oobCode}]}
        "recoverEmail" {:dispatch [:auth/recover-email oobCode]}
        "verifyEmail" {:dispatch [:auth/verify-email oobCode]}
        {:dispatch [:redirect :not-found]}))))

(reg-event-fx
  :auth/verify-email
  events/default-interceptors
  (fn [{:keys [_db]} [action-code]]
    (-> (.applyActionCode (.auth js/firebase) action-code)
        (.then #(dispatch-sync [:auth/verify-email-success]))
        (.catch #(dispatch-sync [:redirect :verify-email {:status "error"}])))))

(reg-event-fx
  :auth/verify-email-success
  events/default-interceptors
  (fn [{:keys [_db]} []]
    (.signOut (.auth js/firebase))
    {:dispatch   [:clear-user-info]
     :http-xhrio {:method          :post
                  :uri             "/api/log-out"
                  :timeout         15000
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:redirect :verify-email {:status "success"}]
                  :on-failure      [:do-nothing]}}))

; TODO - Store the restored email in the db and allow the user to request a password reset separately.
(reg-event-fx
  :auth/recover-email
  events/default-interceptors
  (fn [{:keys [_db]} [action-code]]
    (let [restored-email (atom "")]
      (-> (.checkActionCode (.auth js/firebase) action-code)
          (.then (fn [info]
                   (reset! restored-email (.. info -data -email))
                   (.applyActionCode (.auth js/firebase) action-code)))
          (.catch #(dispatch-sync [:redirect :recover-email {:status "failure"}]))
          (.then (fn []
                   (-> (.sendPasswordResetEmail (.auth js/firebase) @restored-email)
                       (.then #(dispatch-sync [:redirect :recover-email {:status "success"}]))
                       (.catch #(dispatch-sync [:redirect :recover-email {:status "error"}])))))))))

(reg-event-fx
  :auth/reset-password
  events/default-interceptors
  (fn [{:keys [db]} [action-code]]
    (let [new-password (get-in db [:set-password/set-password :set-password/new-password])]
      (-> (.verifyPasswordResetCode (.auth js/firebase) action-code)
          (.then (fn []
                   (-> (.confirmPasswordReset (.auth js/firebase) action-code new-password)
                       (.then #(dispatch-sync [:auth/reset-password-success]))
                       (.catch #(dispatch-sync [:flash/request-error "Error resetting password."])))))
          (.catch #(dispatch-sync [:flash/request-error "Error: Password reset link has expired."]))))))

(reg-event-fx
  :auth/reset-password-success
  events/default-interceptors
  (fn [{:keys [_db]} []]
    {:dispatch-n [[:set-password/clear]
                  [:flash/request-success "Password successfully reset! Please log in with your new password."]
                  [:redirect :log-in]]}))