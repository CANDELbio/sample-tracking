(ns org.parkerici.sample-tracking.components.kit.form.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [edi-text]                                      ;React component https://www.npmjs.com/package/react-editext
            [checkbox]                                      ;React component https://www.npmjs.com/package/react-input-checkbox
            [date-input]                                    ;React component https://www.npmjs.com/package/@blueprintjs/datetime
            [time-picker]                                   ;React component https://www.npmjs.com/package/@blueprintjs/datetime
            [clojure.string :as str]
            [org.parkerici.sample-tracking.utils.time :as time-util]
            [org.parkerici.sample-tracking.components.type.filter.views :as type-filter]
            [org.parkerici.sample-tracking.routes :as routes]
            [org.parkerici.sample-tracking.components.kit.form.db]
            [org.parkerici.sample-tracking.components.kit.form.events]
            [org.parkerici.sample-tracking.components.kit.form.subs]))

(defn dispatch-events
  [events new-value]
  (doseq [event events]
    (dispatch (conj event new-value))))

(defn form-input
  ([config-map]
   [:input.form-control
    (merge {:type      (:type config-map)
            :value     (:value config-map)
            :required  (:required config-map)
            :on-change (fn [evt]
                         (let [new-value (-> evt .-target .-value)]
                           (dispatch-events (:events config-map) new-value)))}
           (:input-options config-map))]))

(defn text-input
  ([config-map]
   (form-input (assoc config-map :type "text"))))

(defn number-input
  ([config-map]
   (form-input (assoc config-map :type "number"))))

(defn email-input
  ([config-map]
   (let [input-options (or (:input-options config-map) {})
         email-input-options (-> input-options
                                 (assoc :pattern "[^@\\s]+@[^@\\s]+\\.[^@\\s]+")
                                 (assoc :title "Please enter a valid email address."))
         email-config-map (-> config-map
                              (assoc :type "email")
                              (assoc :input-options email-input-options))]
     (form-input email-config-map))))

(defn text-area-input
  [config-map]
  [:textarea.form-control
   {:type      "text"
    :value     (:value config-map)
    :required  (:required config-map)
    :on-change (fn [evt]
                 (let [new-value (-> evt .-target .-value)]
                   (dispatch-events (:events config-map) new-value)))}])

(defn boolean-input
  [config-map]
  (let [value (:value config-map)]
    [:> checkbox
     {:children  ""
      :theme     "bootstrap-checkbox"
      :value     (if (some? value) value false)
      :disabled  (not (:enabled config-map))
      :on-change (fn [] (doseq [event (:events config-map)] (dispatch event)))}]))

(defn date-input-field
  [config-map]
  [:div
   [:> date-input
    {:value       (:value config-map)
     :format-date (fn [date] (.toLocaleDateString date))
     :placeholder "M/D/YYYY"
     :parse-date  (fn [str] (js/Date. str))
     :input-props {:required (:required config-map)}
     :on-change   (fn [selected-date _is-user-change]
                    (dispatch-events (:events config-map) selected-date))}]])

(defn time-input-initial-value
  "Time input doesn't clear properly if no value is passed in.
  Instead we need to generate and pass in a date with 0 hour and 0 minute"
  []
  (let [date (js/Date.)]
    (.setHours date 0)
    (.setMinutes date 0)
    date))

(defn time-input-field
  [config-map]
  [:div
   [:> time-picker
    {:value               (or (:value config-map) (time-input-initial-value))
     :input-props         {:required (:required config-map)}
     :select-all-on-focus true
     :on-change           (fn [selected-date]
                            (dispatch-events (:events config-map) selected-date))}]])

(defn simple-row
  [label field]
  [:tr [:td [:label label]] [:td field]])

(defn add-set-value-event
  [config-map]
  (let [set-value-event [:kit-form/set-form-value (:db-key config-map)]
        updated-events (cons set-value-event (:events config-map))]
    (assoc config-map :events updated-events)))

(defn text-input-row
  [config-map]
  (simple-row (:label config-map) (text-input (add-set-value-event config-map))))

(defn text-area-input-row
  [config-map]
  (simple-row (:label config-map) (text-area-input (add-set-value-event config-map))))

(defn email-input-row
  [config-map]
  (simple-row (:label config-map) (email-input (add-set-value-event config-map))))

(defn date-input-row
  [config-map]
  (simple-row (:label config-map) (date-input-field (add-set-value-event config-map))))

(defn time-input-row
  [config-map]
  (simple-row (:label config-map) (time-input-field (add-set-value-event config-map))))

(defn boolean-input-row
  [label db-key value]
  (simple-row label (boolean-input {:value value :enabled true :events [[:kit-form/set-form-value db-key (not value)]]})))

; If the event hasn't been marked as collected and it's marked as shipped, we also want to mark it as collected.
(defn generate-shipped-events
  [sample-type-uuid sample-id shipped]
  (let [shipped-events [[:kit-form/mark-sample sample-type-uuid sample-id :shipped (not shipped)]]]
    (if shipped
      shipped-events
      (conj shipped-events [:kit-form/mark-sample sample-type-uuid sample-id :collected (not shipped)]))))

