(ns org.parkerici.sample-tracking.components.kit.form.events
  (:require [org.parkerici.sample-tracking.events :as events]
            [org.parkerici.sample-tracking.db :as db]
            [re-frame.core :refer [reg-event-db reg-event-fx]]
            [ajax.core :as ajax]
            [org.parkerici.sample-tracking.utils.str :as str]))

(reg-event-fx
  :kit-form/initialize
  events/default-interceptors
  (fn [{:keys [db]}]
    (if (contains? db :kit-form/kit-form)
      {}
      {:dispatch [:kit-form/reset-form]})))

(reg-event-fx
  :kit-form/reset-form
  events/default-interceptors
  (fn [{:keys [db]}]
    {:db (dissoc db :kit-form/kit-form)
     :fx [[:dispatch [:type-filter/initialize {:active true}]]
          [:dispatch-later {:ms 100 :dispatch [:kit-form/set-email-from-user-if-unset]}]]}))

(reg-event-db
  :kit-form/set-email-from-user-if-unset
  (fn [db]
    (let [form-email (get-in db [:kit-form/kit-form :kit-form/form-values :form-values/completing-email])
          user-email (get-in db [::db/user :email])]
      (if (and (not form-email) user-email)
        (assoc-in db [:kit-form/kit-form :kit-form/form-values :form-values/completing-email] user-email)
        db))))

(reg-event-fx
  :kit-form/initialize-with-kit
  events/default-interceptors
  (fn [{:keys [db]} []]
    (cond-> {:dispatch [:kit-form/load-kit]}
            (contains? db :kit-form/kit-form) (assoc :db (dissoc db :kit-form/kit-form)))))

(reg-event-fx
  :kit-form/load-kit
  events/default-interceptors
  (fn [{:keys [db]} []]
    {:http-xhrio {:method          :get
                  :uri             "/api/kit"
                  :url-params      (::db/route-params db)
                  :timeout         8000
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:kit-form/parse-kit-response]
                  :on-failure      [:flash/request-error "Error! Server said: "]}
     :dispatch   [:type-filter/initialize]}))

(reg-event-fx
  :kit-form/set-form-value
  events/default-interceptors
  (fn [{:keys [db]} [db-key value]]
    (let [updated-db (if (some? value)
                       (assoc-in db [:kit-form/kit-form :kit-form/form-values (keyword :form-values db-key)] value)
                       (update-in db [:kit-form/kit-form :kit-form/form-values] dissoc (keyword :form-values db-key)))
          fx {:db updated-db}]
      (case db-key
        :kit-id (assoc fx :dispatch [:kit-form/update-sample-ids value])
        fx))))

(reg-event-db
  :kit-form/mark-all-samples
  (fn [db [_ status-key status-value check-ships-with-kit]]
    (let [sample-types (get-in db [:type-filter/type-filter :type-filter/options :options/sample-types])
          kit-id (get-in db [:kit-form/kit-form :kit-form/form-values :form-values/kit-id])
          samples (get-in db [:kit-form/kit-form :kit-form/form-values :form-values/samples])
          updated-samples (reduce (fn [m sample-type]
                                    (let [sample-ships-with-kit (:ships-with-kit sample-type)
                                          sample-uuid (:uuid sample-type)
                                          new-sample-id (str kit-id (:id-suffix sample-type))
                                          cur-sample (or (get samples sample-uuid) {:sample-id new-sample-id})]
                                      (if (or (not check-ships-with-kit) sample-ships-with-kit)
                                        (assoc m sample-uuid (assoc cur-sample status-key status-value))
                                        (assoc m sample-uuid cur-sample)))) {} sample-types)]
      (assoc-in db [:kit-form/kit-form :kit-form/form-values :form-values/samples] updated-samples))))

(reg-event-db
  :kit-form/mark-sample
  (fn [db [_ sample-type-uuid sample-id status-key status-value]]
    (let [samples (get-in db [:kit-form/kit-form :kit-form/form-values :form-values/samples])
          current-sample (get samples sample-type-uuid)
          updated-sample (-> current-sample
                             (assoc status-key status-value)
                             (assoc :sample-id sample-id))
          updated-samples (assoc samples sample-type-uuid updated-sample)]
      (assoc-in db [:kit-form/kit-form :kit-form/form-values :form-values/samples] updated-samples))))

(reg-event-db
  :kit-form/set-sample-id
  (fn [db [_ sample-type-uuid sample-id]]
    (let [samples (get-in db [:kit-form/kit-form :kit-form/form-values :form-values/samples])
          current-sample (get samples sample-type-uuid)
          updated-sample (-> current-sample
                             (assoc :sample-id sample-id)
                             (assoc :user-edited-id true))
          updated-samples (assoc samples sample-type-uuid updated-sample)]
      (assoc-in db [:kit-form/kit-form :kit-form/form-values :form-values/samples] updated-samples))))

; This updates the samples whose ids are stored in the sample map when kit-id gets updated
; Doesn't update ones that have been edited by the user
(reg-event-db
  :kit-form/update-sample-ids
  (fn [db [_ kit-id]]
    (let [sample-types (get-in db [:type-filter/type-filter :type-filter/options :options/sample-types])
          samples (get-in db [:kit-form/kit-form :kit-form/form-values :form-values/samples])
          updated-samples (reduce (fn [m sample-type]
                                    (let [sample-uuid (:uuid sample-type)
                                          id-suffix (:id-suffix sample-type)
                                          sample (get samples sample-uuid)
                                          updated-sample (assoc sample :sample-id (str kit-id id-suffix))]
                                      (if (nil? sample)
                                        m
                                        (if (:user-edited-id sample)
                                          (assoc m sample-uuid sample)
                                          (assoc m sample-uuid updated-sample)))))
                                  {} sample-types)]
      (assoc-in db [:kit-form/kit-form :kit-form/form-values :form-values/samples] updated-samples))))

