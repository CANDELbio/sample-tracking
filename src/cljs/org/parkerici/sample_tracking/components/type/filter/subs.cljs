(ns org.parkerici.sample-tracking.components.type.filter.subs
  (:require [re-frame.core :refer [reg-sub]]
            [org.parkerici.sample-tracking.utils.collection :as coll]))

(reg-sub
  :type-filter/options
  (fn [db]
    (get-in db [:type-filter/type-filter :type-filter/options])))

(reg-sub
  :type-filter/selected-options
  (fn [db]
    (get-in db [:type-filter/type-filter :type-filter/selected-options])))

(reg-sub
  :type-filter/selected-option-values
  (fn [db]
    (let [{:options/keys [sites studies cohorts timepoints kit-types]} (get-in db [:type-filter/type-filter :type-filter/options])
          {selected-site       :selected-options/site selected-study :selected-options/study selected-cohort :selected-options/cohort
           selected-timepoints :selected-options/timepoints selected-kit-type :selected-options/kit-type} (get-in db [:type-filter/type-filter :type-filter/selected-options])]
      {:site       (first (filter #(= selected-site (:uuid %)) sites))
       :study      (first (filter #(= selected-study (:uuid %)) studies))
       :cohort     (first (filter #(= selected-cohort (:uuid %)) cohorts))
       :timepoints (filter #(coll/in? selected-timepoints (:uuid %)) timepoints)
       :kit-type   (first (filter #(= selected-kit-type (:uuid %)) kit-types))
       })))

(reg-sub
  :type-filter/sample-types
  (fn [db]
    (get-in db [:type-filter/type-filter :type-filter/options :options/sample-types])))

(reg-sub
  :type-filter/selected-form-type-fields
  (fn [db]
    (get-in db [:type-filter/type-filter :type-filter/options :options/form-type-fields])))