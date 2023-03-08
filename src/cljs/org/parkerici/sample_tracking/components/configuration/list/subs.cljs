(ns org.parkerici.sample-tracking.components.configuration.list.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
  :configuration-list/configuration-map
  (fn [db]
    (get-in db [:configuration-list/configuration-list :configuration-list/configuration-map])))