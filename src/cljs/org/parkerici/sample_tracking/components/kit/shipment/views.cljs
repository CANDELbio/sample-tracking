(ns org.parkerici.sample-tracking.components.kit.shipment.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [org.parkerici.sample-tracking.components.kit.form.views :as kit-form]
            [modal]                                         ;React component https://www.npmjs.com/package/reactstrap
            [modal-header]                                  ;React component https://www.npmjs.com/package/reactstrap
            [modal-body]                                    ;React component https://www.npmjs.com/package/reactstrap
            [modal-footer]                                  ;React component https://www.npmjs.com/package/reactstrap
            [tooltip]                                       ;React component https://www.npmjs.com/package/reactstrap
            [org.parkerici.sample-tracking.utils.str :as str]
            [org.parkerici.sample-tracking.utils.js :as js-utils]
            [org.parkerici.sample-tracking.components.kit.shipment.events]
            [org.parkerici.sample-tracking.components.kit.shipment.db]
            [org.parkerici.sample-tracking.components.kit.shipment.subs]
            [org.parkerici.sample-tracking.routes :as routes]))

(defn copy-to-clipboard [element-id]
  (let [el (js/document.getElementById element-id)]
    (.select el)
    (js/document.execCommand "copy")))

(defn copy-to-clipboard-element
  [value]
  [:div
   {:style {:text-align "center"}}
   [:div [:input#clipboard-input.form-control
          {:type      "text"
           :value     value
           :read-only true}]]
   [:div
    {:style {:padding-top "5px"}}
    [:button.btn.btn-secondary
     {:type     "button"
      :on-click (fn [] (copy-to-clipboard "clipboard-input"))}
     "Copy to Clipboard"]]])

(defn share-modal
  [kit-uuid visible]
  (let [share-url (str (.. js/window -location -origin) (routes/path-for :shared-kit-shipment :uuid kit-uuid))]
    [:> modal
     {:is-open visible}
     [:> modal-header "Manifest Sharing"]
     [:> modal-body (copy-to-clipboard-element share-url)]
     [:> modal-footer
      [:button.btn.btn-secondary
       {:type     "button"
        :on-click (fn [] (dispatch [:kit-shipment/set-value :kit-shipment/share-modal-visible false]))}
       "Close"]]]))

(defn submitting-modal
  [visible]
  [:> modal
   {:is-open visible}
   [:> modal-header "Submitting Manifest"]
   [:> modal-body "Please wait while your shipping manifest is generated."]])

(defn clear-button
  [enabled]
  [:<>
   [:button.btn.btn-secondary
    {:type     "button"
     :disabled (not enabled)
     :on-click (fn [] (when (js/confirm "Are you sure you want to clear this form?")
                        (dispatch [:kit-shipment/reset-form])))}
    "Clear Form"]
   ])

(defn share-disabled
  [selected-type-options]
  (let [{:keys [site study cohort timepoints kit-type]} selected-type-options
        {:form-values/keys [kit-id]} @(subscribe [:kit-form/form-values])]
    (not (and (some? site) (some? study) (some? cohort) (seq timepoints)
              (some? kit-type) (str/not-blank? kit-id)))))

(defn share-tooltip-text
  [selected-type-options]
  (let [{:keys [site study cohort timepoints kit-type]} selected-type-options
        {:form-values/keys [kit-id]} @(subscribe [:kit-form/form-values])]
    [:div
     {:style {:text-align "left"}}
     "You must enter the following values before sharing:"
     [:ul
      (when-not (some? study) [:li "Study"])
      (when-not (some? site) [:li "Site"])
      (when-not (some? cohort) [:li "Cohort"])
      (when-not (some? timepoints) [:li "Timepoint"])
      (when-not (some? kit-type) [:li "Kit Type"])
      (when-not (some? kit-id) [:li "Kit ID"])]]))

(defn share-tooltip
  [selected-type-options]
  (let [visible @(subscribe [:kit-shipment/share-tooltip-visible])]
    [:> tooltip
     {:is-open   visible
      :placement "top"
      :target    "share-button"}
     (share-tooltip-text selected-type-options)]))

; Wrapping span with mouse-enter and mouse-leave events and pointer-events style is to workaround mouse-enter working
; for disabled buttons but mouse-leave not working for disabled buttons
(defn share-button
  [selected-type-options]
  (let [share-disabled (share-disabled selected-type-options)
        button-style (if share-disabled {:pointer-events "none"} {})]
    [:<>
     [:span
      {:on-mouse-enter (fn [] (when share-disabled (dispatch [:kit-shipment/set-value :kit-shipment/share-tooltip-visible true])))
       :on-mouse-leave (fn [] (when share-disabled (dispatch [:kit-shipment/set-value :kit-shipment/share-tooltip-visible false])))}
      [:button#share-button.btn.btn-secondary
       {:type     "button"
        :disabled share-disabled
        :on-click (fn [] (dispatch [:kit-shipment/share]))
        :style    button-style}
       "Share Form"]]
     (share-tooltip selected-type-options)]))

(defn extra-buttons
  [clear-enabled selected-type-options]
  [:<>
   (clear-button clear-enabled)
   [:div.spacer]
   (share-button selected-type-options)])

(defn component
  []
  (fn []
    ; Hacky method to ensure the background of the page is black.
    ; Use this because the manifest page sets it to white using the same method, and is flaky with resetting it to black
    ; when changing pages.
    (js-utils/set-body-bg-color "black")
    (let [kit-uuid (:form-values/uuid @(subscribe [:kit-form/form-values]))
          share-modal-visible @(subscribe [:kit-shipment/share-modal-visible])
          current-page (first @(subscribe [:page]))
          clear-enabled (not= current-page :shared-kit-shipment)
          selected-type-options @(subscribe [:type-filter/selected-option-values])
          submit-confirmation @(subscribe [:kit-shipment/submit-shipment-confirmation])
          submitting @(subscribe [:kit-shipment/submitting])]
      [:<>
       (when kit-uuid (share-modal kit-uuid share-modal-visible))
       (submitting-modal submitting)
       [kit-form/component {:submit-event        [:kit-shipment/submit]
                            :submit-button-text  "Generate Manifest"
                            :extra-buttons       (extra-buttons clear-enabled selected-type-options)
                            :submit-confirmation submit-confirmation
                            :submit-disabled     submitting}]])))