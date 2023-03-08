(ns org.parkerici.sample-tracking.components.type.upload.events
  (:require [re-frame.core :refer [reg-event-fx]]
            [org.parkerici.sample-tracking.events :as events]
            [ajax.core :as ajax]))

; TODO - Should this go in the on-change function instead of here?
(defn get-uploaded-file
  [file-type]
  (let [element-id (str "file-upload-" file-type)
        el (.getElementById js/document element-id)
        file (first (array-seq (.-files el)))
        form-data (js/FormData.)]
    (.append form-data (.-name file) file)
    form-data))

(reg-event-fx
  :type-upload/upload-file
  events/default-interceptors
  (fn [{:keys [db]} [file-type]]
    {:db         (assoc-in db [:type-upload/type-upload :type-upload/uploading] true)
     :http-xhrio {:method          :post
                  :uri             (str "/api/upload/" file-type)
                  :timeout         60000
                  :body            (get-uploaded-file file-type)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:type-upload/upload-complete file-type true]
                  :on-failure      [:type-upload/upload-complete file-type false]}}))

(defn clear-uploaded-file
  [file-type]
  (let [element-id (str "file-upload-" file-type)
        el (.getElementById js/document element-id)]
    (set! (.. el -value) "")))

(reg-event-fx
  :type-upload/upload-complete
  events/default-interceptors
  (fn [{:keys [db]} [file-type success request]]
    (clear-uploaded-file file-type)
    (let [to-dispatch (if success
                        [:flash/request-success "Upload success!"]
                        [:flash/request-error "Error uploading! Server said: " request])]
      {:db       (assoc-in db [:type-upload/type-upload :type-upload/uploading] false)
       :dispatch to-dispatch})))