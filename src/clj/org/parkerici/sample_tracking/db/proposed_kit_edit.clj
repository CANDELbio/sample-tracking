(ns org.parkerici.sample-tracking.db.proposed-kit-edit
  (:require [org.parkerici.sample-tracking.db.datomic :as d]
            [org.parkerici.sample-tracking.db.core :as db]
            [clojure.string :as str])
  (:import (java.util Date)))

(defn create-or-update-proposed-edit
  "kit-uuid must be parsed uuid and not a string"
  [edit-uuid kit-uuid kit-map email]
  (let [uuid (or edit-uuid (db/squuid))
        txn {:proposed-kit-edit/uuid       uuid
             :proposed-kit-edit/kit        [:kit/uuid kit-uuid]
             :proposed-kit-edit/update-map kit-map
             :proposed-kit-edit/status     :kit-edit-status/pending
             :proposed-kit-edit/user       [:user/email email]
             :proposed-kit-edit/time       (Date.)}]
    (db/transact [txn])
    uuid))

(defn list-proposed-edits
  [config-map]
  (let [{:keys [uuid kit-uuid status]} config-map
        query {:find  '[?uuid ?kit-uuid ?kit-id ?update-map ?status ?email ?time ?participant-id ?collection-timestamp
                        ?kit-email ?kit-type-name]
               :keys  '[uuid kit-uuid kit-id update-map status email time participant-id collection-timestamp
                        kit-email kit-type-name]
               :where '[[?proposed-edit :proposed-kit-edit/uuid ?uuid]
                        [?proposed-edit :proposed-kit-edit/kit ?kit]
                        [?kit :kit/uuid ?kit-uuid]
                        [?kit :kit/kit-id ?kit-id]
                        [(get-else $ ?kit :kit/participant-id "") ?participant-id]
                        [(get-else $ ?kit :kit/collection-timestamp "") ?collection-timestamp]
                        [(get-else $ ?kit :kit/completing-email "") ?kit-email]
                        [?kit :kit/kit-type ?kit-type]
                        [?kit-type :kit-type/name ?kit-type-name]
                        [?proposed-edit :proposed-kit-edit/update-map ?update-map]
                        [?proposed-edit :proposed-kit-edit/status ?status-enum]
                        [?status-enum :db/doc ?status]
                        [?proposed-edit :proposed-kit-edit/user ?user]
                        [?user :user/email ?email]
                        [?proposed-edit :proposed-kit-edit/time ?time]]}
        results (apply d/q-latest [query])]
    (cond-> results
            (some? uuid) (db/reducer-filter :uuid uuid)
            (some? kit-uuid) (db/reducer-filter :kit-uuid kit-uuid)
            (not (str/blank? status)) (db/reducer-filter :status status))))

(defn get-proposed-edit-tx-id
  [uuid]
  (ffirst (d/q-latest '[:find ?tx
                        :in $ ?uuid
                        :where
                        [?form-type :proposed-kit-edit/uuid ?uuid ?tx ?op]]
                      uuid)))

(defn update-proposed-edit-status
  [uuid reviewing-email status]
  (let [txn {:proposed-kit-edit/uuid           uuid
             :proposed-kit-edit/status         status
             :proposed-kit-edit/reviewing-user [:user/email reviewing-email]}]
    (db/transact [txn])
    uuid))

(defn approve-proposed-edit
  [uuid reviewing-email]
  (update-proposed-edit-status uuid reviewing-email :kit-edit-status/approved))

(defn deny-proposed-edit
  [uuid reviewing-email]
  (update-proposed-edit-status uuid reviewing-email :kit-edit-status/denied))