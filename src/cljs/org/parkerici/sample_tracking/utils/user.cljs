(ns org.parkerici.sample-tracking.utils.user
  (:require [org.parkerici.sample-tracking.utils.collection :as coll]))

(defn current-user-roles
  [user]
  (:roles user))

; Currently the mapping for these string values are kept in application.edn under roles
; TODO - Figure out how to load resources in cljs and refactor aero configuration loading from clj to cljc to use here.

(defn user-is-admin
  [user]
  (coll/in? (current-user-roles user) "administrator"))

(defn user-is-editor
  [user]
  (coll/in? (current-user-roles user) "editor"))

(defn user-is-viewer
  [user]
  (coll/in? (current-user-roles user) "viewer"))

(defn user-is-site-admin
  [user]
  (coll/in? (current-user-roles user) "site-admin"))

(defn user-is-site-coordinator
  [user]
  (coll/in? (current-user-roles user) "site-coordinator"))

(defn user-is-site-user
  [user]
  (or (user-is-site-admin user) (user-is-site-coordinator user)))