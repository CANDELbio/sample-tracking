(ns org.parkerici.sample-tracking.components.user.list.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
  :user-list/users
  (fn [db]
    (get-in db [:user-list/user-list :user-list/users])))

(reg-sub
  :user-list/roles
  (fn [db]
    (get-in db [:user-list/user-list :user-list/roles])))

(reg-sub
  :user-list/new-user-email
  (fn [db]
    (get-in db [:user-list/user-list :user-list/new-user-email])))