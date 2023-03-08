(ns org.parkerici.sample-tracking.db.study
  (:require [org.parkerici.sample-tracking.db.core :as db]
            [org.parkerici.sample-tracking.db.datomic :as d])
  (:import (java.util Date)))

(defn create-study
  [name]
  (let [uuid (db/squuid)
        txn {:study/name name :study/uuid uuid :study/active true :study/create-time (Date.)}]
    (db/transact [txn])
    uuid))

(defn find-study-by-name
  [name]
  (first (d/q-latest '[:find ?study-uuid ?active ?create-time
                       :keys uuid active create-time
                       :in $ ?study-name
                       :where
                       [?study :study/uuid ?study-uuid]
                       [?study :study/name ?study-name]
                       [?study :study/active ?active]
                       [?study :study/create-time ?create-time]]
                     name)))

(defn find-study-by-uuid
  [uuid]
  (first (d/q-latest '[:find ?study-name ?active ?create-time
                       :keys name active create-time
                       :in $ ?uuid
                       :where
                       [?study :study/uuid ?uuid]
                       [?study :study/name ?study-name]
                       [?study :study/active ?active]
                       [?study :study/create-time ?create-time]]
                     uuid)))

(defn add-site-to-study
  [site-uuid study-name]
  (d/transact [[:db/add [:study/name study-name] :study/sites [[:site/uuid site-uuid] true]]]))

(defn site-is-associated-with-study
  [site-uuid study-name]
  (seq (d/q-latest '[:find ?study
                     :in $ ?site-uuid ?study-name
                     :where
                     [?study :study/name ?study-name]
                     [?study :study/sites ?site-tuple]
                     [(untuple ?site-tuple) [?site ?active]]
                     [?site :site/uuid ?site-uuid]]
                   site-uuid study-name)))

(defn add-cohort-to-study
  [study-uuid cohort-uuid]
  (d/transact [[:db/add [:study/uuid study-uuid] :study/cohorts [[:cohort/uuid cohort-uuid] true]]]))

(defn add-participant-id-validation-to-study
  [study-name prefix regex message]
  (d/transact [{:db/id                                   [:study/name study-name]
                :study/participant-id-prefix             prefix
                :study/participant-id-regex              regex
                :study/participant-id-validation-message message}]))

(defn add-kit-id-validation-to-study
  [study-name prefix regex message]
  (d/transact [{:db/id                           [:study/name study-name]
                :study/kit-id-prefix             prefix
                :study/kit-id-regex              regex
                :study/kit-id-validation-message message}]))

(defn list-studies
  [active]
  (let [query {:find  '[?uuid ?study-name ?active ?create-time ?participant-id-prefix ?participant-id-regex
                        ?participant-id-validation-message ?kit-id-prefix ?kit-id-regex ?kit-id-validation-message]
               :keys  '[uuid name active create-time participant-id-prefix participant-id-regex
                        participant-id-validation-message kit-id-prefix kit-id-regex kit-id-validation-message]
               :where '[[?study :study/name ?study-name]
                        [?study :study/active ?active]
                        [?study :study/create-time ?create-time]
                        [?study :study/uuid ?uuid]
                        [(get-else $ ?study :study/participant-id-prefix "") ?participant-id-prefix]
                        [(get-else $ ?study :study/participant-id-regex "") ?participant-id-regex]
                        [(get-else $ ?study :study/participant-id-validation-message "") ?participant-id-validation-message]
                        [(get-else $ ?study :study/kit-id-prefix "") ?kit-id-prefix]
                        [(get-else $ ?study :study/kit-id-regex "") ?kit-id-regex]
                        [(get-else $ ?study :study/kit-id-validation-message "") ?kit-id-validation-message]]}
        filtered-query (if (nil? active)
                         query
                         (assoc query :in '[$ ?active]))]
    (apply d/q-latest (remove nil? [filtered-query active]))))

(defn cohort-associated-with-study
  [study-uuid cohort-uuid]
  (seq (d/q-latest '[:find ?cohort ?study
                     :in $ ?cohort-uuid ?study-uuid
                     :where
                     [?study :study/uuid ?study-uuid]
                     [?study :study/cohorts ?cohort-tuple]
                     [(untuple ?cohort-tuple) [?cohort ?active]]
                     [?cohort :cohort/uuid ?cohort-uuid]]
                   cohort-uuid study-uuid)))

(defn update-study
  [uuid name]
  (d/transact [{:db/id      [:study/uuid uuid]
                :study/name name}]))

(defn update-study-active-status
  [uuid status]
  (d/transact [{:db/id        [:study/uuid uuid]
                :study/active status}]))