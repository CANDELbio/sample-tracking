(ns org.parkerici.sample-tracking.db.kit
  (:require [org.parkerici.sample-tracking.db.datomic :as d]
            [org.parkerici.sample-tracking.db.core :as db]
            [java-time :as time]
            [clojure.string :as str])
  (:import (java.util UUID)))

(defn create-or-update-kit
  [uuid kit-map]
  (let [uuid-to-return (or uuid (db/squuid))
        {:keys [kit-id participant-id collection-timestamp timezone
                completing-first-name completing-last-name completing-email comments site cohort timepoints kit-type
                complete submission-timestamp]} kit-map
        txn (cond-> {:kit/kit-id                kit-id
                     :kit/complete              complete
                     :kit/submission-timestamp  submission-timestamp
                     :kit/participant-id        participant-id
                     :kit/timezone              timezone
                     :kit/completing-first-name completing-first-name
                     :kit/completing-last-name  completing-last-name
                     :kit/completing-email      completing-email
                     :kit/comments              comments
                     :kit/site                  [:site/uuid (UUID/fromString site)]
                     :kit/cohort                [:cohort/uuid (UUID/fromString cohort)]
                     :kit/timepoints            (map #(vector :timepoint/uuid (UUID/fromString %)) timepoints)
                     :kit/kit-type              [:kit-type/uuid (UUID/fromString kit-type)]}
                    uuid (assoc :db/id [:kit/uuid uuid])
                    (nil? uuid) (assoc :kit/uuid uuid-to-return)
                    collection-timestamp (assoc :kit/collection-timestamp (time/java-date collection-timestamp)))]
    (db/transact [txn])
    uuid-to-return))

(defn add-sample-to-kit
  [kit-uuid sample-uuid]
  (d/transact [[:db/add [:kit/uuid kit-uuid] :kit/samples [:sample/uuid sample-uuid]]]))

(defn add-shipment-to-kit
  [kit-uuid shipment-uuid]
  (d/transact [[:db/add [:kit/uuid kit-uuid] :kit/shipments [:shipment/uuid shipment-uuid]]]))

(defn add-form-value-to-kit
  [kit-uuid form-value-uuid]
  (d/transact [[:db/add [:kit/uuid kit-uuid] :kit/form-values [:form-value/uuid form-value-uuid]]]))

(defn remove-timepoint-from-kit
  [kit-uuid timepoint-uuid]
  (d/transact [[:db/retract [:kit/uuid kit-uuid] :kit/timepoints [:timepoint/uuid timepoint-uuid]]]))

; TODO - Convert to a pull
(defn list-kits
  [config-map]
  (let [{:keys [uuid complete archived kit-id completing-email completing-email-domain]} config-map
        query '[:find ?kit-uuid ?kit-id ?participant-id ?collection-timestamp ?timezone ?first-name ?last-name
                ?email ?comments ?kit-type-uuid ?kit-type-name ?site-uuid ?study-uuid ?cohort-uuid ?complete ?archived
                :keys uuid kit-id participant-id collection-timestamp timezone completing-first-name completing-last-name
                completing-email comments kit-type-uuid kit-type-name site-uuid study-uuid cohort-uuid complete archived
                :where [?kit :kit/uuid ?kit-uuid]
                [?kit :kit/kit-id ?kit-id]
                [?kit :kit/complete ?complete]
                [?kit :kit/timezone ?timezone]
                [(get-else $ ?kit :kit/participant-id "") ?participant-id]
                [(get-else $ ?kit :kit/collection-timestamp "") ?collection-timestamp]
                [(get-else $ ?kit :kit/completing-first-name "") ?first-name]
                [(get-else $ ?kit :kit/completing-last-name "") ?last-name]
                [(get-else $ ?kit :kit/completing-email "") ?email]
                [(get-else $ ?kit :kit/comments "") ?comments]
                [(get-else $ ?kit :kit/archived false) ?archived]
                [?kit :kit/kit-type ?kit-type]
                [?kit-type :kit-type/uuid ?kit-type-uuid]
                [?kit-type :kit-type/name ?kit-type-name]
                [?kit :kit/site ?site]
                [?site :site/uuid ?site-uuid]
                [?kit :kit/cohort ?cohort]
                [?cohort :cohort/uuid ?cohort-uuid]
                [?cohort :cohort/study ?study]
                [?study :study/uuid ?study-uuid]]
        query-fn (if-let [tx-id (:tx-id config-map)] (d/q-as-of tx-id) d/q-latest)
        results (query-fn query)]
    (cond-> results
            uuid (db/reducer-filter :uuid uuid)
            (some? completing-email) (db/reducer-filter :completing-email completing-email)
            (some? completing-email-domain) (db/reduce-fn-filter (fn [map] (str/ends-with? (:completing-email map) completing-email-domain)))
            (some? kit-id) (db/reducer-filter :kit-id kit-id)
            (some? complete) (db/reducer-filter :complete complete)
            (some? archived) (db/reducer-filter :archived (boolean archived)))))

(defn get-kit
  [config-map]
  (first (list-kits config-map)))

(defn get-kit-vendor-email
  [uuid]
  (let [query '[:find ?vendor-email
                :in $ ?kit-uuid
                :where
                [?kit :kit/uuid ?kit-uuid]
                [?kit :kit/kit-type ?kit-type]
                [?kit-type :kit-type/vendor-email ?vendor-email]]]
    (ffirst (d/q-latest query uuid))))

(defn set-archived
  [uuid archived]
  (db/transact [{:db/id [:kit/uuid uuid] :kit/archived archived}]))