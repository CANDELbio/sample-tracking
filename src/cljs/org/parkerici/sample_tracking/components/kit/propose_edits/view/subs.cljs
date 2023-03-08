(ns org.parkerici.sample-tracking.components.kit.propose-edits.view.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
  :proposed-edit/proposed-edit
  (fn [db]
    (get-in db [:proposed-edit/proposed-edit :proposed-edit/proposed-edit])))