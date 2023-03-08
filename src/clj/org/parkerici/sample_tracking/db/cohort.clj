(ns org.parkerici.sample-tracking.db.cohort
  (:require [org.parkerici.sample-tracking.db.datomic :as d]
            [org.parkerici.sample-tracking.db.core :as db])
  (:import (java.util Date)))

(defn create-cohort
  [name study-uuid]
  (let [uuid (db/squuid)
        txn {:cohort/name name :cohort/uuid uuid :cohort/create-time (Date.) :cohort/study [:study/uuid study-uuid]}]
    (db/transact [txn])
    uuid))

(defn add-kit-type-to-cohort
  [cohort-uuid kit-type-uuid]
  (d/transact [[:db/add [:cohort/uuid cohort-uuid] :cohort/kit-types [[:kit-type/uuid kit-type-uuid] true]]]))

; TODO - Try converting to a pull
(defn find-cohort-by-name-and-study
  [cohort-name study-uuid]
  (first (d/q-latest '[:find ?uuid ?create-time
                       :keys uuid create-time
                       :in $ ?cohort-name ?study-uuid
                       :where
                       [?cohort :cohort/uuid ?uuid]
                       [?cohort :cohort/name ?cohort-name]
                       [?cohort :cohort/create-time ?create-time]
                       [?cohort :cohort/study ?study]
                       [?study :study/uuid ?study-uuid]]
                     cohort-name study-uuid)))

(defn find-cohort-by-uuid
  [uuid]
  (first (d/q-latest '[:find ?cohort-name ?create-time
                       :keys name create-time
                       :in $ ?uuid
                       :where
                       [?cohort :cohort/uuid ?uuid]
                       [?cohort :cohort/name ?cohort-name]
                       [?cohort :cohort/create-time ?create-time]]
                     uuid)))

(defn list-cohorts
  [study-uuid active]
  (let [query {:find  '[?cohort-uuid ?cohort-name ?active ?create-time]
               :keys  '[uuid name active create-time]
               :where '[[?study :study/uuid ?study-uuid]
                        [?study :study/cohorts ?cohort-tuple]
                        [(untuple ?cohort-tuple) [?cohort ?active]]
                        [?cohort :cohort/name ?cohort-name]
                        [?cohort :cohort/uuid ?cohort-uuid]
                        [?cohort :cohort/create-time ?create-time]]}
        filtered-query (if (nil? active)
                         (assoc query :in '[$ ?study-uuid])
                         (assoc query :in '[$ ?study-uuid ?active]))]
    (apply d/q-latest (remove nil? [filtered-query study-uuid active]))))

(defn update-cohort
  [uuid name]
  (d/transact [{:db/id       [:cohort/uuid uuid]
                :cohort/name name}]))

(defn update-cohort-active-status
  [study-uuid cohort-uuid status]
  (let [current-tuple (first (d/q-latest '[:find ?cohort ?active
                                           :in $ ?cohort-uuid ?study-uuid
                                           :where
                                           [?study :study/uuid ?study-uuid]
                                           [?study :study/cohorts ?cohort-tuple]
                                           [(untuple ?cohort-tuple) [?cohort ?active]]
                                           [?cohort :cohort/uuid ?cohort-uuid]]
                                         cohort-uuid study-uuid))]
    (when (some? current-tuple) (d/transact [[:db/retract [:study/uuid study-uuid] :study/cohorts current-tuple]]))
    (d/transact [[:db/add [:study/uuid study-uuid] :study/cohorts [[:cohort/uuid cohort-uuid] status]]])))