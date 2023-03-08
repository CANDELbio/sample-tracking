(ns org.parkerici.sample-tracking.db.timepoint
  (:require [org.parkerici.sample-tracking.db.datomic :as d]
            [org.parkerici.sample-tracking.db.core :as db]))

(defn create-timepoint
  [name]
  (let [uuid (db/squuid)
        txn {:timepoint/name name :timepoint/uuid uuid}]
    (db/transact [txn])
    uuid))

; There should only be one timepoint with a given name.
; Returns the ID of the timepoint with the passed in name if it exists.
(defn find-timepoint-uuid-from-name
  [name]
  (ffirst (d/q-latest '[:find ?timepoint-uuid
                        :in $ ?timepoint-name
                        :where
                        [?timepoint :timepoint/name ?timepoint-name]
                        [?timepoint :timepoint/uuid ?timepoint-uuid]]
                      name)))

(defn find-timepoint-by-uuid
  [uuid]
  (first (d/q-latest '[:find ?timepoint-name
                       :keys name
                       :in $ ?uuid
                       :where
                       [?timepoint :timepoint/uuid ?uuid]
                       [?timepoint :timepoint/name ?timepoint-name]]
                     uuid)))

(defn list-sorted-kit-type-timepoints
  "Gets the timepoints associated with a given kit type
  in the order they were associated with that kit type"
  [kit-type-uuid]
  (let [results (d/q-latest '[:find ?timepoint-uuid ?timepoint-name ?tx-inst
                              :keys uuid name sort-time
                              :in $ ?kit-type-uuid
                              :where
                              [?kit-type :kit-type/uuid ?kit-type-uuid]
                              [?kit-type :kit-type/timepoints ?timepoint ?tx-eid]
                              [?tx-eid :db/txInstant ?tx-inst]
                              [?kit-type :kit-type/timepoints ?timepoint]
                              [?timepoint :timepoint/name ?timepoint-name]
                              [?timepoint :timepoint/uuid ?timepoint-uuid]]
                            kit-type-uuid)
        sorted-results (sort-by :sort-time results)]
    (map #(dissoc % :sort-time) sorted-results)))

(defn update-timepoint
  [uuid name]
  (d/transact [{:db/id          [:timepoint/uuid uuid]
                :timepoint/name name}]))

(defn list-kit-timepoints
  [config-map]
  (let [kit-uuid (:uuid config-map)
        query {:find  '[?timepoint-uuid ?timepoint-name ?kit-uuid]
               :keys  '[uuid timepoint-name kit-uuid]
               :where '[[?kit :kit/uuid ?kit-uuid]
                        [?kit :kit/timepoints ?timepoint]
                        [?timepoint :timepoint/name ?timepoint-name]
                        [?timepoint :timepoint/uuid ?timepoint-uuid]]}
        query-fn (if-let [tx-id (:tx-id config-map)] (d/q-as-of tx-id) d/q-latest)
        results (query-fn query)]
    (cond-> results
            kit-uuid (db/reducer-filter :kit-uuid kit-uuid))))