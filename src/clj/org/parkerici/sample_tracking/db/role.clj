(ns org.parkerici.sample-tracking.db.role
  (:require [org.parkerici.sample-tracking.db.datomic :as d]
            [org.parkerici.sample-tracking.db.core :as db]))

(defn create-role
  [name]
  (let [uuid (db/squuid)]
    (db/transact [{:role/name name :role/uuid uuid}])
    uuid))

(defn find-role-uuid
  [name]
  (ffirst (d/q-latest '[:find ?uuid
                        :in $ ?name
                        :where
                        [?role :role/name ?name]
                        [?role :role/uuid ?uuid]]
                      name)))

(defn list-roles
  []
  (map first (d/q-latest '[:find (pull ?role [[:role/uuid :as :uuid]
                                              [:role/name :as :name]])
                           :where [?role :role/uuid]])))