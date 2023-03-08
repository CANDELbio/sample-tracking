(ns org.parkerici.sample-tracking.components.flash.events
  (:require [re-frame.core :refer [reg-event-db]]
            [org.parkerici.sample-tracking.events :as events]))

(reg-event-db
  :flash/set-flash
  events/default-interceptors
  (fn [db [message priority]]
    (assoc db :flash/flash {:flash/message  message
                            :flash/priority priority})))

(reg-event-db
  :flash/clear-flash
  events/default-interceptors
  (fn [db _]
    (dissoc db :flash/flash)))

(reg-event-db
  :flash/request-success
  (fn [db [_ message _response]]
    (assoc db :flash/flash
              {:flash/priority "success"
               :flash/message  message})))

(reg-event-db
  :flash/request-error
  (fn [db [_ message response]]
    (assoc db :flash/flash
              {:flash/priority "danger"
               :flash/message  (str message response)})))