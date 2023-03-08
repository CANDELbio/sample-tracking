(ns org.parkerici.sample-tracking.api.kit-shipment
  (:require [clojure.set :as set]
            [java-time :as time]
            [org.parkerici.sample-tracking.db.core :as db]
            [org.parkerici.sample-tracking.db.timepoint :as timepoint-db]
            [org.parkerici.sample-tracking.db.kit-type :as kit-type-db]
            [org.parkerici.sample-tracking.db.kit :as kit-db]
            [org.parkerici.sample-tracking.db.form-type :as form-type-db]
            [org.parkerici.sample-tracking.db.form-value :as form-value-db]
            [org.parkerici.sample-tracking.db.sample :as sample-db]
            [org.parkerici.sample-tracking.db.shipment :as shipment-db]
            [org.parkerici.sample-tracking.db.history :as history-db]
            [org.parkerici.sample-tracking.db.proposed-kit-edit :as proposed-kit-edit-db]
            [org.parkerici.sample-tracking.api.email :as email]
            [org.parkerici.sample-tracking.utils.collection :as coll-utils])
  (:import (java.util Date UUID)))

(defn create-samples-and-add-to-kit-shipment
  [kit-uuid shipment-uuid samples]
  (doseq [sample-type-uuid (keys samples)]
    (let [sample (get samples sample-type-uuid)
          sample-collected (boolean (:collected sample))
          sample-shipped (boolean (:shipped sample))]
      (when sample-collected
        (let [sample-uuid (sample-db/create-or-update-sample nil (name sample-type-uuid) (:sample-id sample)
                                                             sample-collected sample-shipped)]
          (kit-db/add-sample-to-kit kit-uuid sample-uuid)
          (when (and shipment-uuid sample-shipped) (sample-db/add-sample-to-shipment shipment-uuid sample-uuid)))))))

