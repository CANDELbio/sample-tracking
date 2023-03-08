(ns org.parkerici.sample-tracking.components.user.list.db
  (:require [cljs.spec.alpha :as s]))

(s/def :user-list/users vector?)
(s/def :user-list/roles vector?)
(s/def :user-list/new-user-email string?)

(s/def :user-list/user-list (s/keys :opt [:user-list/users :user-list/roles :user-list/adding-user]))
