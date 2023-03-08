(ns org.parkerici.sample-tracking.components.kit.propose-edits.list.events
  (:require [re-frame.core :refer [reg-event-fx]]
            [ajax.core :as ajax]
            [org.parkerici.sample-tracking.events :as events]))

(reg-event-fx
  :proposed-edits-list/initialize
  events/default-interceptors
  (fn [{:keys [db]} []]
    {:db       (assoc-in db [:proposed-edits-list/proposed-edits-list :proposed-edits-list/filter] "pending")
     :dispatch [:proposed-edits-list/fetch-edits]}))

(reg-event-fx
  :proposed-edits-list/set-filter
  events/default-interceptors
  (fn [{:keys [db]} [value]]
    {:db       (assoc-in db [:proposed-edits-list/proposed-edits-list :proposed-edits-list/filter] value)
     :dispatch [:proposed-edits-list/fetch-edits]}))

(reg-event-fx
  :proposed-edits-list/fetch-edits
  events/default-interceptors
  (fn [{:keys [db]} []]
    (let [filter (get-in db [:proposed-edits-list/proposed-edits-list :proposed-edits-list/filter])]
      {:http-xhrio {:method          :get
                    :uri             "/api/kit/propose-edit/list"
                    :timeout         8000
                    :url-params      {:status filter}
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [:fetch-success :proposed-edits-list/proposed-edits-list [:proposed-edits-list/proposed-edits]]
                    :on-failure      [:flash/request-error "Error! Server said: "]}})))