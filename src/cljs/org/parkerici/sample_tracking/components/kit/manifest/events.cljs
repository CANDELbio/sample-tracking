(ns org.parkerici.sample-tracking.components.kit.manifest.events
  (:require [re-frame.core :refer [reg-event-fx]]
            [org.parkerici.sample-tracking.events :as events]
            [org.parkerici.sample-tracking.components.type.filter.utils :as filter-utils]))

; Clears fields in preparation for completing another form.
(reg-event-fx
  :kit-manifest/clear-fields-and-redirect
  events/default-interceptors
  (fn [{:keys [_db]} []]
    {:fx [[:dispatch [:clear-keys (filter-utils/selected-type-db-keys :kit-type)]]
          [:dispatch [:clear-keys [:type-filter/type-filter :type-filter/options :options/form-type-fields]]]
          [:dispatch [:clear-keys (filter-utils/selected-type-db-keys :timepoints)]]
          [:dispatch [:clear-keys [:type-filter/type-filter :type-filter/options :options/sample-types]]]
          [:dispatch [:clear-keys [:kit-form/kit-form :kit-form/form-values :form-values/samples]]]
          [:dispatch [:clear-keys [:kit-form/kit-form :kit-form/form-values :form-values/air-waybill]]]
          [:dispatch [:clear-keys [:kit-form/kit-form :kit-form/form-values :form-values/form-type-field-values]]]
          [:dispatch [:clear-keys [:kit-form/kit-form :kit-form/form-values :form-values/kit-id]]]
          [:dispatch [:clear-keys [:kit-form/kit-form :kit-form/form-values :form-values/uuid]]]
          [:dispatch [:clear-keys [:kit-form/kit-form :kit-form/form-values :form-values/comments]]]
          [:dispatch [:clear-keys [:kit-form/kit-form :kit-form/kit-id-submitted]]]
          [:dispatch [:redirect :kit-shipment]]]}))