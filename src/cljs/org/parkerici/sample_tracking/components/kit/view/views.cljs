(ns org.parkerici.sample-tracking.components.kit.view.views
  (:require [re-frame.core :refer [subscribe]]
            [org.parkerici.sample-tracking.pages.manifest :as manifest-page]
            [org.parkerici.sample-tracking.components.kit.manifest.events]))

(defn component
  []
  (fn [config-map]
    (let [{:keys [site study cohort timepoints kit-type]} @(subscribe [:type-filter/selected-option-values])
          {:form-values/keys [kit-id participant-id air-waybill collection-date collection-time completing-first-name
                              completing-last-name completing-email samples comments complete archived
                              form-type-field-values history]} @(subscribe [:kit-form/form-values])
          most-recent-edit (first history)
          date-display-fn (fn [date] (when (some? date) (.toLocaleDateString date)))
          time-display-fn (fn [time] (when (some? time) (.format (js/dayjs time) "HH:mm")))
          sample-types @(subscribe [:type-filter/sample-types])
          selected-form-type-fields @(subscribe [:type-filter/selected-form-type-fields])
          manifest-map (cond-> {:site-name                 (:name site)
                                :study-name                (:name study)
                                :cohort-name               (:name cohort)
                                :timepoint-names           (map #(:name %) timepoints)
                                :kit-name                  (:name kit-type)
                                :kit-id                    kit-id
                                :participant-id            participant-id
                                :collection-date           collection-date
                                :collection-time           collection-time
                                :selected-form-type-fields selected-form-type-fields
                                :form-type-field-values    form-type-field-values
                                :selected-sample-types     sample-types
                                :sample-values             samples
                                :air-waybill               air-waybill
                                :completing-first-name     completing-first-name
                                :completing-last-name      completing-last-name
                                :completing-email          completing-email
                                :comments                  comments
                                :date-display-fn           date-display-fn
                                :time-display-fn           time-display-fn}
                               (:show-admin-fields config-map) (assoc :edit-email (:agent-email most-recent-edit))
                               (:show-admin-fields config-map) (assoc :edit-timestamp (js/Date. (:time most-recent-edit)))
                               (:show-admin-fields config-map) (assoc :archived archived)
                               (:show-admin-fields config-map) (assoc :complete complete)
                               (:add-signature-fields config-map) (assoc :add-signature-fields true)
                               (:add-empty-field-lines config-map) (assoc :add-empty-field-lines true))]
      (manifest-page/content manifest-map))))