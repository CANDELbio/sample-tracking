(ns org.parkerici.sample-tracking.components.kit.form.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
  :kit-form/form-values
  (fn [db]
    (get-in db [:kit-form/kit-form :kit-form/form-values])))

(defn check-samples-status
  [db status-key check-fn]
  (let [sample-types (get-in db [:type-filter/type-filter :type-filter/options :options/sample-types])
        samples (get-in db [:kit-form/kit-form :kit-form/form-values :form-values/samples])
        statuses (map (fn [sample-type] (get (get samples (:uuid sample-type)) status-key)) sample-types)]
    (check-fn true? statuses)))

(reg-sub
  :kit-form/some-samples-collected
  (fn [db]
    (check-samples-status db :collected some)))

(reg-sub
  :kit-form/some-samples-shipped
  (fn [db]
    (check-samples-status db :shipped some)))
