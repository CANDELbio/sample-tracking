(ns org.parkerici.sample-tracking.components.type.filter.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [org.parkerici.sample-tracking.events :as events]
            [org.parkerici.sample-tracking.components.type.filter.utils :as filter-utils]
            [ajax.core :as ajax]))

(reg-event-fx
  :type-filter/initialize
  events/default-interceptors
  (fn [{:keys [db]} [studies-url-params]]
    {:db         (dissoc db :type-filter/type-filter)
     :dispatch-n [[:type-filter/get-types "/api/study" studies-url-params [:type-filter/options :options/studies]]]}))

(defn next-options-to-fetch
  [selected-type]
  (case selected-type
    :study [:sites]
    :site [:cohorts]
    :cohort [:kit-types]
    :kit-type [:timepoints :sample-types :form-type-fields]
    []))

(defn option-fetch-uri
  [option]
  (case option
    :sites "/api/site"
    :cohorts "/api/cohort"
    :kit-types "/api/kit-type"
    :timepoints "/api/timepoint"
    :sample-types "/api/sample-type"
    :form-type-fields "/api/form-type-fields"))

; Given a selected type, value, and params, sets the value for the selected type
; Also uses the value to fetch the next set of options for the next form element
; Additional params get merged and used for the fetch request for the next options.
(reg-event-fx
  :type-filter/set-selected-type
  events/default-interceptors
  (fn [{:keys [db]} [selected-type value additional-params]]
    (let [options-to-fetch (next-options-to-fetch selected-type)
          url-params (assoc additional-params selected-type value)
          to-dispatch (map (fn [option]
                             (let [uri (option-fetch-uri option)
                                   success-key [:type-filter/options (keyword :options option)]]
                               [:type-filter/get-types uri url-params success-key])) options-to-fetch)]
      {:db         (assoc-in db (filter-utils/selected-type-db-keys selected-type) value)
       :dispatch-n to-dispatch})
    ))

(reg-event-fx
  :type-filter/get-types
  events/default-interceptors
  (fn [{:keys [_db]} [uri url-params success-key]]
    {:http-xhrio {:method          :get
                  :uri             uri
                  :timeout         15000
                  :url-params      url-params
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:fetch-success :type-filter/type-filter success-key]
                  :on-failure      [:flash/request-error "Error! Server said: "]}}))