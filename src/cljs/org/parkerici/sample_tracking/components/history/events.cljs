(ns org.parkerici.sample-tracking.components.history.events
  (:require [re-frame.core :refer [reg-event-fx reg-event-db]]
            [ajax.core :as ajax]
            [org.parkerici.sample-tracking.events :as events]
            [org.parkerici.sample-tracking.db :as db]))

(reg-event-fx
  :history/initialize
  events/default-interceptors
  (fn [{:keys [db]} []]
    {:http-xhrio {:method          :get
                  :uri             "/api/history"
                  :url-params      (::db/route-params db)
                  :timeout         8000
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:fetch-success :history/history [:history/histories]]
                  :on-failure      [:flash/request-error "Error! Server said: "]}}))