(ns org.parkerici.sample-tracking.components.auth.set-password.events
  (:require [org.parkerici.sample-tracking.events :as events]
            [re-frame.core :refer [reg-event-db reg-event-fx]]))

(reg-event-fx
  :set-password/set-value
  events/default-interceptors
  (fn [{:keys [db]} [key value]]
    {:db       (assoc-in db [:set-password/set-password key] value)
     :dispatch [:set-password/check-password-validity]}))

(defn generate-validity-map
  [new-password confirm-password]
  (let [num-lowercase (count (re-seq #"[a-z]" new-password))
        num-upper-case (count (re-seq #"[A-Z]" new-password))
        num-digits (count (re-seq #"\d" new-password))
        num-non-alphas (count (re-seq #"\W" new-password))]
    {:lowercase-valid  (> num-lowercase 0)
     :uppercase-valid  (> num-upper-case 0)
     :digits-valid     (> num-digits 0)
     :non-alphas-valid (> num-non-alphas 0)
     :length-valid     (>= (count new-password) 8)
     :passwords-match  (= new-password confirm-password)}))

(reg-event-db
  :set-password/check-password-validity
  events/default-interceptors
  (fn [db []]
    (let [new-password (get-in db [:set-password/set-password :set-password/new-password])
          confirm-password (get-in db [:set-password/set-password :set-password/confirm-password])
          validity-map (generate-validity-map new-password confirm-password)]
      (assoc-in db [:set-password/set-password :set-password/validity-map] validity-map))))

(reg-event-db
  :set-password/clear
  events/default-interceptors
  (fn [db []]
    (dissoc db :set-password/set-password)))