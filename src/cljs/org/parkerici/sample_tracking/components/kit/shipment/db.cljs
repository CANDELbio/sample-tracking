(ns org.parkerici.sample-tracking.components.kit.shipment.db
  (:require [cljs.spec.alpha :as s]))

(s/def :kit-shipment/submitting boolean?)
(s/def :kit-shipment/share-modal-visible boolean?)
(s/def :kit-shipment/share-tooltip-visible boolean?)

(s/def :kit-shipment/kit-shipment (s/keys :opt [:kit-shipment/submitting
                                                :kit-shipment/share-modal-visible
                                                :kit-shipment/share-tooltip-visible]))