(ns org.parkerici.sample-tracking.components.type.filter.views
  (:require [re-frame.core :refer [dispatch subscribe]]
            [react-select]                                  ;React component https://www.npmjs.com/package/react-select
            [org.parkerici.sample-tracking.components.type.filter.events]
            [org.parkerici.sample-tracking.components.type.filter.subs]
            [org.parkerici.sample-tracking.components.type.filter.db]
            [org.parkerici.sample-tracking.utils.react-select :as rs-utils]
            [org.parkerici.sample-tracking.components.type.filter.utils :as filter-utils]))

(defn select-input
  [selected-values options multi on-change]
  [:div
   {:style {:position "relative"}}
   ; Hidden field so that the select-input is set as required on forms.
   [:input.form-control
    {:tab-index -1
     :style     {:opacity 0 :position "absolute"}
     :value     selected-values
     :on-change nil
     :required  true}]
   [:> react-select
    {:value             selected-values
     :on-change         on-change
     :is-clearable      true
     :is-multi          multi
     :options           options
     :class-name        "react-select-container"
     :class-name-prefix "react-select"
     :theme             rs-utils/select-theme-fn}]])

(defn proto-filter-row
  [label options selected-options selected-type keys-to-clear url-params multi]
  (let [formatted-options (rs-utils/format-select-options options :uuid :name)
        formatted-selection (rs-utils/format-selected-option selected-options formatted-options)]
    [:tr
     [:td [:label label]]
     [:td (select-input formatted-selection formatted-options multi (fn [selected]
                                                                      (doseq [key-to-clear keys-to-clear]
                                                                        (dispatch [:clear-keys key-to-clear]))
                                                                      (if (nil? selected)
                                                                        (dispatch [:clear-keys (filter-utils/selected-type-db-keys selected-type)])
                                                                        (let [selected-value (if multi (map #(.-value %) selected) (.-value selected))]
                                                                          (dispatch [:type-filter/set-selected-type selected-type selected-value url-params])))))]]))

(defn multi-filter-row
  [label options selected-options selected-type keys-to-clear url-params]
  (proto-filter-row label options selected-options selected-type keys-to-clear url-params true))

(defn filter-row
  [label options selected-option selected-type keys-to-clear url-params]
  (proto-filter-row label options [selected-option] selected-type keys-to-clear url-params false))

(defn component-rows
  [& {:keys [url-params keys-to-clear]}]
  (let [{:options/keys [sites studies cohorts timepoints kit-types]} @(subscribe [:type-filter/options])
        {selected-site       :selected-options/site selected-study :selected-options/study selected-cohort :selected-options/cohort
         selected-timepoints :selected-options/timepoints selected-kit-type :selected-options/kit-type} @(subscribe [:type-filter/selected-options])
        study-key :study
        site-key :site
        site-db-keys (filter-utils/selected-type-db-keys site-key)
        site-options-keys (filter-utils/type-db-keys :sites)
        cohort-key :cohort
        cohort-db-keys (filter-utils/selected-type-db-keys cohort-key)
        cohort-options-keys (filter-utils/type-db-keys :cohorts)
        timepoints-key :timepoints
        timepoints-db-keys (filter-utils/selected-type-db-keys timepoints-key)
        timepoint-options-keys (filter-utils/type-db-keys timepoints-key)
        kit-type-key :kit-type
        kit-type-db-keys (filter-utils/selected-type-db-keys kit-type-key)
        kit-type-options-keys (filter-utils/type-db-keys :kit-types)
        selected-form-type-fields-db-keys [:type-filter/type-filter :type-filter/options :options/form-type-fields]
        sample-types-db-keys [:type-filter/type-filter :type-filter/options :options/sample-types]
        clear-on-kit-types-change (concat [timepoints-db-keys timepoint-options-keys selected-form-type-fields-db-keys sample-types-db-keys] (:kit-types keys-to-clear))
        clear-on-cohort-change (conj clear-on-kit-types-change kit-type-db-keys kit-type-options-keys)
        clear-on-site-change (conj clear-on-cohort-change cohort-db-keys cohort-options-keys)
        clear-on-study-change (concat (conj clear-on-site-change site-db-keys site-options-keys) (:study keys-to-clear))]
    [:<>
     (filter-row "Study" studies selected-study study-key clear-on-study-change (merge {} (:study url-params)))
     (filter-row "Site" sites selected-site site-key clear-on-site-change (merge {:study selected-study} (:site url-params)))
     (filter-row "Cohort" cohorts selected-cohort cohort-key clear-on-cohort-change (merge {} (:cohort url-params)))
     (filter-row "Kit Type" kit-types selected-kit-type kit-type-key clear-on-kit-types-change (merge {} (:kit-type url-params)))
     (multi-filter-row "Timepoints" timepoints selected-timepoints timepoints-key [] (merge {:kit-type selected-kit-type} (:timepoints url-params)))]))

(defn component-table
  []
  (fn [& {:keys [url-params keys-to-clear]}]
    [:table {:width "100%"}
     [:tbody
      (component-rows :url-params url-params :keys-to-clear keys-to-clear)]]))