(defn sample-table-row
  [kit-id sample-values sample-type show-reminder]
  (let [sample-type-uuid (:uuid sample-type)
        sample-value (get sample-values sample-type-uuid)
        sample-id (or (:sample-id sample-value) (str kit-id (:id-suffix sample-type)))
        ships-with-kit (:ships-with-kit sample-type)
        collected (boolean (:collected sample-value))       ; Coerce to bool if nil so React doesn't complain about checkbox with checked nil
        shipped (boolean (:shipped sample-value))           ; Coerce to bool if nil so React doesn't complain about checkbox with checked nil
        collected-events [[:kit-form/mark-sample sample-type-uuid sample-id :collected (not collected)]]
        shipped-events (generate-shipped-events sample-type-uuid sample-id shipped)]
    [:tr {:key sample-type-uuid}
     [:td [:> edi-text
           {:type        "text"
            :input-props {:class-name "form-control"}
            :value       sample-id
            :on-save     (fn [new-value]
                           (dispatch [:kit-form/set-sample-id sample-type-uuid new-value]))}]]
     [:td (:name sample-type)]
     (when show-reminder [:td (:reminder sample-type)])
     [:td {:style {:text-align "right"}} (boolean-input {:value collected :enabled (not shipped) :events collected-events})]
     [:td {:style {:text-align "right"}} (boolean-input {:value shipped :enabled ships-with-kit :events shipped-events})]]))