(defn create-update-form-value
  [form-value-uuid form-value-field-id-key form-fields form-values]
  (let [form-value-field-id (name form-value-field-id-key)
        form-field (first (filter #(= form-value-field-id (:field-id %)) form-fields))
        form-field-type (:type form-field)
        form-field-uuid (:uuid form-field)
        raw-form-value (get form-values form-value-field-id-key)
        parsed-form-value (case form-field-type
                            "time" (if (string? raw-form-value) (time/java-date raw-form-value) raw-form-value)
                            "boolean" (if (string? raw-form-value) (Boolean/valueOf raw-form-value) raw-form-value)
                            "int" (if (string? raw-form-value) (Long/parseLong raw-form-value) raw-form-value)
                            raw-form-value)]
    (form-value-db/create-or-update-form-value form-value-uuid form-field-uuid form-field-type parsed-form-value)))

(defn create-form-values-and-add-to-kit
  [kit-uuid kit-type-uuid form-values]
  (let [form-fields (form-type-db/get-form-type-fields kit-type-uuid)]
    (doseq [form-value-field-id-key (keys form-values)]
      (let [form-value-uuid (create-update-form-value nil form-value-field-id-key form-fields form-values)]
        (kit-db/add-form-value-to-kit kit-uuid form-value-uuid)))))

(defn create-kit-shipment
  [kit-map]
  (let [{:keys [air-waybill kit-type samples form-type-field-values]} kit-map
        kit-type-uuid (UUID/fromString kit-type)
        kit-uuid (kit-db/create-or-update-kit nil kit-map)
        shipment-uuid (when (some? air-waybill) (shipment-db/create-or-update-shipment nil air-waybill))]
    (when (some? shipment-uuid) (kit-db/add-shipment-to-kit kit-uuid shipment-uuid))
    (create-samples-and-add-to-kit-shipment kit-uuid shipment-uuid samples)
    (create-form-values-and-add-to-kit kit-uuid kit-type-uuid form-type-field-values)
    kit-uuid))

; If uuid is passed in then filters on that as a kit uuid. Otherwise returns all kits.
; If tx-id is passed in then queries the kit-shipment values as-of the historical tx-id
(defn list-kit-shipment
  [config-map]
  (let [kits (kit-db/list-kits config-map)
        timepoints (timepoint-db/list-kit-timepoints config-map)
        samples (sample-db/list-samples config-map)
        form-values (form-value-db/list-form-values config-map)
        shipments (shipment-db/list-shipments config-map)
        pending-edits (map #(select-keys % [:kit-uuid :uuid :email :time])
                           (proposed-kit-edit-db/list-proposed-edits {:status "pending"}))
        edit-history (map #(select-keys % [:entity-uuid :agent-email :time])
                          (history-db/list-history nil))]
    (-> kits
        (coll-utils/merge-map-colls :uuid timepoints :kit-uuid :timepoints)
        (coll-utils/merge-map-colls :uuid samples :kit-uuid :samples)
        (coll-utils/merge-map-colls :uuid form-values :kit-uuid :form-values)
        (coll-utils/merge-map-colls :uuid shipments :kit-uuid :shipments)
        (coll-utils/merge-map-colls :uuid pending-edits :kit-uuid :pending-edits)
        (coll-utils/merge-map-colls :uuid edit-history :entity-uuid :history))))

(defn delete-samples
  [current-sample-map sample-type-uuids]
  (let [uuids (doall (map #(vector :sample-type/uuid (:uuid (get current-sample-map %))) sample-type-uuids))]
    (db/retract-entities uuids)))

(defn update-existing-samples
  [shipment-uuid current-sample-map updated-sample-map sample-type-uuids]
  (let [shipment-sample-uuids (sample-db/list-shipment-samples shipment-uuid)]
    (doseq [sample-type-uuid sample-type-uuids]
      (let [current-sample (get current-sample-map sample-type-uuid)
            sample-uuid (:uuid current-sample)
            current-sample-in-shipment (boolean (some #(= sample-uuid %) shipment-sample-uuids))
            updated-sample (get updated-sample-map sample-type-uuid)
            updated-sample-collected (boolean (:collected updated-sample))
            updated-sample-shipped (boolean (:shipped updated-sample))]
        ; Update the existing shipment in the db with the new values
        (sample-db/create-or-update-sample sample-uuid (name sample-type-uuid) (:sample-id updated-sample)
                                           updated-sample-collected updated-sample-shipped)
        ; If the current sample was marked as shipped, but the updated one was not then remove the sample from the associated shipment.
        (when (and current-sample-in-shipment (not updated-sample-shipped)) (sample-db/remove-sample-from-shipment shipment-uuid sample-uuid))
        ; If the current sample was not marked as shipped, but the updated one is then add the sample to the associated shipment
        (when (and (not current-sample-in-shipment) updated-sample-shipped) (sample-db/add-sample-to-shipment shipment-uuid sample-uuid))))))

; Currently assumes one shipment per kit. May not be the case in the future.
(defn create-or-update-shipment
  [kit-uuid air-waybill]
  (let [current-shipment-uuid (:uuid (first (shipment-db/list-shipments kit-uuid)))
        new-shipment-uuid (shipment-db/create-or-update-shipment current-shipment-uuid air-waybill)]
    (when (and (some? new-shipment-uuid) (not= new-shipment-uuid current-shipment-uuid))
      (kit-db/add-shipment-to-kit kit-uuid new-shipment-uuid))
    (if current-shipment-uuid
      current-shipment-uuid
      new-shipment-uuid)))

(defn create-update-delete-samples
  [kit-uuid shipment-uuid current-sample-map updated-sample-map]
  (let [updated-sample-type-uuids (set (keys updated-sample-map))
        current-sample-type-uuids (set (keys current-sample-map))
        new-sample-type-uuids (set/difference updated-sample-type-uuids current-sample-type-uuids)
        delete-sample-type-uuids (set/difference current-sample-type-uuids updated-sample-type-uuids)
        update-sample-type-uuids (set/intersection current-sample-type-uuids updated-sample-type-uuids)]
    (create-samples-and-add-to-kit-shipment kit-uuid shipment-uuid (select-keys updated-sample-map new-sample-type-uuids))
    (delete-samples current-sample-map delete-sample-type-uuids)
    (update-existing-samples shipment-uuid current-sample-map updated-sample-map update-sample-type-uuids)))

(defn update-existing-form-values
  [kit-uuid kit-type-uuid current-form-values new-form-values]
  (let [form-fields (form-type-db/get-form-type-fields kit-type-uuid)
        current-form-field-id-map (reduce (fn [m v] (assoc m (keyword (:field-id v)) (:uuid v))) {} current-form-values)]
    (doseq [form-value-field-id-key (keys new-form-values)]
      (let [current-form-value-id (get current-form-field-id-map form-value-field-id-key)
            updated-form-value-id (create-update-form-value (get current-form-field-id-map form-value-field-id-key) form-value-field-id-key form-fields new-form-values)]
        (when (nil? current-form-value-id) (kit-db/add-form-value-to-kit kit-uuid updated-form-value-id))))))

(defn delete-existing-form-values
  [current-form-values]
  (let [ids (map #(vector :form-value/uuid (:uuid %)) current-form-values)]
    (db/retract-entities ids)))

(defn delete-existing-create-new-form-values
  [kit-uuid kit-type-uuid current-form-values new-form-values]
  (delete-existing-form-values current-form-values)
  (create-form-values-and-add-to-kit kit-uuid kit-type-uuid new-form-values))

(defn update-form-values
  [kit-db-id current-kit-type-uuid updated-kit-type-uuid current-form-values new-form-values]
  (let [current-form-type (kit-type-db/get-kit-type-form-type current-kit-type-uuid)
        updated-form-type (kit-type-db/get-kit-type-form-type updated-kit-type-uuid)]
    ; If the form-type hasn't changed we can just update the existing form values.
    ; If it has changed then we should delete the existing values and create new ones.
    (if (= (:uuid current-form-type) (:uuid updated-form-type))
      (update-existing-form-values kit-db-id updated-kit-type-uuid current-form-values new-form-values)
      (delete-existing-create-new-form-values kit-db-id updated-kit-type-uuid current-form-values new-form-values))))

(defn get-kit-values
  [kit-uuid]
  (let [config-map {:uuid kit-uuid}
        kit (first (kit-db/list-kits config-map))
        sample-map (reduce (fn [m v] (assoc m (keyword (str (:sample-type-uuid v))) v)) {} (sample-db/list-samples config-map))
        form-values (form-value-db/list-form-values config-map)
        shipment (first (shipment-db/list-shipments config-map))]
    {:kit         kit
     :samples     sample-map
     :form-values form-values
     :shipment    shipment}))

(defn remove-deleted-timepoints
  [kit-uuid updated-kit-map]
  (let [current-timepoints (timepoint-db/list-kit-timepoints {:uuid kit-uuid})
        current-timepoint-uuids (map :uuid current-timepoints)
        updated-timepoints-uuids (map #(UUID/fromString %) (:timepoints updated-kit-map))
        deleted-timepoints (set/difference (set current-timepoint-uuids) (set updated-timepoints-uuids))]
    (doseq [timepoint-uuid deleted-timepoints] (kit-db/remove-timepoint-from-kit kit-uuid timepoint-uuid))))

(defn update-kit-shipment
  [kit-uuid kit-map]
  (let [{:keys [air-waybill kit-type samples form-type-field-values]} kit-map
        config-map {:uuid kit-uuid}
        kit-type-uuid (UUID/fromString kit-type)
        current-kit (first (kit-db/list-kits config-map))
        current-sample-map (reduce (fn [m v] (assoc m (keyword (str (:sample-type-uuid v))) v)) {} (sample-db/list-samples config-map))
        current-form-values (form-value-db/list-form-values config-map)
        shipment-id (create-or-update-shipment kit-uuid air-waybill)]
    (kit-db/create-or-update-kit kit-uuid kit-map)
    (remove-deleted-timepoints kit-uuid kit-map)
    (create-update-delete-samples kit-uuid shipment-id current-sample-map samples)
    (update-form-values kit-uuid (:kit-type-uuid current-kit) kit-type-uuid current-form-values form-type-field-values)
    kit-uuid))

(defn update-kit-shipment-with-history
  [kit-uuid user kit-map]
  (let [current-kit-values (get-kit-values kit-uuid)]
    (update-kit-shipment kit-uuid kit-map)
    (history-db/create-history user :kit-shipment kit-uuid (str current-kit-values) (str (get-kit-values kit-uuid)))
    kit-uuid))

; If a kit hasn't been created, create it as complete otherwise update it to be complete. Send and email when done.
(defn submit-kit-shipment
  [submitted-kit-uuid kit-map]
  (let [completed-kit-map (merge kit-map {:complete true :submission-timestamp (Date.)})
        kit-uuid (or submitted-kit-uuid (create-kit-shipment completed-kit-map))]
    (when-not (nil? submitted-kit-uuid)
      (update-kit-shipment kit-uuid completed-kit-map))
    (email/send-manifest-email kit-map kit-uuid)
    kit-uuid))

(defn set-kit-shipment-archived
  [kit-uuid user archived]
  (let [current-kit-values (get-kit-values kit-uuid)]
    (kit-db/set-archived kit-uuid archived)
    (doseq [shipment (shipment-db/list-shipments {:uuid kit-uuid})]
      (shipment-db/set-archived (:uuid shipment) archived))
    (history-db/create-history user :kit-shipment kit-uuid (str current-kit-values) (str (get-kit-values kit-uuid)))
    kit-uuid))