(ns org.parkerici.sample-tracking.components.kit.list.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
  :kit-list/kits
  (fn [db]
    (get-in db [:kit-list/kit-list :kit-list/kits])))

(reg-sub
  :kit-list/filter
  (fn [db]
    (get-in db [:kit-list/kit-list :kit-list/filter])))
