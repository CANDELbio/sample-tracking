(ns org.parkerici.sample-tracking.components.kit.shipment.events
  (:require [re-frame.core :refer [reg-event-fx reg-event-db]]
            [ajax.core :as ajax]
            [org.parkerici.sample-tracking.components.kit.utils :as utils]
            [org.parkerici.sample-tracking.events :as events]
            [org.parkerici.sample-tracking.routes :as routes]
            [org.parkerici.sample-tracking.db :as db]))

(reg-event-fx
  :kit-shipment/submit
  events/default-interceptors
  (fn [{:keys [db]} []]
    {:db         (assoc-in db [:kit-shipment/kit-shipment :kit-shipment/submitting] true)
     :http-xhrio (utils/kit-form-submit-xhrio db :post "/api/kit" [:kit-shipment/on-submit [:kit-shipment/submit-success]]
                                              [:kit-shipment/on-submit [:flash/request-error "Error generating manifest! Server said: "]])}))

(reg-event-fx
  :kit-shipment/on-submit
  events/default-interceptors
  (fn [{:keys [db]} [event response]]
    {:db       (assoc-in db [:kit-shipment/kit-shipment :kit-shipment/submitting] false)
     :dispatch (conj event response)}))

(reg-event-fx
  :kit-shipment/submit-success
  events/default-interceptors
  (fn [{:keys [_db]} [_response]]
    (routes/redirect (routes/path-for :kit-manifest))))

(reg-event-fx
  :kit-shipment/share
  events/default-interceptors
  (fn [{:keys [db]} []]
    (let [kit-uuid (get-in db [:kit-form/kit-form :kit-form/form-values :form-values/uuid])
          method (if (nil? kit-uuid) :post :patch)]
      {:http-xhrio (utils/kit-form-submit-xhrio db method "/api/kit/share" [:kit-shipment/share-success] [:flash/request-error "Error sharing! Server said: "])
       :dispatch   [:kit-shipment/set-value :kit-shipment/share-modal-visible true]})))

(reg-event-db
  :kit-shipment/share-success
  (fn [db [_ response]]
    (assoc-in db [:kit-form/kit-form :kit-form/form-values :form-values/uuid] (get-in response [:data :uuid]))))

(reg-event-db
  :kit-shipment/set-value
  (fn [db [_ key value]]
    (assoc-in db [:kit-shipment/kit-shipment key] value)))

(reg-event-fx
  :kit-shipment/reset-form
  events/default-interceptors
  (fn [{:keys [db]} []]
    (let [kit-uuid (get-in db [:kit-form/kit-form :kit-form/form-values :form-values/uuid])
          updated-db (if (some? kit-uuid)
                       (update-in db [:kit-form/kit-form :kit-form/form-values] dissoc :form-values/uuid)
                       db)]
      {:db       updated-db
       :dispatch [:kit-form/reset-form]})))

(reg-event-fx
  :kit-shipment/initialize-shared-kit
  events/default-interceptors
  (fn [{:keys [db]} []]
    {:db         (assoc-in db [:kit-form/kit-form :kit-form/form-values :form-values/uuid] (:uuid (::db/route-params db)))
     :http-xhrio {:method          :get
                  :uri             "/api/kit/share"
                  :url-params      (::db/route-params db)
                  :timeout         8000
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:kit-form/parse-kit-response]
                  :on-failure      [:flash/request-error "Error! Server said: "]}
     :dispatch   [:type-filter/initialize {:active true}]}))