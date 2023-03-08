(ns org.parkerici.sample-tracking.db.core
  (:require [org.parkerici.sample-tracking.db.datomic :as d]
            [clojure.core.reducers :as r])
  (:import (java.util UUID)))

(defn squuid []
  (let [uuid (UUID/randomUUID)
        time (System/currentTimeMillis)
        secs (quot time 1000)
        lsb (.getLeastSignificantBits uuid)
        msb (.getMostSignificantBits uuid)
        timed-msb (bit-or (bit-shift-left secs 32)
                          (bit-and 0x00000000ffffffff msb))]
    (UUID. timed-msb lsb)))

(defn remove-nil-map-values
  [map]
  (into {} (filter #(not (nil? (second %))) map)))

(defn reduce-fn-filter
  [map-coll fn]
  (into [] (r/filter fn map-coll)))

(defn reducer-filter
  [map-coll key value]
  (reduce-fn-filter map-coll (fn [map] (= (get map key) value))))

; Convenience function. Assumes that the passed in transaction will create/modify one entity.
; Runs the transaction and returns the id of the created entity.
(defn transact-and-return-id
  [txn]
  (let [transaction-results (d/transact (map remove-nil-map-values txn))]
    (first (vals (:tempids transaction-results)))))

(defn transact
  [txn]
  (d/transact (map remove-nil-map-values txn)))

(defn retract-entities
  [db-ids]
  (let [txns (for [db-id db-ids] [:db/retractEntity db-id])]
    (d/transact txns)))