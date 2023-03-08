(ns org.parkerici.sample-tracking.db.shipment
  (:require [org.parkerici.sample-tracking.db.datomic :as d]
            [org.parkerici.sample-tracking.db.core :as db]))

(defn create-or-update-shipment
  [uuid air-waybill]
  (let [uuid-to-return (or uuid (db/squuid))
        txn (cond-> {:shipment/air-waybill air-waybill}
                    uuid (assoc :db/id [:shipment/uuid uuid])
                    (nil? uuid) (assoc :shipment/uuid uuid-to-return))]
    (db/transact [txn])
    uuid-to-return))

; Gets the shipments explicitly associated with a kit
(defn list-shipments
  [config-map]
  (let [kit-uuid (:uuid config-map)
        query {:find  '[?shipment-uuid ?air-waybill ?kit-uuid]
               :keys  '[uuid air-waybill kit-uuid]
               :where '[[?kit :kit/uuid ?kit-uuid]
                        [?kit :kit/shipments ?shipment]
                        [?shipment :shipment/uuid ?shipment-uuid]
                        [?shipment :shipment/air-waybill ?air-waybill]]}
        query-fn (if-let [tx-id (:tx-id config-map)] (d/q-as-of tx-id) d/q-latest)
        results (query-fn query)]
    (cond-> results
            kit-uuid (db/reducer-filter :kit-uuid kit-uuid))))

(defn set-archived
  [uuid archived]
  (db/transact [{:db/id [:shipment/uuid uuid] :shipment/archived archived}]))
