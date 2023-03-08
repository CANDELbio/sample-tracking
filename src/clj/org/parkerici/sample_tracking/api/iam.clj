(ns org.parkerici.sample-tracking.api.iam
  (:require [org.parkerici.sample-tracking.db.user :as user-db]
            [org.parkerici.sample-tracking.db.role :as role-db]))

(defn find-or-create-user
  [email]
  (let [user-uuid (user-db/find-user-uuid email)]
    (if user-uuid
      user-uuid
      (user-db/create-user email))))

(defn find-or-create-role
  [name]
  (let [role-uuid (role-db/find-role-uuid name)]
    (if role-uuid
      role-uuid
      (role-db/create-role name))))

(defn add-role-to-user
  [email role-name]
  (let [user-uuid (find-or-create-user email)
        role-uuid (find-or-create-role role-name)]
    (if (user-db/user-has-role user-uuid role-uuid)
      (throw (Exception. "User already has role."))
      (user-db/add-role-to-user user-uuid role-uuid))))

(defn remove-role-from-user
  [email role-name]
  (let [user-uuid (find-or-create-user email)
        role-uuid (find-or-create-role role-name)]
    (if (user-db/user-has-role user-uuid role-uuid)
      (user-db/remove-role-from-user user-uuid role-uuid)
      (throw (Exception. "User does not have role to remove.")))))

(defn get-users-roles
  [email]
  (doall (map first (user-db/get-users-roles email))))

(defn get-users-with-role
  [role-name]
  (doall (map first (user-db/get-users-with-role role-name))))

(defn get-user
  [email]
  (first (user-db/list-users {:email email})))

(defn reactivate-user
  [email]
  (user-db/set-user-deactivated-status email false)
  (find-or-create-user email))

(defn deactivate-user
  [email]
  (let [user-uuid (user-db/find-user-uuid email)]
    (user-db/set-user-deactivated-status email true)
    (doseq [role (role-db/list-roles)]
      (when (user-db/user-has-role user-uuid (:uuid role))
        (user-db/remove-role-from-user user-uuid (:uuid role))))
    user-uuid))