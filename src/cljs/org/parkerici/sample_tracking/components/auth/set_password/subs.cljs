(ns org.parkerici.sample-tracking.components.auth.set-password.subs
  (:require [re-frame.core :refer [reg-sub]]))
(reg-sub
  :set-password/new-password
  (fn [db]
    (get-in db [:set-password/set-password :set-password/new-password])))

(reg-sub
  :set-password/confirm-password
  (fn [db]
    (get-in db [:set-password/set-password :set-password/confirm-password])))

(reg-sub
  :set-password/validity-map
  (fn [db]
    (get-in db [:set-password/set-password :set-password/validity-map])))

(reg-sub
  :set-password/valid-password
  (fn [db]
    (let [validity-map (get-in db [:set-password/set-password :set-password/validity-map])
          validity-values (vals validity-map)]
      (and (not-empty validity-values) (every? true? validity-values)))))