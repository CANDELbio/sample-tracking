(ns org.parkerici.sample-tracking.db.migration
  (:require [org.parkerici.sample-tracking.db.datomic :as d]
            [org.parkerici.sample-tracking.db.core :as db])
  (:import (java.util Date)))

(defn create-migration
  [name]
  (let [uuid (db/squuid)]
    (db/transact [{:migration/uuid uuid :migration/name name :migration/time (Date.)}])
    uuid))

(defn migration-has-been-run
  [name]
  (let [migrations (d/q-latest '[:find ?uuid
                                 :in $ ?migration-name
                                 :where
                                 [?migration :migration/name ?migration-name]
                                 [?migration :migration/uuid ?uuid]]
                               name)]
    (not= (count migrations) 0)))