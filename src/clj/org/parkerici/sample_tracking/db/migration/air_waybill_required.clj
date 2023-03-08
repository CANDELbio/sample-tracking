(ns org.parkerici.sample-tracking.db.migration.air-waybill-required
  (:require [org.parkerici.sample-tracking.db.datomic :as d]
            [org.parkerici.sample-tracking.db.core :as db]))

(defn list-kit-types-without-air-waybill-required
  []
  (d/q-latest {:find  '[?uuid]
               :keys  '[uuid]
               :where '[[?kit-type :kit-type/uuid ?uuid]
                        [(missing? $ ?kit-type :kit-type/air-waybill-required)]]}))

(defn list-kit-types-air-waybill
  []
  (d/q-latest {:find  '[?uuid ?air-waybill]
               :keys  '[uuid air-waybill]
               :where '[[?kit-type :kit-type/uuid ?uuid]
                        [?kit-type :kit-type/air-waybill-required ?air-waybill]]}))


(defn set-kit-type-air-waybill-required
  [uuid required]
  (db/transact [{:db/id [:kit-type/uuid uuid] :kit-type/air-waybill-required required}]))