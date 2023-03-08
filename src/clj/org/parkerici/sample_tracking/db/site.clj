(ns org.parkerici.sample-tracking.db.site
  (:require [org.parkerici.sample-tracking.db.datomic :as d]
            [org.parkerici.sample-tracking.db.core :as db])
  (:import (java.util Date)))

(defn create-site
  [name]
  (let [uuid (db/squuid)
        txn {:site/name name :site/uuid uuid :site/create-time (Date.)}]
    (db/transact [txn])
    uuid))

(defn find-site-by-name
  [name]
  (first (d/q-latest '[:find ?site-uuid ?create-time
                       :keys uuid create-time
                       :in $ ?site-name
                       :where
                       [?site :site/uuid ?site-uuid]
                       [?site :site/name ?site-name]
                       [?site :site/create-time ?create-time]]
                     name)))

(defn find-site-by-uuid
  [uuid]
  (first (d/q-latest '[:find ?site-name ?create-time
                       :keys name create-time
                       :in $ ?uuid
                       :where
                       [?site :site/uuid ?uuid]
                       [?site :site/name ?site-name]
                       [?site :site/create-time ?create-time]]
                     uuid)))

(defn list-sites
  [study-uuid active]
  (let [query {:find  '[?site-uuid ?site-name ?active ?create-time]
               :keys  '[uuid name active create-time]
               :where '[[?study :study/uuid ?study-uuid]
                        [?study :study/sites ?site-tuple]
                        [(untuple ?site-tuple) [?site ?active]]
                        [?site :site/name ?site-name]
                        [?site :site/uuid ?site-uuid]
                        [?site :site/create-time ?create-time]]}
        filtered-query (if (nil? active)
                         (assoc query :in '[$ ?study-uuid])
                         (assoc query :in '[$ ?study-uuid ?active]))]
    (apply d/q-latest (remove nil? [filtered-query study-uuid active]))))

(defn list-study-tuples
  []
  (let [query {:find  '[?study-uuid ?site-tuple]
               :keys  '[uuid tuple]
               :where '[[?study :study/uuid ?study-uuid]
                        [?study :study/sites ?site-tuple]]}]
    (apply d/q-latest (remove nil? [query]))))

(defn list-all-sites
  []
  (let [query {:find  '[?site ?site-uuid ?site-name]
               :keys  '[id uuid name]
               :where '[[?site :site/uuid ?site-uuid]
                        [?site :site/name ?site-name]]}]
    (apply d/q-latest (remove nil? [query]))))

(defn update-site
  [uuid name]
  (d/transact [{:db/id     [:site/uuid uuid]
                :site/name name}]))

(defn update-site-active-status
  [study-uuid site-uuid status]
  (let [current-tuple (first (d/q-latest '[:find ?site ?active
                                           :in $ ?site-uuid ?study-uuid
                                           :where
                                           [?study :study/uuid ?study-uuid]
                                           [?study :study/sites ?site-tuple]
                                           [(untuple ?site-tuple) [?site ?active]]
                                           [?site :site/uuid ?site-uuid]]
                                         site-uuid study-uuid))]
    (when (some? current-tuple) (d/transact [[:db/retract [:study/uuid study-uuid] :study/sites current-tuple]]))
    (d/transact [[:db/add [:study/uuid study-uuid] :study/sites [[:site/uuid site-uuid] status]]])))
