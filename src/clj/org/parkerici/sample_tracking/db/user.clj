(ns org.parkerici.sample-tracking.db.user
  (:require [org.parkerici.sample-tracking.db.datomic :as d]
            [org.parkerici.sample-tracking.db.core :as db]))

(defn create-user
  [email]
  (let [uuid (db/squuid)]
    (db/transact [{:user/email email :user/uuid uuid}])
    uuid))

(defn find-user-uuid
  [email]
  (ffirst (d/q-latest '[:find ?uuid
                        :in $ ?email
                        :where
                        [?user :user/email ?email]
                        [?user :user/uuid ?uuid]]
                      email)))

(defn add-role-to-user
  [user-uuid role-uuid]
  (d/transact [[:db/add [:user/uuid user-uuid] :user/roles [:role/uuid role-uuid]]]))

(defn remove-role-from-user
  [user-uuid role-uuid]
  (d/transact [[:db/retract [:user/uuid user-uuid] :user/roles [:role/uuid role-uuid]]]))

(defn set-user-deactivated-status
  [email status]
  (d/transact [{:db/id            [:user/email email]
                :user/deactivated status}]))

(defn user-has-role
  [user-uuid role-uuid]
  (seq (d/q-latest '[:find ?email
                     :in $ ?user-uuid ?role-uuid
                     :where
                     [?user :user/uuid ?user-uuid]
                     [?user :user/email ?email]
                     [?user :user/roles ?role]
                     [?role :role/uuid ?role-uuid]] user-uuid role-uuid)))

(defn get-users-roles
  [email]
  (d/q-latest '[:find ?role-name
                :in $ ?email
                :where
                [?user :user/email ?email]
                [?user :user/roles ?role]
                [?role :role/name ?role-name]]
              email))

(defn get-users-with-role
  [role-name]
  (d/q-latest '[:find ?email
                :in $ ?role-name
                :where
                [?role :role/name ?role-name]
                [?user :user/roles ?role]
                [?user :user/email ?email]]
              role-name))

(defn list-users
  [options]
  (let [all-users (map first (d/q-latest '[:find (pull ?user [[:user/uuid :as :uuid]
                                                              [:user/email :as :email]
                                                              [:user/deactivated :default false :as :deactivated]
                                                              {[:user/roles :as :roles] [[:role/name :as :name]
                                                                                         [:role/uuid :as :uuid]]}])
                                           :where [?user :user/uuid]]))]
    (if (:email options)
      (db/reducer-filter all-users :email (:email options))
      all-users)))