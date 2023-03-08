(ns org.parkerici.sample-tracking.db.history
  (:require [org.parkerici.sample-tracking.db.datomic :as d]
            [org.parkerici.sample-tracking.db.core :as db])
  (:import (java.util Date)))

(defn create-history
  [agent-email entity-type entity-id old-value new-value]
  (let [uuid (db/squuid)
        txn {:history/uuid        uuid
             :history/agent-email agent-email
             :history/entity-type entity-type
             :history/entity-uuid entity-id
             :history/old-value   old-value
             :history/new-value   new-value
             :history/time        (Date.)}]
    (db/transact [txn])
    uuid))

; Pull version to use at some point. Didn't use before uuids because :as doesn't work with :db/id and returns
; each entity as it's own list for some reason.
;
;(defn list-history
;  [id]
;  (let [query {:find  '[(pull ?history [[:db/id :as :id] [:history/agent-email :as :agent-email]
;                                        [:history/entity-type :as :entity-type] [:history/entity-id :as :entity-id]
;                                        [:history/old-value :as :old-value] [:history/new-value :as :new-value]
;                                        [:history/time :as :time]])]
;               :where '[[?history :history/entity-id ?entity-id]]}
;        filtered-query (if (nil? id)
;                         query
;                         (assoc query :in '[$ ?entity-id]))]
;    (apply d/q-latest (remove nil? [filtered-query id]))))

(defn list-history
  [entity-uuid]
  (let [query {:find  '[?history-uuid ?agent-email ?entity-type ?entity-uuid ?old-value ?new-value ?time ?tx-inst]
               :keys  '[uuid agent-email entity-type entity-uuid old-value new-value time sort-time]
               :where '[[?history :history/uuid ?history-uuid ?tx-eid]
                        [?history :history/agent-email ?agent-email]
                        [?history :history/entity-type ?entity-type]
                        [?history :history/entity-uuid ?entity-uuid]
                        [?history :history/old-value ?old-value]
                        [?history :history/new-value ?new-value]
                        [?history :history/time ?time]
                        [?tx-eid :db/txInstant ?tx-inst]]}
        filtered-query (if (nil? entity-uuid)
                         query
                         (assoc query :in '[$ ?entity-uuid]))
        results (apply d/q-latest (remove nil? [filtered-query entity-uuid]))
        sorted-results (reverse (sort-by :sort-time results))]
    (map #(dissoc % :sort-time) sorted-results)))