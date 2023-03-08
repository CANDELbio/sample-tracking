(ns org.parkerici.sample-tracking.subs
  (:require [re-frame.core :refer [reg-sub]]
            [org.parkerici.sample-tracking.db :as db]))

(reg-sub
  :page
  (fn [db]
    [(::db/page db) (::db/route-params db)]))

(reg-sub
  :route-params
  (fn [db]
    (::db/route-params db)))

(reg-sub
  :query-params
  (fn [db]
    (::db/query-params db)))

(reg-sub
  :user
  (fn [db]
    (::db/user db)))

(reg-sub
  :firebase-initialized
  (fn [db]
    (::db/firebase-initialized db)))