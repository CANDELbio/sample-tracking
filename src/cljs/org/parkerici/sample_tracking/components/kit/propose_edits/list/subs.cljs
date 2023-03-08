(ns org.parkerici.sample-tracking.components.kit.propose-edits.list.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
  :proposed-edits-list/proposed-edits
  (fn [db]
    (get-in db [:proposed-edits-list/proposed-edits-list :proposed-edits-list/proposed-edits])))