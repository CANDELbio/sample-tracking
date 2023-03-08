(ns org.parkerici.sample-tracking.api.propose-kit-edits
  (:require [clojure.edn :as edn]
            [org.parkerici.sample-tracking.api.kit-shipment :as kit-shipment]
            [org.parkerici.sample-tracking.db.proposed-kit-edit :as proposed-kit-edit-db]
            [org.parkerici.sample-tracking.db.site :as site-db]
            [org.parkerici.sample-tracking.db.study :as study-db]
            [org.parkerici.sample-tracking.db.cohort :as cohort-db]
            [org.parkerici.sample-tracking.db.form-type :as form-type-db]
            [org.parkerici.sample-tracking.db.kit-type :as kit-type-db]
            [org.parkerici.sample-tracking.db.sample-type :as sample-type-db]
            [org.parkerici.sample-tracking.db.timepoint :as timepoint-db]
            [org.parkerici.sample-tracking.api.email :as email]
            [org.parkerici.sample-tracking.utils.collection :as coll-utils])
  (:import (java.util UUID)))

(defn propose-kit-edits
  [kit-map user-email]
  (let [kit-uuid (UUID/fromString (:uuid kit-map))
        pending-proposed-edit-uuid (:uuid (first (proposed-kit-edit-db/list-proposed-edits {:kit-uuid kit-uuid :status "pending"})))
        uuid (proposed-kit-edit-db/create-or-update-proposed-edit pending-proposed-edit-uuid kit-uuid (str kit-map) user-email)]
    (email/send-proposed-edit-email kit-map user-email)
    uuid))

(defn list-proposed-edits
  [config-map]
  (let [proposed-edits (proposed-kit-edit-db/list-proposed-edits config-map)
        timepoints (timepoint-db/list-kit-timepoints config-map)]
    (coll-utils/merge-map-colls proposed-edits :kit-uuid timepoints :kit-uuid :timepoints)))

