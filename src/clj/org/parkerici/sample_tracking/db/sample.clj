(ns org.parkerici.sample-tracking.db.sample
  (:require [org.parkerici.sample-tracking.db.datomic :as d]
            [org.parkerici.sample-tracking.db.core :as db])
  (:import (java.util UUID)))

(defn create-or-update-sample
  [uuid sample-type-uuid sample-id collected shipped]
  (let [uuid-to-return (or uuid (db/squuid))
        txn (cond-> {:sample/sample-type [:sample-type/uuid (UUID/fromString sample-type-uuid)]
                     :sample/sample-id   sample-id
                     :sample/collected   collected
                     :sample/shipped     shipped}
                    uuid (assoc :db/id [:sample/uuid uuid])
                    (nil? uuid) (assoc :sample/uuid uuid-to-return))]
    (db/transact [txn])
    uuid-to-return))

(defn add-sample-to-shipment
  [shipment-uuid sample-uuid]
  (d/transact [{:db/id [:sample/uuid sample-uuid] :sample/shipment [:shipment/uuid shipment-uuid]}]))

(defn remove-sample-from-shipment
  [shipment-uuid sample-uuid]
  (d/transact [[:db/retract [:sample/uuid sample-uuid] :sample/shipment [:shipment/uuid shipment-uuid]]]))

(defn list-shipment-samples
  [shipment-uuid]
  (flatten (d/q-latest '[:find ?sample-uuid
                         :in $ ?shipment-uuid
                         :where
                         [?sample :sample/uuid ?sample-uuid]
                         [?sample :sample/shipment ?shipment]
                         [?shipment :shipment/uuid ?shipment-uuid]] shipment-uuid)))

(defn list-samples
  [config-map]
  (let [kit-uuid (:uuid config-map)
        query {:find  '[?kit-uuid ?sample-uuid ?sample-id ?sample-type-uuid ?collected ?shipped]
               :keys  '[kit-uuid uuid sample-id sample-type-uuid collected shipped]
               :where '[[?kit :kit/uuid ?kit-uuid]
                        [?kit :kit/samples ?sample]
                        [?sample :sample/uuid ?sample-uuid]
                        [?sample :sample/sample-type ?sample-type]
                        [?sample :sample/sample-id ?sample-id]
                        [(get-else $ ?sample :sample/collected false) ?collected]
                        [(get-else $ ?sample :sample/shipped false) ?shipped]
                        [?sample-type :sample-type/uuid ?sample-type-uuid]]}
        query-fn (if-let [tx-id (:tx-id config-map)] (d/q-as-of tx-id) d/q-latest)
        results (query-fn query)]
    (cond-> results
            kit-uuid (db/reducer-filter :kit-uuid kit-uuid))))

; TODO - Convert to a pull
(defn list-samples-for-export
  ([config-map]
   (let [{:keys [uuid complete shipped archived]} config-map
         query '[:find ?site-name ?study-name ?cohort-name ?kit-type-name ?kit-type-uuid ?kit-type-item-number ?kit-uuid
                 ?kit-id ?participant-id ?collection-timestamp ?completing-first-name ?completing-last-name
                 ?completing-email ?kit-comments ?sample-type-name ?sample-type-uuid ?sample-id ?collected ?shipped
                 ?air-waybill ?complete ?timezone ?archived
                 :keys site-name study-name cohort-name kit-type-name kit-type-uuid kit-type-item-number kit-uuid kit-id
                 participant-id collection-timestamp completing-first-name completing-last-name completing-email
                 kit-comments sample-type-name sample-type-uuid sample-id collected shipped air-waybill complete
                 timezone archived
                 :where [?kit :kit/kit-id ?kit-id]
                 [?kit :kit/complete ?complete]
                 [?kit :kit/timezone ?timezone]
                 [(get-else $ ?kit :kit/participant-id "") ?participant-id]
                 [(get-else $ ?kit :kit/collection-timestamp "") ?collection-timestamp]
                 [(get-else $ ?kit :kit/completing-first-name "") ?completing-first-name]
                 [(get-else $ ?kit :kit/completing-last-name "") ?completing-last-name]
                 [(get-else $ ?kit :kit/completing-email "") ?completing-email]
                 [(get-else $ ?kit :kit/comments "") ?kit-comments]
                 [(get-else $ ?kit :kit/archived false) ?archived]
                 [?kit :kit/uuid ?kit-uuid]
                 [?kit :kit/kit-type ?kit-type]
                 [?kit-type :kit-type/uuid ?kit-type-uuid]
                 [?kit-type :kit-type/name ?kit-type-name]
                 [?kit-type :kit-type/item-number ?kit-type-item-number]
                 [?kit :kit/site ?site]
                 [?site :site/name ?site-name]
                 [?study :study/name ?study-name]
                 [?kit :kit/cohort ?cohort]
                 [?cohort :cohort/name ?cohort-name]
                 [?cohort :cohort/study ?study]
                 [?kit :kit/samples ?sample]
                 [?sample :sample/sample-type ?sample-type]
                 [?sample-type :sample-type/name ?sample-type-name]
                 [?sample-type :sample-type/uuid ?sample-type-uuid]
                 [?sample :sample/sample-id ?sample-id]
                 [(get-else $ ?sample :sample/collected false) ?collected]
                 [(get-else $ ?sample :sample/shipped false) ?shipped]
                 [(get-else $ ?sample :sample/shipment -1) ?shipment]
                 [(get-else $ ?shipment :shipment/air-waybill "") ?air-waybill]]
         results (d/q-latest query)]
     (cond-> results
             uuid (db/reducer-filter :kit-uuid uuid)
             (some? complete) (db/reducer-filter :complete complete)
             (some? shipped) (db/reducer-filter :shipped shipped)
             (some? archived) (db/reducer-filter :archived (boolean archived)))))
  ([]
   (list-samples-for-export {})))