(reg-event-db
  :kit-form/set-form-type-field-value
  (fn [db [_ field-id field-value]]
    (assoc-in db [:kit-form/kit-form :kit-form/form-values :form-values/form-type-field-values field-id] field-value)))

(reg-event-fx
  :kit-form/parse-kit-response
  events/default-interceptors
  (fn [{:keys [_db]} [response]]
    (let [kit (first (get-in response [:data :items]))
          {:keys [site-uuid study-uuid cohort-uuid timepoints kit-type-uuid]} kit
          timepoint-uuids (map #(:uuid %) timepoints)]
      {:fx [[:dispatch [:type-filter/set-selected-type :study study-uuid {:active true}]]
            [:dispatch [:type-filter/set-selected-type :site site-uuid {:study study-uuid :active true}]]
            [:dispatch [:type-filter/set-selected-type :cohort cohort-uuid {}]]
            [:dispatch [:type-filter/set-selected-type :timepoints timepoint-uuids {:cohort cohort-uuid :active true}]]
            [:dispatch [:type-filter/set-selected-type :kit-type kit-type-uuid {}]]
            ; Don't know why, but doesn't work unless using dispatch-later
            [:dispatch-later {:ms 100 :dispatch [:kit-form/set-kit-values kit]}]]})))

(defn parse-and-adjust-timestamp
  "Parses the timestamp in the passed in timezone
  Then sets the timezone to the current timezone while keeping the local time the same
  This so that when a user on the east coast submits a time for 1 PM and we edit on the west coast
  we will see 1 PM instead of 10 AM."
  [timestamp timezone]
  (when (and (str/not-blank? timestamp) (str/not-blank? timezone))
    (let [current-timezone (js/dayjs.tz.guess)
          date-time (js/dayjs timestamp)
          zoned-date-time (.tz date-time timezone)
          adjusted-date-time (.tz zoned-date-time current-timezone true)]
      (.toDate adjusted-date-time))))

(defn parse-form-values
  [form-values timezone]
  (when-not (empty? form-values)
    (reduce (fn [m v]
              (let [{:keys [field-type value field-id]} v
                    parsed-value (if (= field-type "time")
                                   (parse-and-adjust-timestamp value timezone)
                                   value)]
                (assoc m field-id parsed-value))) {} form-values)))

(reg-event-db
  :kit-form/set-kit-values
  (fn [db [_ kit-map]]
    (let [namespace :kit-form/kit-form
          {:keys [uuid kit-id participant-id shipments collection-timestamp completing-first-name completing-last-name
                  completing-email comments samples complete form-values timezone archived pending-edits history]} kit-map
          air-waybill (:air-waybill (first shipments))
          collection-date-time (parse-and-adjust-timestamp collection-timestamp timezone)
          parsed-form-values (parse-form-values form-values timezone)]
      (cond-> db
              (str/not-blank? uuid) (assoc-in [namespace :kit-form/form-values :form-values/uuid] uuid)
              (str/not-blank? kit-id) (assoc-in [namespace :kit-form/form-values :form-values/kit-id] kit-id)
              (str/not-blank? participant-id) (assoc-in [namespace :kit-form/form-values :form-values/participant-id] participant-id)
              (str/not-blank? air-waybill) (assoc-in [namespace :kit-form/form-values :form-values/air-waybill] air-waybill)
              (some? collection-date-time) (assoc-in [namespace :kit-form/form-values :form-values/collection-date] collection-date-time)
              (some? collection-date-time) (assoc-in [namespace :kit-form/form-values :form-values/collection-time] collection-date-time)
              (str/not-blank? completing-first-name) (assoc-in [namespace :kit-form/form-values :form-values/completing-first-name] completing-first-name)
              (str/not-blank? completing-last-name) (assoc-in [namespace :kit-form/form-values :form-values/completing-last-name] completing-last-name)
              (str/not-blank? completing-email) (assoc-in [namespace :kit-form/form-values :form-values/completing-email] completing-email)
              (str/not-blank? comments) (assoc-in [namespace :kit-form/form-values :form-values/comments] comments)
              (not (empty? samples)) (assoc-in [namespace :kit-form/form-values :form-values/samples] (reduce (fn [m v] (assoc m (:sample-type-uuid v) v)) {} samples))
              (not (nil? complete)) (assoc-in [namespace :kit-form/form-values :form-values/complete] complete)
              (str/not-blank? timezone) (assoc-in [namespace :kit-form/form-values :form-values/timezone] timezone)
              (some? archived) (assoc-in [namespace :kit-form/form-values :form-values/archived] archived)
              (some? parsed-form-values) (assoc-in [namespace :kit-form/form-values :form-values/form-type-field-values] parsed-form-values)
              (some? pending-edits) (assoc-in [namespace :kit-form/form-values :form-values/pending-edits] pending-edits)
              (some? history) (assoc-in [namespace :kit-form/form-values :form-values/history] history)))))

(reg-event-fx
  :kit-form/check-kit-id
  events/default-interceptors
  (fn [{:keys [_db]} [kit-id]]
    {:http-xhrio {:method          :get
                  :uri             "/api/kit/submitted"
                  :url-params      {:kit-id kit-id}
                  :timeout         8000
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:kit-form/set-kit-id-submitted]
                  :on-failure      [:do-nothing]}}))

(reg-event-db
  :kit-form/set-kit-id-submitted
  (fn [db [_ response]]
    (let [kit (first (get-in response [:data :items]))
          exists (:exists kit)]
      (assoc-in db [:kit-form/kit-form :kit-form/kit-id-submitted] exists))))