(ns org.parkerici.sample-tracking.api.email
  (:require [org.parkerici.sample-tracking.configuration :as c]
            [org.parkerici.sample-tracking.db.study :as study-db]
            [org.parkerici.sample-tracking.db.site :as site-db]
            [org.parkerici.sample-tracking.db.cohort :as cohort-db]
            [org.parkerici.sample-tracking.db.kit-type :as kit-type-db]
            [org.parkerici.sample-tracking.db.timepoint :as timepoint-db]
            [org.parkerici.sample-tracking.db.sample-type :as sample-type-db]
            [org.parkerici.sample-tracking.db.form-type :as form-type-db]
            [org.parkerici.sample-tracking.db.kit :as kit-db]
            [org.parkerici.sample-tracking.api.export :as export]
            [org.parkerici.sample-tracking.utils.path :as path]
            [org.parkerici.sample-tracking.pages.manifest :as manifest-page]
            [again.core :as again]
            [clojure.java.io :as io]
            [hiccup.core :as hiccup]
            [clj-htmltopdf.core :as htmltopdf]
            [org.parkerici.sample-tracking.utils.date-time :as dt])
  (:import [com.sendgrid SendGrid SendGrid$Email]
           (java.util UUID)))

(def csv-attachment-name "samples.csv")
(def pdf-attachment-name "manifest.pdf")

(defn send-message
  [to subject content csv-file]
  (let [pdf-path (path/join (c/temp-path) pdf-attachment-name)
        _ (htmltopdf/->pdf content pdf-path)
        pdf-file (io/file pdf-path)
        sg (new SendGrid (c/sendgrid-api-key))
        email (doto (new SendGrid$Email)
                (.addTo to)
                (.setFrom (c/email-sender))
                (.setSubject subject)
                (.setHtml content))
        email-with-csv (if csv-file
                         (.addAttachment email csv-attachment-name csv-file)
                         email)
        email-with-pdf (if pdf-file
                         (.addAttachment email-with-csv pdf-attachment-name pdf-file)
                         email-with-csv)]
    (try
      (again/with-retries
        [1000 10000]
        (.send sg email-with-pdf))
      (catch Exception ex
        (.printStackTrace ex)
        (println (.getMessage ex))))
    (io/delete-file pdf-path)))

(defn manifest-email-body
  [kit-map config]
  (let [{:keys [kit-id site study cohort participant-id air-waybill collection-timestamp timezone
                completing-first-name completing-last-name completing-email comments timepoints kit-type samples
                form-type-field-values]} kit-map
        kit-type-uuid (UUID/fromString kit-type)
        site-name (:name (site-db/find-site-by-uuid (UUID/fromString site)))
        study-name (:name (study-db/find-study-by-uuid (UUID/fromString study)))
        cohort-name (:name (cohort-db/find-cohort-by-uuid (UUID/fromString cohort)))
        timepoint-names (map #(:name (timepoint-db/find-timepoint-by-uuid (UUID/fromString %))) timepoints)
        kit-type-name (kit-type-db/get-kit-type-name kit-type-uuid)
        selected-sample-types (sort-by :id-suffix (sample-type-db/list-sample-types kit-type-uuid))
        selected-form-type-fields (form-type-db/get-form-type-fields kit-type-uuid)
        date-display-fn (fn [date] (when date (dt/generate-date-string date timezone)))
        time-display-fn (fn [time] (when time (dt/generate-time-string time timezone)))
        completing-user-manifest (:completing-user-manifest config)
        content (manifest-page/content {:site-name                 site-name
                                        :study-name                study-name
                                        :cohort-name               cohort-name
                                        :timepoint-names           timepoint-names
                                        :kit-name                  kit-type-name
                                        :kit-id                    kit-id
                                        :participant-id            participant-id
                                        :collection-date           collection-timestamp
                                        :collection-time           collection-timestamp
                                        :selected-form-type-fields selected-form-type-fields
                                        :form-type-field-values    form-type-field-values
                                        :selected-sample-types     selected-sample-types
                                        :sample-values             samples
                                        :air-waybill               air-waybill
                                        :completing-first-name     completing-first-name
                                        :completing-last-name      completing-last-name
                                        :completing-email          completing-email
                                        :comments                  comments
                                        :date-display-fn           date-display-fn
                                        :time-display-fn           time-display-fn
                                        :add-empty-field-lines     completing-user-manifest
                                        :add-signature-fields      completing-user-manifest})]
    (hiccup/html [:div.page-body
                  [:h1 "Kit Shipment Manifest"]
                  [:div.kit-manifest
                   content]])))

(defn send-manifest-email
  [kit-map kit-uuid]
  (let [completing-user-body (manifest-email-body kit-map {:completing-user-manifest true})
        non-completing-user-body (manifest-email-body kit-map {:completing-user-manifest false})
        subject "Sample Tracking Kit Manifest"
        completing-email (:completing-email kit-map)
        vendor-email (kit-db/get-kit-vendor-email kit-uuid)
        csv-path (path/join (c/temp-path) csv-attachment-name)
        _ (export/export-samples-to-csv {:uuid kit-uuid :shipped true} csv-path)
        csv-file (io/file csv-path)]
    (when (c/send-manifest-emails)
      (send-message (c/email-manifest-recipient) subject non-completing-user-body csv-file)
      (send-message completing-email subject completing-user-body csv-file)
      (when (and (c/send-vendor-emails) (some? vendor-email))
        (send-message vendor-email subject non-completing-user-body csv-file)))
    (io/delete-file csv-path)))

(defn proposed-edit-body
  [user-email]
  (hiccup/html
    [:div.page-body
     [:p (str user-email " has proposed a kit edit. Please login to the application to approve or deny it.")]]))

(defn send-proposed-edit-email
  [update-map user-email]
  (let [recipient (c/email-manifest-recipient)
        subject (str "Edit Proposed for Kit " (:kit-id update-map))
        body (proposed-edit-body user-email)]
    (when (c/send-manifest-emails)
      (send-message recipient subject body nil))))