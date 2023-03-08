(ns org.parkerici.sample-tracking.components.kit.propose-edits.view.events
  (:require [re-frame.core :refer [reg-event-fx reg-event-db]]
            [ajax.core :as ajax]
            [org.parkerici.sample-tracking.db :as db]
            [org.parkerici.sample-tracking.events :as events]))

(reg-event-fx
  :proposed-edit/initialize
  events/default-interceptors
  (fn [{:keys [db]} []]
    {:http-xhrio {:method          :get
                  :uri             "/api/kit/propose-edit/view"
                  :url-params      (::db/route-params db)
                  :timeout         8000
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:proposed-edit/fetch-success]
                  :on-failure      [:flash/request-error "Error! Server said: "]}}))

(reg-event-db
  :proposed-edit/fetch-success
  (fn [db [_ response]]
    (let [value (first (get-in response [:data :items]))]
      (if (some? value)
        (assoc-in db [:proposed-edit/proposed-edit :proposed-edit/proposed-edit] value)
        db))))

(reg-event-fx
  :proposed-edit/set-status
  events/default-interceptors
  (fn [{:keys [db]} [status]]
    {:http-xhrio {:method          :post
                  :uri             "/api/kit/propose-edit/set-status"
                  :url-params      (assoc (::db/route-params db) :status status)
                  :timeout         8000
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:proposed-edit/set-status-success status]
                  :on-failure      [:flash/request-error "Error! Server said: "]}}))

(reg-event-fx
  :proposed-edit/set-status-success
  events/default-interceptors
  (fn [{:keys [_db]} [status _response]]
    {:dispatch-n [[:flash/request-success (str "Status was successfully updated to " status ".")]
                  [:redirect :proposed-kit-edit-list]]}))