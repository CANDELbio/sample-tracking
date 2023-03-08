(ns org.parkerici.sample-tracking.components.type.upload.views
  (:require [re-frame.core :refer [dispatch]]
            [org.parkerici.sample-tracking.components.type.upload.events]
            [org.parkerici.sample-tracking.components.type.upload.subs]
            [org.parkerici.sample-tracking.components.type.upload.db]))

(defn component
  []
  (fn [file-type]
    [:input {:type      "file" :id (str "file-upload-" file-type)
             :on-change #(dispatch [:type-upload/upload-file file-type])}]))