(ns org.parkerici.sample-tracking.db.kit-type
  (:require [org.parkerici.sample-tracking.db.datomic :as d]
            [org.parkerici.sample-tracking.db.core :as db])
  (:import (java.util Date)))

(defn create-kit-type
  [name item-number vendor-email collection-date-required air-waybill-required]
  (let [uuid (db/squuid)
        txn {:kit-type/name                     name
             :kit-type/item-number              item-number
             :kit-type/vendor-email             vendor-email
             :kit-type/collection-date-required collection-date-required
             :kit-type/air-waybill-required     air-waybill-required
             :kit-type/uuid                     uuid
             :kit-type/create-time              (Date.)}]
    (db/transact [txn])
    uuid))

(defn find-active-kit-type-by-name-and-cohort
  [kit-type-name cohort-uuid]
  (first (d/q-latest '[:find ?kit-type-uuid
                       :keys uuid
                       :in $ ?kit-type-name ?cohort-uuid ?active
                       :where
                       [?cohort :cohort/uuid ?cohort-uuid]
                       [?cohort :cohort/kit-types ?kit-type-tuple]
                       [(untuple ?kit-type-tuple) [?kit-type ?active]]
                       [?kit-type :kit-type/name ?kit-type-name]
                       [?kit-type :kit-type/uuid ?kit-type-uuid]] kit-type-name cohort-uuid true)))

;TODO - Convert to pull
(defn find-kit-type-by-uuid
  [uuid]
  (first (d/q-latest '[:find ?name ?item-number ?create-time
                       :keys name item-number create-time
                       :in $ ?uuid
                       :where
                       [?kit-type :kit-type/uuid ?uuid]
                       [?kit-type :kit-type/name ?name]
                       [?kit-type :kit-type/item-number ?item-number]
                       [?kit-type :kit-type/create-time ?create-time]]
                     uuid)))

(defn add-timepoint-to-kit-type
  [timepoint-uuid kit-type-uuid]
  (d/transact [[:db/add [:kit-type/uuid kit-type-uuid] :kit-type/timepoints [:timepoint/uuid timepoint-uuid]]]))

(defn add-form-type-to-kit-type
  [form-type-uuid kit-type-item-no]
  (d/transact [{:db/id [:kit-type/item-number kit-type-item-no] :kit-type/form-type [:form-type/uuid form-type-uuid]}]))

(defn add-sample-type-to-kit-type
  [sample-type-uuid kit-type-uuid]
  (d/transact [[:db/add [:kit-type/uuid kit-type-uuid] :kit-type/sample-types [:sample-type/uuid sample-type-uuid]]]))

(defn kit-type-has-form-type
  [kit-type-item-no]
  (seq (d/q-latest '[:find ?form-type
                     :in $ ?item-number
                     :where
                     [?kit-type :kit-type/item-number ?item-number]
                     [?kit-type :kit-type/form-type ?form-type]]
                   kit-type-item-no)))

(defn get-kit-type-form-type
  [kit-type-uuid]
  (first (d/q-latest '[:find ?form-type-uuid ?form-type-name ?form-type-fields
                       :keys uuid name fields
                       :in $ ?kit-type-uuid
                       :where
                       [?kit-type :kit-type/uuid ?kit-type-uuid]
                       [?kit-type :kit-type/form-type ?form-type]
                       [?form-type :form-type/uuid ?form-type-uuid]
                       [?form-type :form-type/name ?form-type-name]
                       [?form-type :form-type/fields ?form-type-fields]]
                     kit-type-uuid)))

(defn list-kit-types
  [cohort-uuid active]
  (let [query {:find  '[?kit-type-uuid ?kit-type-name ?item-number ?active ?create-time ?collection-date-required
                        ?air-waybill-required]
               :keys  '[uuid name item-number active create-time collection-date-required air-waybill-required]
               :where '[[?cohort :cohort/uuid ?cohort-uuid]
                        [?cohort :cohort/kit-types ?kit-type-tuple]
                        [(untuple ?kit-type-tuple) [?kit-type ?active]]
                        [?kit-type :kit-type/uuid ?kit-type-uuid]
                        [?kit-type :kit-type/timepoints ?timepoints]
                        [?kit-type :kit-type/name ?kit-type-name]
                        [?kit-type :kit-type/item-number ?item-number]
                        [(get-else $ ?kit-type :kit-type/form-type "") ?form-type]
                        [?kit-type :kit-type/create-time ?create-time]
                        [?kit-type :kit-type/collection-date-required ?collection-date-required]
                        [?kit-type :kit-type/air-waybill-required ?air-waybill-required]]}
        filtered-query (if (nil? active)
                         (assoc query :in '[$ ?cohort-uuid])
                         (assoc query :in '[$ ?cohort-uuid ?active]))]
    (apply d/q-latest (remove nil? [filtered-query cohort-uuid active]))))

(defn get-kit-type-name
  [uuid]
  (ffirst (d/q-latest '[:find ?kit-type-name
                        :in $ ?uuid
                        :where
                        [?kit-type :kit-type/uuid ?uuid]
                        [?kit-type :kit-type/name ?kit-type-name]]
                      uuid)))

(defn update-kit-type
  [uuid name item-number collection-date-required air-waybill-required]
  (d/transact [{:db/id                             [:kit-type/uuid uuid]
                :kit-type/name                     name
                :kit-type/item-number              item-number
                :kit-type/collection-date-required collection-date-required
                :kit-type/air-waybill-required     air-waybill-required}]))

(defn update-kit-type-active-status
  [cohort-uuid kit-type-uuid status]
  (let [current-tuple (first (d/q-latest '[:find ?kit-type ?active
                                           :in $ ?kit-type-uuid ?cohort-uuid
                                           :where
                                           [?cohort :cohort/uuid ?cohort-uuid]
                                           [?cohort :cohort/kit-types ?kit-type-tuple]
                                           [(untuple ?kit-type-tuple) [?kit-type ?active]]
                                           [?kit-type :kit-type/uuid ?kit-type-uuid]]
                                         kit-type-uuid cohort-uuid))]
    (when (some? current-tuple) (d/transact [[:db/retract [:cohort/uuid cohort-uuid] :cohort/kit-types current-tuple]]))
    (d/transact [[:db/add [:cohort/uuid cohort-uuid] :cohort/kit-types [[:kit-type/uuid kit-type-uuid] status]]])))