(defn sample-table
  [kit-id sample-values sample-types some-samples-collected some-samples-shipped]
  (let [some-reminders (not (reduce (fn [b v] (and b (str/blank? (:reminder v)))) true sample-types))
        all-collected-events [[:kit-form/mark-all-samples :collected (not some-samples-collected) false]]
        all-shipped-base-event [:kit-form/mark-all-samples :shipped (not some-samples-shipped) true]
        all-shipped-events (if some-samples-collected
                             [all-shipped-base-event]
                             [[:kit-form/mark-all-samples :collected true true]
                              all-shipped-base-event])]
    [:table {:width "100%"}
     [:thead
      [:tr
       [:th "Sample ID"]
       [:th "Sample Type"]
       (when some-reminders [:th "Reminder"])
       [:th {:style {:text-align "right"}}
        "Collected "
        (boolean-input {:value some-samples-collected :enabled (not some-samples-shipped) :events all-collected-events})]
       [:th {:style {:text-align "right"}}
        "Shipped "
        (boolean-input {:value some-samples-shipped :enabled true :events all-shipped-events})]]]
     [:tbody
      (map #(sample-table-row kit-id sample-values % some-reminders) sample-types)]]))

(defn form-type-select-input
  [field-value field]
  (let [field-id (:field-id field)
        required (:required field)
        options (:options field)]
    [:select {:required  required
              :id        field-id
              :value     (or field-value "")
              :on-change (fn [evt]
                           (let [new-value (-> evt .-target .-value)]
                             (dispatch [:kit-form/set-form-type-field-value field-id new-value])))}
     [:option {:value ""} "Select..."]
     (for [option-value (keys options)]
       [:option {:key option-value :value option-value} (get options option-value)])]))

; TODO Think about how to DRY this up. There's a good amount of overlap in the below four, but some mismatch with
; TODO select input above and boolean input that complicates it.
(defn form-type-boolean-input
  [field-value field]
  (boolean-input {:events  [[:kit-form/set-form-type-field-value (:field-id field) (not field-value)]]
                  :value   field-value
                  :enabled true}))

(defn form-type-string-input
  [field-value field]
  (text-input {:events   [[:kit-form/set-form-type-field-value (:field-id field)]]
               :value    field-value
               :required (:required field)}))

(defn form-type-number-input
  [field-value field]
  (number-input {:events   [[:kit-form/set-form-type-field-value (:field-id field)]]
                 :value    field-value
                 :required (:required field)}))

(defn form-type-time-input
  [field-value field]
  (time-input-field {:events   [[:kit-form/set-form-type-field-value (:field-id field)]]
                     :value    field-value
                     :required (:required field)}))

(defn form-type-field-input
  [field-value field]
  (case (:type field)
    "boolean" (form-type-boolean-input (boolean field-value) field)
    "select" (form-type-select-input field-value field)
    "string" (form-type-string-input field-value field)
    "int" (form-type-number-input field-value field)
    "time" (form-type-time-input field-value field)))

(defn form-type-fields
  [field-values field]
  (let [field-id (:field-id field)
        field-label (:label field)
        field-value (get field-values field-id)]
    [:tr {:key field-id} [:td field-label] [:td (form-type-field-input field-value field)]]))

(defn validator-map
  [selected-study pattern-key title-key]
  (cond-> {}
          (contains? selected-study pattern-key) (assoc :pattern (get selected-study pattern-key))
          (contains? selected-study title-key) (assoc :title (get selected-study title-key))))

(defn component
  []
  (fn [config-map]
    (let [{:keys [submit-event submit-button-text submit-confirmation extra-buttons show-admin-fields submit-disabled
                  allow-pending-edits]} config-map
          {selected-study :study selected-kit-type :kit-type} @(subscribe [:type-filter/selected-option-values])
          {:form-values/keys [uuid kit-id participant-id air-waybill collection-date collection-time
                              completing-first-name completing-last-name completing-email samples comments complete
                              archived form-type-field-values pending-edits history]} @(subscribe [:kit-form/form-values])
          some-samples-collected @(subscribe [:kit-form/some-samples-collected])
          some-samples-shipped @(subscribe [:kit-form/some-samples-shipped])
          pending-edit (first pending-edits)
          most-recent-edit (first history)
          kit-id-value (or kit-id (:kit-id-prefix selected-study))
          kit-validator-map (validator-map selected-study :kit-id-regex :kit-id-validation-message)
          participant-id-value (or participant-id (:participant-id-prefix selected-study))
          participant-validator-map (validator-map selected-study :participant-id-regex :participant-id-validation-message)
          sample-values-key [:kit-form/kit-form :kit-form/form-values :form-values/samples]
          selected-sample-types @(subscribe [:type-filter/sample-types])
          selected-form-type-fields @(subscribe [:type-filter/selected-form-type-fields])
          selected-form-type-fields-key [:kit-form/kit-form :kit-form/form-values :form-values/form-type-field-values]
          participant-id-key [:kit-form/kit-form :kit-form/form-values :form-values/participant-id]
          active-url-params {:active true}
          additional-url-params {:study  active-url-params
                                 :site   active-url-params
                                 :cohort active-url-params}
          keys-to-clear {:kit-types [sample-values-key selected-form-type-fields-key]
                         :study     [participant-id-key]}
          submit-button-attrs (cond-> {:type "submit"}
                                      (when submit-disabled) (assoc :disabled true))]
      (if (and (not allow-pending-edits) (and uuid (> (count pending-edits) 0)))
        [:<>
         [:p "Editing this kit disabled due to pending proposed edits."]
         [:p "You must respond to the "
          [:a {:href (routes/path-for :proposed-kit-edit-view :uuid (:uuid pending-edit))} "proposed edits"]
          " before editing this kit."]]
        [:form {:on-submit (fn [evt]
                             (.preventDefault evt)
                             (when (or (nil? submit-confirmation) (js/confirm submit-confirmation))
                               (dispatch submit-event)))}
         [:table {:width "100%"}
          [:tbody
           (type-filter/component-rows :url-params additional-url-params :keys-to-clear keys-to-clear)
           (text-input-row {:label         "Kit ID"
                            :db-key        :kit-id
                            :value         kit-id-value
                            :required      true
                            :events        [[:kit-form/check-kit-id]]
                            :input-options kit-validator-map})
           (text-input-row {:label         "Participant ID"
                            :db-key        :participant-id
                            :value         participant-id-value
                            :required      true
                            :input-options participant-validator-map})
           (date-input-row {:label    "Collection Date"
                            :db-key   :collection-date
                            :value    collection-date
                            :required (:collection-date-required selected-kit-type)})
           (time-input-row {:label    "Collection Time (24-hour)"
                            :db-key   :collection-time
                            :value    collection-time
                            :required (:collection-date-required selected-kit-type)})
           (map #(form-type-fields form-type-field-values %) selected-form-type-fields)
           (when (some? selected-sample-types)
             (simple-row "Samples"
                         (sample-table kit-id samples selected-sample-types some-samples-collected some-samples-shipped)))
           (text-input-row {:label         "Air Waybill"
                            :db-key        :air-waybill
                            :value         air-waybill
                            :required      (or (:air-waybill-required selected-kit-type) some-samples-shipped)
                            :input-options {:pattern "[0-9]{12}" :title "Air Waybill must be 12 digits long."}})
           (text-input-row {:label    "Form Completed By First Name"
                            :db-key   :completing-first-name
                            :value    completing-first-name
                            :required true})
           (text-input-row {:label    "Form Completed By Last Name"
                            :db-key   :completing-last-name
                            :value    completing-last-name
                            :required true})
           (email-input-row {:label    "Form Completed By Email"
                             :db-key   :completing-email
                             :value    completing-email
                             :required true})
           (text-area-input-row {:label    "Comments"
                                 :db-key   :comments
                                 :value    comments
                                 :required false})
           (when allow-pending-edits (simple-row "Pending Edit Email" (:email pending-edit)))
           (when allow-pending-edits (simple-row "Pending Edit Timestamp" (time-util/timestamp-string-to-formatted-string (:time pending-edit))))
           (when show-admin-fields (boolean-input-row "Complete" :complete complete))
           (when show-admin-fields (simple-row "Archived" (str archived)))
           (when show-admin-fields (simple-row "Last Editing User" (:agent-email most-recent-edit)))
           (when show-admin-fields (simple-row "Last Edit Timestamp" (time-util/timestamp-string-to-formatted-string (:time most-recent-edit))))]]
         (when (some? extra-buttons)
           [:<>
            extra-buttons
            [:div.spacer]])
         [:button.btn.btn-secondary submit-button-attrs submit-button-text]]))))