(defn convert-update-map-to-display-map
  "There are three formats of maps for kits. Form maps for populating edit forms, display maps for generating a view page
  using manifest.cljc, and create/update maps for creating or updating kits.
  This function converts an update map to a display map."
  [update-map]
  (let [kit-type-uuid (UUID/fromString (:kit-type update-map))
        site-name (:name (site-db/find-site-by-uuid (UUID/fromString (:site update-map))))
        study-name (:name (study-db/find-study-by-uuid (UUID/fromString (:study update-map))))
        cohort-name (:name (cohort-db/find-cohort-by-uuid (UUID/fromString (:cohort update-map))))
        timepoint-names (map #(:name (timepoint-db/find-timepoint-by-uuid (UUID/fromString %))) (:timepoints update-map))
        kit-name (kit-type-db/get-kit-type-name kit-type-uuid)
        collection-timestamp (:collection-timestamp update-map)
        selected-form-type-fields (form-type-db/get-form-type-fields kit-type-uuid)
        selected-sample-types (sample-type-db/list-sample-types kit-type-uuid)
        unchaged-update-map (select-keys update-map [:kit-id :participant-id :form-type-field-values :air-waybill
                                                     :completing-first-name :completing-last-name :completing-email
                                                     :comments :complete])
        display-map {:site-name                 site-name
                     :study-name                study-name
                     :cohort-name               cohort-name
                     :timepoint-names           timepoint-names
                     :kit-name                  kit-name
                     :collection-date           collection-timestamp
                     :collection-time           collection-timestamp
                     :selected-form-type-fields selected-form-type-fields
                     :selected-sample-types     selected-sample-types
                     :sample-values             (:samples update-map)
                     :archived                  false
                     }]
    (merge display-map unchaged-update-map)))

(defn convert-form-map-to-display-map
  "There are three formats of maps for kits. Form maps for populating edit forms, display maps for generating a view page
  using manifest.cljc, and create/update maps for creating or updating kits.
  This function converts an edit form map to a display map."
  [original-map]
  (let [kit-type-uuid (:kit-type-uuid original-map)
        site-name (:name (site-db/find-site-by-uuid (:site-uuid original-map)))
        study-name (:name (study-db/find-study-by-uuid (:study-uuid original-map)))
        cohort-name (:name (cohort-db/find-cohort-by-uuid (:cohort-uuid original-map)))
        timepoint-names (map :timepoint-name (:timepoints original-map))
        kit-name (kit-type-db/get-kit-type-name kit-type-uuid)
        collection-timestamp (:collection-timestamp original-map)
        selected-form-type-fields (form-type-db/get-form-type-fields kit-type-uuid)
        form-type-field-values (reduce (fn [m v]
                                         (assoc m (keyword (:field-id v)) (:value v))) {} (:form-values original-map))
        selected-sample-types (sample-type-db/list-sample-types kit-type-uuid)
        samples (reduce (fn [m s] (assoc m (:sample-type-uuid s) s)) {} (:samples original-map))
        air-waybill (:air-waybill (first (:shipments original-map)))
        unchaged-map-entries (select-keys original-map [:kit-id :participant-id :completing-first-name
                                                        :completing-last-name :completing-email :comments :complete
                                                        :archived])
        display-map {:site-name                 site-name
                     :study-name                study-name
                     :cohort-name               cohort-name
                     :timepoint-names           timepoint-names
                     :kit-name                  kit-name
                     :collection-date           collection-timestamp
                     :collection-time           collection-timestamp
                     :selected-form-type-fields selected-form-type-fields
                     :form-type-field-values    form-type-field-values
                     :selected-sample-types     selected-sample-types
                     :sample-values             samples
                     :air-waybill               air-waybill
                     :archived                  false
                     }]
    (merge display-map unchaged-map-entries)))

(defn get-proposed-kit-edit-for-display
  [uuid]
  (let [proposed-edit (first (proposed-kit-edit-db/list-proposed-edits {:uuid uuid}))
        tx-id (proposed-kit-edit-db/get-proposed-edit-tx-id uuid)
        unedited-map (first (kit-shipment/list-kit-shipment {:uuid (:kit-uuid proposed-edit) :tx-id tx-id}))
        unedited-display-map (convert-form-map-to-display-map unedited-map)
        update-map (edn/read-string (:update-map proposed-edit))
        updated-display-map (convert-update-map-to-display-map update-map)]
    {:original-map unedited-display-map :update-map updated-display-map :status (:status proposed-edit)}))

(defn get-proposed-kit-edit
  [uuid]
  (first (proposed-kit-edit-db/list-proposed-edits {:uuid uuid})))

(defn approve-proposed-kit-edit
  [uuid reviewing-user]
  (let [proposed-edit (first (proposed-kit-edit-db/list-proposed-edits {:uuid uuid}))
        update-map (edn/read-string (:update-map proposed-edit))]
    (kit-shipment/update-kit-shipment-with-history (:kit-uuid proposed-edit) reviewing-user update-map)
    (proposed-kit-edit-db/approve-proposed-edit uuid reviewing-user)))

(defn deny-proposed-kit-edit
  [uuid reviewing-user]
  (proposed-kit-edit-db/deny-proposed-edit uuid reviewing-user))

(defn kit-has-pending-edits
  [uuid]
  (> (count (proposed-kit-edit-db/list-proposed-edits {:uuid uuid :status "pending"})) 0))

(defn convert-update-map-form-values
  [kit-type form-type-field-values]
  (let [form-type-fields (form-type-db/get-form-type-fields (UUID/fromString kit-type))
        updated-form-values form-type-field-values]
    (map (fn [field-key]
           (let [field (first (filter #(= (name field-key) (:field-id %)) form-type-fields))
                 value (get updated-form-values field-key)
                 field-type (:type field)
                 field-id (:field-id field)]
             (-> {}
                 (assoc :field-id field-id)
                 (assoc :value value)
                 (assoc :field-type field-type))))
         (keys updated-form-values))))

(defn convert-update-map-to-form-map
  " There are three formats of maps for kits. Form maps for populating edit forms, display maps for generating a view page
  using manifest.cljc, and create/update maps for creating or updating kits.
  This function converts an update map to an edit form map."
  [proposed-edit]
  (let [proposed-edit-uuid (:uuid proposed-edit)
        proposed-edit-email (:email proposed-edit)
        proposed-edit-time (:time proposed-edit)
        update-map (edn/read-string (:update-map proposed-edit))
        {:keys [samples timepoints form-type-field-values air-waybill site kit-type cohort study]} update-map
        form-samples (map (fn [sample-type-uuid]
                            (let [sample (get samples sample-type-uuid)]
                              (assoc sample :sample-type-uuid sample-type-uuid)))
                          (keys samples))
        form-timepoints (map #(assoc {} :uuid %) timepoints)
        shipments [{:air-waybill air-waybill}]
        form-values (convert-update-map-form-values kit-type form-type-field-values)
        unchaged-map-entries (select-keys update-map [:timezone :completing-last-name :collection-timestamp
                                                      :completing-email :comments :completing-first-name
                                                      :participant-id :kit-id :uuid])
        form-map {:pending-edits [{:uuid proposed-edit-uuid :email proposed-edit-email :time proposed-edit-time}]
                  :samples       form-samples
                  :timepoints    form-timepoints
                  :shipments     shipments
                  :site-uuid     site
                  :kit-type-uuid kit-type
                  :cohort-uuid   cohort
                  :form-values   form-values
                  :study-uuid    study}]
    (merge unchaged-map-entries form-map)))

(defn get-kit-or-proposed-edit
  "Gets a kit map if there is no pending proposed edit, otherwise gets a map of the pending proposed edit. When using
  this function you should get the kit from kit-shipment first so that the email filtering from the config-map is used."
  [config-map]
  (let [kit (first (kit-shipment/list-kit-shipment config-map))]
    (if (= (count (:pending-edits kit)) 0)
      kit
      (let [pending-proposed-edit (first (proposed-kit-edit-db/list-proposed-edits {:kit-uuid (:uuid kit) :status "pending"}))]
        (convert-update-map-to-form-map pending-proposed-edit)))))