(ns org.parkerici.sample-tracking.db.sample-type
  (:require [org.parkerici.sample-tracking.db.datomic :as d]
            [org.parkerici.sample-tracking.db.core :as db]))

(defn create-sample-type
  [name id-suffix ships-with-kit reminder]
  (let [uuid (db/squuid)
        txn {:sample-type/name           name
             :sample-type/uuid           uuid
             :sample-type/id-suffix      id-suffix
             :sample-type/ships-with-kit ships-with-kit
             :sample-type/reminder       reminder}]
    (db/transact [txn])
    uuid))

(defn find-sample-type-by-uuid
  [uuid]
  (first (d/q-latest '[:find ?name ?id-suffix ?ships-with-kit ?reminder
                       :keys name id-suffix ships-with-kit reminder
                       :in $ ?uuid
                       :where
                       [?sample-type :sample-type/uuid ?uuid]
                       [?sample-type :sample-type/name ?name]
                       [?sample-type :sample-type/id-suffix ?id-suffix]
                       [?sample-type :sample-type/ships-with-kit ?ships-with-kit]
                       [?sample-type :sample-type/reminder ?reminder]]
                     uuid)))

(defn add-attribute-value-to-sample-type
  [sample-type-uuid value-uuid]
  (d/transact [[:db/add [:sample-type/uuid sample-type-uuid] :sample-type/attribute-values [:sample-attribute-value/uuid value-uuid]]]))

(defn list-sample-types
  [kit-type-uuid]
  (let [results (d/q-latest '[:find ?sample-type-uuid ?sample-type-name ?id-suffix ?ships-with-kit ?reminder ?kit-type-uuid
                              :keys uuid name id-suffix ships-with-kit reminder kit-type-uuid
                              :where
                              [?kit-type :kit-type/uuid ?kit-type-uuid]
                              [?kit-type :kit-type/sample-types ?sample-type]
                              [?sample-type :sample-type/name ?sample-type-name]
                              [?sample-type :sample-type/uuid ?sample-type-uuid]
                              [?sample-type :sample-type/id-suffix ?id-suffix]
                              [?sample-type :sample-type/ships-with-kit ?ships-with-kit]
                              [?sample-type :sample-type/reminder ?reminder]])]
    (cond-> results
            kit-type-uuid (db/reducer-filter :kit-type-uuid kit-type-uuid))))

(defn update-sample-type
  [uuid name id-suffix ships-with-kit reminder]
  (d/transact [{:db/id                      [:sample-type/uuid uuid]
                :sample-type/name           name
                :sample-type/id-suffix      id-suffix
                :sample-type/ships-with-kit ships-with-kit
                :sample-type/reminder       reminder}]))
