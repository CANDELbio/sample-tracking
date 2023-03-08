(ns org.parkerici.sample-tracking.components.configuration.list.events
  (:require [re-frame.core :refer [reg-event-fx reg-event-db]]
            [org.parkerici.sample-tracking.events :as events]
            [ajax.core :as ajax]))

(reg-event-fx
  :configuration-list/initialize
  events/default-interceptors
  (fn [{:keys [db]} []]
    {:db         (dissoc db :configuration-list/configuration-list)
     :http-xhrio {:method          :get
                  :uri             "/api/configuration"
                  :timeout         15000
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:configuration-list/fetch-success]
                  :on-failure      [:flash/request-error "Error! Server said: "]}}))

(reg-event-db
  :configuration-list/fetch-success
  (fn [db [_ response]]
    (let [value (first (get-in response [:data :items]))]
      (assoc-in db [:configuration-list/configuration-list :configuration-list/configuration-map] value))))