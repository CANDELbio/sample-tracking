(ns org.parkerici.sample-tracking.components.kit.edit.events
  (:require [re-frame.core :refer [reg-event-fx reg-event-db]]
            [ajax.core :as ajax]
            [org.parkerici.sample-tracking.components.kit.utils :as utils]
            [org.parkerici.sample-tracking.events :as events]))

(reg-event-fx
  :kit-edit/submit
  events/default-interceptors
  (fn [{:keys [db]} []]
    {:http-xhrio (utils/kit-form-submit-xhrio db :patch "/api/kit" [:kit-edit/submit-success] [:flash/request-error "Error editing kit! Server said: "])}))

(reg-event-fx
  :kit-edit/submit-success
  events/default-interceptors
  (fn [{:keys [_db]} []]
    {:dispatch-n [[:flash/request-success "Edit success!"]
                  [:redirect :kit-list]]}))
(reg-event-fx
  :kit-edit/set-kit-archived
  events/default-interceptors
  (fn [{:keys [db]} [value]]
    {:http-xhrio {:method          :patch
                  :uri             "/api/kit/set-archived"
                  :url-params      {:uuid     (get-in db [:kit-form/kit-form :kit-form/form-values :form-values/uuid])
                                    :archived value}
                  :timeout         8000
                  :response-format (ajax/json-response-format {:keywords? true})
                  :format          (ajax/json-request-format)
                  :on-success      [:kit-edit/set-kit-archived-success value]
                  :on-failure      [:flash/request-error "Error! Server said: "]}}))

(reg-event-fx
  :kit-edit/set-kit-archived-success
  events/default-interceptors
  (fn [{:keys [db]} [value _response]]
    {:db       (assoc-in db [:kit-form/kit-form :kit-form/form-values :form-values/archived] value)
     :dispatch [:redirect :kit-list]}))