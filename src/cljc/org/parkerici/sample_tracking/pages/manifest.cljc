(ns org.parkerici.sample-tracking.pages.manifest
  (:require
    [clojure.string :as string]))

(def highlight-attrs {:class "alert-danger"})

(defn simple-row
  ([label field]
   (simple-row label field false))
  ([label field highlight]
   (let [row-attrs (if highlight highlight-attrs {})]
     [:tr row-attrs [:td [:label label]] [:td field]])))

(defn boolean-display
  [value]
  (if value
    "[X]"
    "[ ]"))

(defn sample-table-row
  [kit-id sample-values highlighted-samples sample-type key-prefix]
  (let [sample-type-uuid (:uuid sample-type)
        sample-value (or (get sample-values sample-type-uuid) (get sample-values (keyword (str sample-type-uuid))))
        highlight-sample (or (get highlighted-samples sample-type-uuid) (get highlighted-samples (keyword (str sample-type-uuid))))
        sample-id (or (:sample-id sample-value) (str kit-id (:id-suffix sample-type)))
        collected (:collected sample-value)
        shipped (:shipped sample-value)
        base-row-attrs {:key (str key-prefix sample-id)}
        row-attrs (if highlight-sample (merge base-row-attrs highlight-attrs) base-row-attrs)]
    [:tr row-attrs
     [:td sample-id]
     [:td (:name sample-type)]
     [:td (:reminder sample-type)]
     [:td (boolean-display collected)]
     [:td (boolean-display shipped)]]))

(defn table-style
  []
  #?(:cljs    {:width "100%" :style {:text-align "left"}}
     :default {:width "100%" :style "text-align: left;"}))

(defn sample-table
  [kit-id sample-values highlighted-samples sample-types key-prefix]
  [:table (table-style)
   [:thead
    [:tr
     [:th "Sample ID"]
     [:th "Sample Type"]
     [:th "Reminder"]
     [:th "Collected"]
     [:th "Shipped"]]]
   [:tbody
    (map #(sample-table-row kit-id sample-values highlighted-samples % key-prefix) sample-types)]])

(defn empty-line-style
  []
  #?(:cljs    {:width "100%" :style {:width "300px" :border-bottom "1px solid black" :height "30px"}}
     :default {:width "100%" :style "width: 300px; border-bottom: 1px solid black; height: 30px;"}))

(defn possible-empty-field-display
  [value default-display add-empty-field-lines]
  (cond
    (some? value) default-display
    add-empty-field-lines [:div (empty-line-style)]
    :else ""))

(defn possible-empty-field-row
  [label field highlight add-empty-field-lines]
  (if (and (nil? field) add-empty-field-lines)
    (simple-row label [:div (empty-line-style)])
    (simple-row label field highlight)))

(defn form-type-field-select-display
  [field-value field]
  (let [options (into {} (:options field))]
    (or (get options field-value) (get options (keyword field-value)))))

(defn form-type-field-display
  [field-value field time-display-fn]
  (case (:type field)
    "boolean" (boolean-display field-value)
    "select" (form-type-field-select-display field-value field)
    "string" field-value
    "int" field-value
    "time" (when (some? field-value) (time-display-fn field-value))))

(defn form-type-fields
  [field-values highlighted-field-values field time-display-fn add-empty-field-lines key-prefix]
  (let [{:keys [field-id label type]} field
        field-value (or (get field-values field-id) (get field-values (keyword field-id)))
        highlight-field (or (get highlighted-field-values field-id) (get highlighted-field-values (keyword field-id)))
        field-value-for-display (form-type-field-display field-value field time-display-fn)
        sanitized-field-value-for-display (if (= type "boolean")
                                            field-value-for-display
                                            (possible-empty-field-display field-value field-value-for-display add-empty-field-lines))
        base-row-attrs {:key (str key-prefix field-id)}
        row-attrs (if highlight-field (merge base-row-attrs highlight-attrs) base-row-attrs)]
    [:tr row-attrs [:td label] [:td sanitized-field-value-for-display]]))

; Doing these individual functions instead of one function with [:<>] because the latter renders weird in emails.
(defn signature-header-row
  []
  [:tr [:td [:b "Signed by Clinical Site Personnel Only"]]])

(defn signature-sign-row
  []
  [:tr [:td "Sign:"] [:td [:div (empty-line-style)]]])

(defn signature-date-row
  []
  [:tr [:td "Date:"] [:td [:div (empty-line-style)]]])

(defn content
  ([content-map]
   (content content-map {}))
  ([content-map diff-map]
   (let [{:keys [site-name study-name cohort-name timepoint-names kit-name kit-id participant-id collection-date
                 collection-time selected-form-type-fields form-type-field-values selected-sample-types sample-values
                 air-waybill completing-first-name completing-last-name completing-email comments complete archived
                 date-display-fn time-display-fn add-empty-field-lines add-signature-fields key-prefix edit-email
                 edit-timestamp]} content-map
         {highlight-site            :site-name highlight-study :study-name highlight-cohort :cohort-name
          highlight-timepoints      :timepoint-names highlight-kit-type :kit-name highlight-kit-id :kit-id
          highlight-participant-id  :participant-id highlight-collection-date :collection-date
          highlight-collection-time :collection-time highlighted-field-values :form-type-field-values
          highlighted-sample-values :sample-values highlight-air-waybill :air-waybill
          highlight-first-name      :completing-first-name highlight-last-name :completing-last-name
          highlight-email           :completing-email highlight-comments :comments} diff-map]
     [:table (table-style)
      [:tbody
       (simple-row "Study" study-name highlight-site)
       (simple-row "Site" site-name highlight-study)
       (simple-row "Cohort" cohort-name highlight-cohort)
       (simple-row "Kit Type" kit-name highlight-kit-type)
       (simple-row "Timepoints" (string/join ", " timepoint-names) highlight-timepoints)
       (simple-row "Kit ID" kit-id highlight-kit-id)
       (simple-row "Participant ID" participant-id highlight-participant-id)
       (simple-row "Collection Date" (possible-empty-field-display collection-date (date-display-fn collection-date) add-empty-field-lines) highlight-collection-date)
       (simple-row "Collection Time (24-hour)" (possible-empty-field-display collection-date (time-display-fn collection-time) add-empty-field-lines) highlight-collection-time)
       (map #(form-type-fields form-type-field-values highlighted-field-values % time-display-fn add-empty-field-lines key-prefix) selected-form-type-fields)
       (when (some? selected-sample-types) (simple-row "Samples" (sample-table kit-id sample-values highlighted-sample-values selected-sample-types key-prefix)))
       (simple-row "Air Waybill" air-waybill highlight-air-waybill)
       (simple-row "Form Completed By First Name" completing-first-name highlight-first-name)
       (simple-row "Form Completed By Last Name" completing-last-name highlight-last-name)
       (simple-row "Form Completed By Email" completing-email highlight-email)
       (possible-empty-field-row "Comments" comments highlight-comments add-empty-field-lines)
       (when add-signature-fields [:tr])
       (when add-signature-fields (signature-header-row))
       (when add-signature-fields (signature-sign-row))
       (when add-signature-fields (signature-date-row))
       (when (some? edit-email) (simple-row "Last Editing User" (str edit-email)))
       (when (some? edit-timestamp)
         (simple-row "Last Edit Timestamp" (str (date-display-fn edit-timestamp) " " (time-display-fn edit-timestamp))))
       (when (some? complete) (simple-row "Complete" (str complete)))
       (when (some? archived) (simple-row "Archived" (str archived)))]])))