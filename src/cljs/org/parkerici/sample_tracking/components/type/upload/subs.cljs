(ns org.parkerici.sample-tracking.components.type.upload.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
  :type-upload/uploading
  (fn [db]
    (get-in db [:type-upload/type-upload :type-upload/uploading])))