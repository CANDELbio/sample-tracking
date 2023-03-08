(ns org.parkerici.sample-tracking.components.kit.shipment.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
  :kit-shipment/submitting
  (fn [db]
    (get-in db [:kit-shipment/kit-shipment :kit-shipment/submitting])))

(reg-sub
  :kit-shipment/share-modal-visible
  (fn [db]
    (get-in db [:kit-shipment/kit-shipment :kit-shipment/share-modal-visible])))

(reg-sub
  :kit-shipment/share-tooltip-visible
  (fn [db]
    (get-in db [:kit-shipment/kit-shipment :kit-shipment/share-tooltip-visible])))

(reg-sub
  :kit-shipment/submit-shipment-confirmation
  (fn [db]
    (when (get-in db [:kit-form/kit-form :kit-form/kit-id-submitted])
      (let [kit-id (get-in db [:kit-form/kit-form :kit-form/form-values :form-values/kit-id])]
        (str "This kit ID has already been used. Are you sure " kit-id " is correct?\nIf it is correct please click 'OK'. Otherwise click 'Cancel' and correct it.")))))