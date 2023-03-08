(ns org.parkerici.sample-tracking.components.type.list.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [edi-text]                                      ;React component https://www.npmjs.com/package/react-editext
            [checkbox]                                      ;React component https://www.npmjs.com/package/react-input-checkbox
            [clojure.string :as str]
            [org.parkerici.sample-tracking.routes :as routes]
            [org.parkerici.sample-tracking.components.type.filter.views :as type-filter]
            [org.parkerici.sample-tracking.components.type.list.events]))

(defn edi-text-input
  [input-type type-key db-id field value]
  [:> edi-text
   {:type        input-type
    :input-props {:class-name "form-control"}
    :value       value
    :on-save     (fn [new-value]
                   (dispatch [:type-list/update-type type-key db-id field new-value]))}])

(defn edit-boolean-input
  [type-key db-id field value]
  [:> checkbox
   {:children  ""
    :theme     "bootstrap-checkbox"
    :value     value
    :on-change (fn []
                 (dispatch [:type-list/update-type type-key db-id field (not value)]))}])

(defn edit-input
  [type-key db-id field value]
  (cond
    (boolean? value) (edit-boolean-input type-key db-id field value)
    (string? value) (edi-text-input "text" type-key db-id field value)
    (number? value) (edi-text-input "number" type-key db-id field (str value)))
  )

(defn active-status-input
  [db-key id active params]
  [:> checkbox
   {:children  ""
    :theme     "bootstrap-checkbox"
    :value     active
    :on-change (fn []
                 (dispatch [:type-list/update-active-status db-key id (not active) (merge params {:active (not active)})]))}])

(defn edit-row
  [label type-key value active-post-params]
  (when value
    (let [uuid (get value :uuid)]
      [:tr {:key uuid}
       [:td [:label label]]
       [:td (edit-input type-key uuid :name (:name value))]
       [:td (when (some? (:active value)) (active-status-input type-key uuid (:active value) active-post-params))]
       [:td (:create-time value)]
       [:td [:a {:href (routes/path-for :entity-history :uuid uuid)} "Link"]]])))

(defn sample-table-row
  [sample fields]
  (let [uuid (get sample :uuid)]
    [:tr {:key uuid}
     (for [field fields]
       (let [field-value (get sample field)]
         [:td {:key (str uuid field-value)} (edit-input :sample-types uuid field field-value)]))
     [:td [:a {:href (routes/path-for :entity-history :uuid uuid)} "Link"]]]))

; TODO - Use table building functions from org.parkerici.sample-tracking.utils.table
(defn sample-table
  [samples]
  (let [fields [:name :id-suffix :ships-with-kit :reminder]]
    [:table {:width "100%"}
     [:thead
      [:tr
       (for [field fields]
         [:th {:key field} (str/capitalize (name field))])
       [:th "History"]]]
     [:tbody
      (map #(sample-table-row % fields) samples)]]))

; TODO - Maybe try to use table building functions from org.parkerici.sample-tracking.utils.table?
(defn edit-table
  []
  (let [{:keys [site study cohort timepoints kit-type]} @(subscribe [:type-filter/selected-option-values])
        sample-types @(subscribe [:type-filter/sample-types])
        study-uuid (:uuid study)
        site-uuid (:uuid site)
        cohort-uuid (:uuid cohort)
        kit-type-uuid (:uuid kit-type)]
    [:div
     [:table {:width "100%"}
      [:thead
       [:tr
        [:th "Field"]
        [:th "Name"]
        [:th "Active"]
        [:th "Create Time"]
        [:th "History"]]]
      [:tbody
       (edit-row "Study" :studies study {:study study-uuid})
       (edit-row "Site" :sites site {:study study-uuid :site site-uuid})
       (edit-row "Cohort" :cohorts cohort {:study study-uuid :cohort cohort-uuid})
       (map #(edit-row "Timepoint" :timepoints % nil) timepoints)
       (edit-row "Kit Type" :kit-types kit-type {:cohort cohort-uuid :kit-type kit-type-uuid})]]
     (when (some? kit-type)
       [:table {:width "100%"}
        [:tbody
         [:tr
          [:td [:label "Kit Type Item Number"]]
          [:td (edit-input :kit-types kit-type-uuid :item-number (:item-number kit-type))]]
         [:tr
          [:td [:label "Kit Type Collection Date Required"]]
          [:td (edit-boolean-input :kit-types kit-type-uuid :collection-date-required (:collection-date-required kit-type))]]
         [:tr
          [:td [:label "Kit Type Air Waybill Required"]]
          [:td (edit-boolean-input :kit-types kit-type-uuid :air-waybill-required (:air-waybill-required kit-type))]]]])
     (when (some? sample-types)
       [:table {:width "100%"}
        [:tbodys
         [:tr [:td [:label "Samples"]] [:td (sample-table sample-types)]]]])]))

(defn component
  []
  (fn []
    [:div
     [:h2 "Select Types"]
     [type-filter/component-table]
     [:h2 "Edit Types"]
     (edit-table)]))