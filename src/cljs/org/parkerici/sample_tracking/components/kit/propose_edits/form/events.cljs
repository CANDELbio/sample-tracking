(ns org.parkerici.sample-tracking.components.kit.propose-edits.form.events
  (:require [re-frame.core :refer [reg-event-fx]]
            [ajax.core :as ajax]
            [org.parkerici.sample-tracking.db :as db]
            [org.parkerici.sample-tracking.components.kit.utils :as utils]
            [org.parkerici.sample-tracking.events :as events]))

(reg-event-fx
  :propose-edits/initialize
  events/default-interceptors
  (fn [{:keys [db]} []]
    (cond-> {:dispatch [:propose-edits/load-kit]}
            (contains? db :kit-form/kit-form) (assoc :db (dissoc db :kit-form/kit-form)))))

(reg-event-fx
  :propose-edits/load-kit
  events/default-interceptors
  (fn [{:keys [db]} []]
    {:http-xhrio {:method          :get
                  :uri             "/api/kit/propose-edit"
                  :url-params      (::db/route-params db)
                  :timeout         8000
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:kit-form/parse-kit-response]
                  :on-failure      [:flash/request-error "Error! Server said: "]}
     :dispatch   [:type-filter/initialize]}))

(reg-event-fx
  :propose-edits/submit
  events/default-interceptors
  (fn [{:keys [db]} []]
    {:http-xhrio (utils/kit-form-submit-xhrio db :post "/api/kit/propose-edit" [:propose-edits/submit-success] [:flash/request-error "Error proposing edit! Server said: "])}))

(reg-event-fx
  :propose-edits/submit-success
  events/default-interceptors
  (fn [{:keys [_db]} []]
    {:dispatch-n [[:flash/request-success "Proposal submitted!"]
                  [:redirect :kit-list]]}))