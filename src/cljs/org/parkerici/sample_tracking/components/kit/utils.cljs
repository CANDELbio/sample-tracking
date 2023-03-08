(ns org.parkerici.sample-tracking.components.kit.utils
  (:require [org.parkerici.sample-tracking.components.type.filter.utils :as tf-utils]
            [re-frame.core :refer [dispatch]]
            [org.parkerici.sample-tracking.utils.str :as str]
            [ajax.core :as ajax]))

(defn- combine-collection-date-and-time
  "Combines the collection-date and time if both are present. Otherwise returns whichever has a non-nil value."
  [collection-date time]
  (let [combined-date (cond
                        (and (some? collection-date) (some? time)) (let [combined-date (js/Date. collection-date)
                                                                         collection-time (js/Date. time)]
                                                                     (.setHours combined-date (.getHours collection-time))
                                                                     (.setMinutes combined-date (.getMinutes collection-time))
                                                                     combined-date)
                        (some? collection-date) collection-date
                        (some? time) time)]
    (when (some? combined-date) combined-date)))

(defn- combine-collection-date-with-time-form-values
  "Combines collection-date with any time storing form-values"
  [collection-date selected-fields field-values]
  (reduce (fn [m field]
            (let [field-id (:field-id field)
                  field-type (:type field)
                  field-value (get field-values field-id)
                  processed-field-value (if (and (some? field-value) (= field-type "time"))
                                          (combine-collection-date-and-time collection-date field-value)
                                          field-value)]
              (if (some? processed-field-value)
                (assoc m field-id field-value)
                m))) {} selected-fields))

(defn kit-form-submit-xhrio
  [db method uri on-success on-error]
  (let [kit-uuid (get-in db [:kit-form/kit-form :kit-form/form-values :form-values/uuid])
        {:selected-options/keys [site study cohort timepoints kit-type]} (get-in db [:type-filter/type-filter :type-filter/selected-options])
        {:form-values/keys [kit-id participant-id air-waybill collection-date collection-time completing-first-name completing-last-name
                            completing-email samples comments form-type-field-values complete pending-edits]} (get-in db [:kit-form/kit-form :kit-form/form-values])
        selected-form-type-fields (tf-utils/selected-form-type-fields db)
        combined-form-type-field-values (combine-collection-date-with-time-form-values collection-date selected-form-type-fields form-type-field-values)
        collection-timestamp (combine-collection-date-and-time collection-date collection-time)
        params (cond-> {:timezone (js/dayjs.tz.guess)}
                       (some? collection-timestamp) (assoc :collection-timestamp collection-timestamp)
                       (some? combined-form-type-field-values) (assoc :form-type-field-values combined-form-type-field-values)
                       (str/not-blank? kit-uuid) (assoc :uuid kit-uuid)
                       (some? kit-type) (assoc :kit-type kit-type)
                       (some? site) (assoc :site site)
                       (some? study) (assoc :study study)
                       (some? cohort) (assoc :cohort cohort)
                       (some? timepoints) (assoc :timepoints timepoints)
                       (str/not-blank? kit-id) (assoc :kit-id kit-id)
                       (str/not-blank? participant-id) (assoc :participant-id participant-id)
                       (str/not-blank? air-waybill) (assoc :air-waybill air-waybill)
                       (str/not-blank? completing-first-name) (assoc :completing-first-name completing-first-name)
                       (str/not-blank? completing-last-name) (assoc :completing-last-name completing-last-name)
                       (str/not-blank? completing-email) (assoc :completing-email completing-email)
                       (str/not-blank? comments) (assoc :comments comments)
                       (str/not-blank? samples) (assoc :samples samples)
                       (str/not-blank? complete) (assoc :complete complete)
                       (not-empty pending-edits) (assoc :pending-edits pending-edits))]
    {:method          method
     :uri             uri
     :timeout         8000
     :params          params
     :format          (ajax/json-request-format)
     :response-format (ajax/json-response-format {:keywords? true})
     :on-success      on-success
     :on-failure      on-error}))

(defn confirm-button
  [message event label]
  [:button.btn.btn-secondary
   {:type     "button"
    :on-click (fn [] (when (js/confirm message)
                       (dispatch event)))}
   label])

(defn cancel-button
  []
  (confirm-button "Are you sure you want to discard your edits?" [:redirect :kit-list] "Cancel"))