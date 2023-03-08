(ns org.parkerici.sample-tracking.components.history.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
  :history/histories
  (fn [db]
    (get-in db [:history/history :history/histories])))
