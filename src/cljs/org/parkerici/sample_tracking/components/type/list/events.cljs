(ns org.parkerici.sample-tracking.components.type.list.events
  (:require [re-frame.core :refer [reg-event-fx]]
            [org.parkerici.sample-tracking.events :as events]
            [org.parkerici.sample-tracking.components.type.filter.utils :as filter-utils]
            [ajax.core :as ajax]))

(reg-event-fx
  :type-list/initialize
  events/default-interceptors
  (fn [_db]
    {:dispatch [:type-filter/initialize]}))

(defn get-endpoint
  [db-key]
  (case db-key
    :studies "/api/study"
    :sites "/api/site"
    :cohorts "/api/cohort"
    :timepoints "/api/timepoint"
    :kit-types "/api/kit-type"
    :sample-types "/api/sample-type"))

; Not sure if :do-nothing on-success is the "right way to do this"
; Instead should I post the update to the server, have the server return the updated value, and then replace that in the db?
(reg-event-fx
  :type-list/update-type
  events/default-interceptors
  (fn [{:keys [db]} [type-key id field value]]
    (let [current-type-db-keys (filter-utils/type-db-keys type-key)
          current-type-options (get-in db current-type-db-keys)
          updated-db (assoc-in db current-type-db-keys (into [] (map (fn [val]
                                                                       (if (= (get val :uuid) id)
                                                                         (assoc val field value)
                                                                         val))
                                                                     current-type-options)))
          updated-type (first (filter #(= (get % :uuid) id) (get-in updated-db current-type-db-keys)))]
      {:db         updated-db
       :http-xhrio {:method          :post
                    :uri             (get-endpoint type-key)
                    :timeout         15000
                    :params          updated-type
                    :format          (ajax/json-request-format)
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [:do-nothing]
                    :on-failure      [:flash/request-error "Error! Server said: "]}})))

(reg-event-fx
  :type-list/update-active-status
  events/default-interceptors
  (fn [{:keys [db]} [type-key id active params]]
    (let [current-type-db-keys (filter-utils/type-db-keys type-key)
          updated-db (assoc-in db current-type-db-keys (into [] (map (fn [val]
                                                                       (if (= (get val :uuid) id)
                                                                         (assoc val :active active)
                                                                         val))
                                                                     (get-in db current-type-db-keys))))]
      {:db         updated-db
       :http-xhrio {:method          :post
                    :uri             "/api/set-active"
                    :timeout         15000
                    :params          params
                    :format          (ajax/json-request-format)
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [:do-nothing]
                    :on-failure      [:flash/request-error "Error! Server said: "]}})))