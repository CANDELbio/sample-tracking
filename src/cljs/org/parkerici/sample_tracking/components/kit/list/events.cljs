(ns org.parkerici.sample-tracking.components.kit.list.events
  (:require [re-frame.core :refer [reg-event-fx]]
            [ajax.core :as ajax]
            [org.parkerici.sample-tracking.events :as events]))

(reg-event-fx
  :kit-list/initialize
  events/default-interceptors
  (fn [{:keys [db]} []]
    {:db       (assoc-in db [:kit-list/kit-list :kit-list/filter] "complete")
     :dispatch [:kit-list/fetch-kits]}))


(reg-event-fx
  :kit-list/set-kit-filter
  events/default-interceptors
  (fn [{:keys [db]} [value]]
    {:db       (assoc-in db [:kit-list/kit-list :kit-list/filter] value)
     :dispatch [:kit-list/fetch-kits]}))

(reg-event-fx
  :kit-list/fetch-kits
  events/default-interceptors
  (fn [{:keys [db]} []]
    (let [filter (get-in db [:kit-list/kit-list :kit-list/filter])
          params (case filter
                   "complete" {:complete true :archived false}
                   "incomplete" {:complete false :archived false}
                   "archived" {:archived true}
                   "all" {})]
      {:http-xhrio {:method          :get
                    :uri             "/api/kit"
                    :url-params      params
                    :timeout         8000
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [:fetch-success :kit-list/kit-list [:kit-list/kits]]
                    :on-failure      [:flash/request-error "Error! Server said: "]}})))
