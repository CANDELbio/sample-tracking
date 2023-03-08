(ns org.parkerici.sample-tracking.api
  "The main interface for all of the API functionality that's available to users.
  Tries to loosely format responses according to Google's JSON style guide
  https://google.github.io/styleguide/jsoncstyleguide.xml
  Acceptable top level keys are :api-version, :data, and :error
  :error should be an object that should have the key :message with an error message string
  :data should be an object that may have the key :uuid or :items"
  (:require [cheshire.core :as json]
            [trptcolin.versioneer.core :as version]
            [org.parkerici.sample-tracking.utils.ring :as ring-utils]
            [org.parkerici.sample-tracking.db.study :as study-db]
            [org.parkerici.sample-tracking.db.site :as site-db]
            [org.parkerici.sample-tracking.db.cohort :as cohort-db]
            [org.parkerici.sample-tracking.db.kit-type :as kit-type-db]
            [org.parkerici.sample-tracking.db.timepoint :as timepoint-db]
            [org.parkerici.sample-tracking.db.sample-type :as sample-type-db]
            [org.parkerici.sample-tracking.db.form-type :as form-type-db]
            [org.parkerici.sample-tracking.db.kit :as kit-db]
            [org.parkerici.sample-tracking.db.history :as history-db]
            [org.parkerici.sample-tracking.db.role :as role-db]
            [org.parkerici.sample-tracking.db.user :as user-db]
            [org.parkerici.sample-tracking.api.iam :as iam]
            [org.parkerici.sample-tracking.api.kit-type :as kit-type]
            [org.parkerici.sample-tracking.api.form-type :as form-type]
            [org.parkerici.sample-tracking.api.site :as site-api]
            [org.parkerici.sample-tracking.api.study :as study-api]
            [org.parkerici.sample-tracking.api.kit-shipment :as kit-shipment]
            [org.parkerici.sample-tracking.api.propose-kit-edits :as kit-edits]
            [org.parkerici.sample-tracking.api.export :as export]
            [org.parkerici.sample-tracking.api.firebase :as firebase]
            [org.parkerici.sample-tracking.configuration :as c]
            [org.parkerici.multitool.core :as multitool]
            [java-time :as time]
            [clojure.string :as str])
  (:import (java.util UUID)))

(defn api-response
  [data & {:keys [status] :or {status 200}}]
  (ring-utils/json-response data :status status))

(defn catch-error-response
  [to-try]
  (try
    (to-try)
    (api-response {})
    (catch Exception e (api-response {:error {:message (.getMessage e)} :status 400}))))

(defn parse-request
  [request]
  (json/parse-string (slurp (:body request)) true))

(defn api-version
  []
  (api-response {:api-version (version/get-version "sample-tracking" "sample-tracking")}))

(defn parse-boolean-or-nil
  [value]
  (if (nil? value) nil (Boolean/valueOf value)))

(defn parse-uuid-or-nil
  [value]
  (if (nil? value) nil (UUID/fromString value)))

(defn first-uploaded-file-path
  [{:keys [multipart-params]}]
  (let [upload (first (vals multipart-params))]
    (:tempfile upload)))

(defn firebase-js-credentials
  []
  (let [credentials-path (c/firebase-js-credentials-path)
        credentials (json/parse-string (slurp credentials-path) true)]
    (ring-utils/json-response credentials :status 200)))

(defn upload-kit-types
  [request]
  (let [path (first-uploaded-file-path request)]
    (catch-error-response #(kit-type/parse-kit-type-csv-and-save-to-db path))))

(defn upload-form-types
  [request]
  (let [path (first-uploaded-file-path request)]
    (catch-error-response #(form-type/parse-form-type-csv-and-save-to-db path))))

(defn upload-sites
  [request]
  (let [path (first-uploaded-file-path request)]
    (catch-error-response #(site-api/parse-site-csv-and-save-to-db path))))

(defn upload-studies
  [request]
  (let [path (first-uploaded-file-path request)]
    (catch-error-response #(study-api/parse-study-csv-and-save-to-db path))))

(defn list-studies
  [active]
  (api-response {:data {:items (sort-by :name (study-db/list-studies (parse-boolean-or-nil active)))}}))

(defn list-sites
  [study-uuid active]
  (api-response {:data {:items (sort-by :name (site-db/list-sites (parse-uuid-or-nil study-uuid) (parse-boolean-or-nil active)))}}))

(defn get-form-type-fields
  [kit-type-uuid]
  (api-response {:data {:items (form-type-db/get-form-type-fields (parse-uuid-or-nil kit-type-uuid))}}))

(defn list-cohorts
  [study-uuid active]
  (api-response {:data {:items (sort-by :name (cohort-db/list-cohorts (parse-uuid-or-nil study-uuid) (parse-boolean-or-nil active)))}}))

(defn list-timepoints
  [kit-type-uuid]
  (api-response {:data {:items (timepoint-db/list-sorted-kit-type-timepoints (parse-uuid-or-nil kit-type-uuid))}}))

(defn list-kit-types
  [cohort-uuid active]
  ; If one timepoint is passed in we get it as a single number. If multiple are passed in we get a seq.
  ; We need to make the results of either call uniform and parse the numbers into longs.
  (api-response {:data {:items (sort-by :name (kit-type-db/list-kit-types (parse-uuid-or-nil cohort-uuid) (parse-boolean-or-nil active)))}}))

(defn list-sample-types
  [kit-type-uuid]
  (api-response {:data {:items (sort-by :id-suffix (sample-type-db/list-sample-types (parse-uuid-or-nil kit-type-uuid)))}}))

(defn datomic-transaction-response
  [tx-results]
  (let [error-message (:cognitect.anomalies/message tx-results)]
    (if (nil? error-message)
      (api-response {})
      (api-response {:error {:message error-message}} :status 400))))

(defn update-with-history
  [request entity-type fetch-fn update-fn]
  (let [input (parse-request request)
        entity-uuid (parse-uuid-or-nil (:uuid input))
        current-entity (fetch-fn entity-uuid)
        results (update-fn input)
        updated-entity (fetch-fn entity-uuid)]
    (history-db/create-history (get-in request [:session :identity]) entity-type entity-uuid (str current-entity) (str updated-entity))
    (datomic-transaction-response results)))

(defn update-study
  [request]
  (update-with-history request :study study-db/find-study-by-uuid
                       (fn [i] (study-db/update-study (parse-uuid-or-nil (:uuid i)) (:name i)))))

(defn update-site
  [request]
  (update-with-history request :site site-db/find-site-by-uuid
                       (fn [i] (site-db/update-site (parse-uuid-or-nil (:uuid i)) (:name i)))))

(defn update-cohort
  [request]
  (update-with-history request :cohort cohort-db/find-cohort-by-uuid
                       (fn [i] (cohort-db/update-cohort (parse-uuid-or-nil (:uuid i)) (:name i)))))

(defn update-timepoint
  [request]
  (update-with-history request :timepoint timepoint-db/find-timepoint-by-uuid
                       (fn [i] (timepoint-db/update-timepoint (parse-uuid-or-nil (:uuid i)) (:name i)))))

(defn update-kit-type
  [request]
  (update-with-history request :kit-type kit-type-db/find-kit-type-by-uuid
                       (fn [i]
                         (kit-type-db/update-kit-type
                           (parse-uuid-or-nil (:uuid i))
                           (:name i)
                           (Long/parseLong (str (:item-number i)))
                           (:collection-date-required i)
                           (:air-waybill-required i)))))

(defn update-sample-type
  [request]
  (update-with-history request :sample-type sample-type-db/find-sample-type-by-uuid (fn [i] (sample-type-db/update-sample-type (parse-uuid-or-nil (:uuid i)) (:name i) (:id-suffix i) (:ships-with-kit i) (:reminder i)))))

(defn set-active
  [request]
  (let [{:keys [study site cohort kit-type active]} (parse-request request)
        parsed-status (Boolean/valueOf active)
        study-uuid (parse-uuid-or-nil study)
        site-uuid (parse-uuid-or-nil site)
        cohort-uuid (parse-uuid-or-nil cohort)
        kit-type-uuid (parse-uuid-or-nil kit-type)]
    (cond
      (and (some? study-uuid) (some? site-uuid)) (datomic-transaction-response (site-db/update-site-active-status study-uuid site-uuid parsed-status))
      (and (some? study-uuid) (some? cohort-uuid)) (datomic-transaction-response (cohort-db/update-cohort-active-status study-uuid cohort-uuid parsed-status))
      (and (some? cohort-uuid) (some? kit-type-uuid)) (datomic-transaction-response (kit-type-db/update-kit-type-active-status cohort-uuid kit-type-uuid parsed-status))
      (some? study-uuid) (datomic-transaction-response (study-db/update-study-active-status study-uuid parsed-status))
      :else (api-response {:error {:message "Parameters not accepted"}}))))

(defn submit-kit-shipment
  [request]
  (let [input (parse-request request)
        uuid (parse-uuid-or-nil (:uuid input))
        complete-kit (kit-db/get-kit {:uuid uuid :complete true})]
    (if (or (nil? uuid) (not complete-kit))
      (api-response {:data {:uuid (kit-shipment/submit-kit-shipment uuid input)}})
      (api-response {:error {:message "Kit has already been submitted."}} :status 400))))

(defn create-incomplete-kit-shipment
  [request]
  (let [input (merge (parse-request request) {:complete false})
        uuid (kit-shipment/create-kit-shipment input)]
    (api-response {:data {:uuid uuid}})))

(defn list-kit-shipments
  [request]
  (let [user (get-in request [:session :identity])
        roles (get-in request [:session :roles])
        {:keys [uuid complete archived]} (:params request)
        parsed-kit-uuid (parse-uuid-or-nil uuid)
        parsed-complete (parse-boolean-or-nil complete)
        parsed-archived (parse-boolean-or-nil archived)
        shipment-options (cond-> {}
                                 (contains? roles (c/site-coordinator-role)) (assoc :completing-email user)
                                 (contains? roles (c/site-admin-role)) (assoc :completing-email-domain (second (str/split user #"@")))
                                 (some? parsed-kit-uuid) (assoc :uuid parsed-kit-uuid)
                                 (some? parsed-complete) (assoc :complete parsed-complete)
                                 (some? parsed-archived) (assoc :archived parsed-archived))
        kits (kit-shipment/list-kit-shipment shipment-options)]
    (api-response {:data {:items (reverse (sort-by :uuid kits))}})))

(defn kit-shipment-submitted
  [kit-id]
  (let [submitted-kit (kit-db/get-kit {:kit-id kit-id :complete true :archived false})]
    (api-response {:data {:items [{:kit-id kit-id :exists (some? submitted-kit)}]}})))

(defn get-incomplete-kit-shipment
  [raw-kit-uuid]
  (let [kit-uuid (parse-uuid-or-nil raw-kit-uuid)
        complete-kit (kit-db/get-kit {:uuid kit-uuid :complete true})
        shipment-options (cond-> {}
                                 (some? kit-uuid) (assoc :uuid kit-uuid))]
    (if (not complete-kit)
      (api-response {:data {:items (kit-shipment/list-kit-shipment shipment-options)}})
      (api-response {:error {:message "Kit not found."}} :status 400))))

(defn update-kit-shipment
  [request]
  (let [input (parse-request request)
        kit-uuid (parse-uuid-or-nil (:uuid input))
        user (get-in request [:session :identity])]
    (if (kit-edits/kit-has-pending-edits kit-uuid)
      (api-response {:error {:message "Kit has pending edits."}} :status 400)
      (api-response {:data {:uuid (kit-shipment/update-kit-shipment-with-history kit-uuid user input)}}))))

(defn propose-kit-shipment-edit
  [request]
  (let [user-email (get-in request [:session :identity])
        input (parse-request request)
        uuid (kit-edits/propose-kit-edits input user-email)]
    (api-response {:data {:uuid uuid}})))

(defn get-proposed-kit-shipment-edit-for-view
  [request]
  (let [uuid (get-in request [:params :uuid])
        parsed-uuid (parse-uuid-or-nil uuid)]
    (api-response {:data {:items [(kit-edits/get-proposed-kit-edit-for-display parsed-uuid)]}})))

(defn list-proposed-kit-shipment-edits
  [status]
  (api-response {:data {:items (reverse (sort-by :uuid (kit-edits/list-proposed-edits {:status status})))}}))

(defn get-kit-shipment-or-proposed-edit
  [request]
  (let [user (get-in request [:session :identity])
        roles (get-in request [:session :roles])
        uuid (parse-uuid-or-nil (get-in request [:params :uuid]))
        shipment-options (cond-> {:uuid uuid}
                                 (contains? roles (c/site-coordinator-role)) (assoc :completing-email user)
                                 (contains? roles (c/site-admin-role)) (assoc :completing-email-domain (second (str/split user #"@"))))]
    (api-response {:data {:items [(kit-edits/get-kit-or-proposed-edit shipment-options)]}})))

(defn update-proposed-kit-shipment-edit-status
  [request status-update-fn]
  (let [user-email (get-in request [:session :identity])
        uuid (get-in request [:params :uuid])
        parsed-uuid (parse-uuid-or-nil uuid)
        proposed-edit (kit-edits/get-proposed-kit-edit parsed-uuid)
        proposed-edit-status (:status proposed-edit)]
    (cond
      (nil? status-update-fn) (api-response {:error {:message "Status not found"} :status 400})
      (not= proposed-edit-status "pending") (api-response {:error {:message "Edit not pending."}} :status 400)
      :else (api-response (status-update-fn parsed-uuid user-email)))))

(defn set-kit-shipment-edit-status
  [request]
  (let [status (get-in request [:params :status])
        update-fn (case status
                    "approved" kit-edits/approve-proposed-kit-edit
                    "denied" kit-edits/deny-proposed-kit-edit
                    nil)]
    (update-proposed-kit-shipment-edit-status request update-fn)))

(defn update-incomplete-kit-shipment
  [request]
  (let [input (parse-request request)
        kit-uuid (parse-uuid-or-nil (:uuid input))
        complete-kit (kit-db/get-kit {:uuid kit-uuid :complete true})]
    (if (not complete-kit)
      (api-response {:data {:uuid (kit-shipment/update-kit-shipment kit-uuid input)}})
      (api-response {:error {:message "Kit not found."}} :status 400))))

(defn set-kit-shipment-archived
  [request raw-uuid raw-archived]
  (let [user (get-in request [:session :identity])
        kit-uuid (parse-uuid-or-nil raw-uuid)
        archived (parse-boolean-or-nil raw-archived)]
    (api-response {:data {:uuid (kit-shipment/set-kit-shipment-archived kit-uuid user archived)}})))

(defn export-samples->csv
  [raw-uuid raw-complete raw-archived raw-uncollected]
  (let [export-options (multitool/clean-map
                         {:uuid                (parse-uuid-or-nil raw-uuid)
                          :complete            (parse-boolean-or-nil raw-complete)
                          :archived            (parse-boolean-or-nil raw-archived)
                          :include-uncollected (parse-boolean-or-nil raw-uncollected)})
        streaming-samples (export/export-samples-to-streaming-csv export-options)
        filename (str "ereq_" (time/format (time/formatter "YYYY_MM_dd_HH_mm") (time/local-date-time)) ".csv")]
    (ring-utils/csv-response streaming-samples filename)))

(defn user-info
  [request]
  (let [{:keys [identity is-a-user email-verified roles auth-error]} (:session request)
        items (cond
                (some? identity) [{:email          identity
                                   :roles          roles
                                   :is-a-user      is-a-user
                                   :email-verified email-verified}]
                (true? auth-error) [{:auth-error true}]
                :else [{}])]
    (api-response {:data {:items items}})))

(defn list-history
  [entity-uuid]
  (api-response {:data {:items (sort-by :time (history-db/list-history (parse-uuid-or-nil entity-uuid)))}}))

(defn list-roles
  []
  (api-response {:data {:items (sort-by :name (role-db/list-roles))}}))

(defn list-users
  []
  (api-response {:data {:items (sort-by :email (user-db/list-users {}))}}))

(defn create-user
  [request]
  (let [email (:email (parse-request request))
        user (iam/get-user email)]
    (cond
      (true? (:deactivated user)) (api-response {:data {:uuid (iam/reactivate-user email)}})
      (some? user) (api-response {:error {:message "User already exists."}} :status 400)
      :else (api-response {:data {:uuid (user-db/create-user email)}}))))

(defn deactivate-user
  [request]
  (let [requesting-email (get-in request [:session :identity])
        deactivating-email (:email (parse-request request))]
    (if (= requesting-email deactivating-email)
      (api-response {:error {:message "Cannot deactivate yourself."}} :status 400)
      (catch-error-response #(iam/deactivate-user deactivating-email)))))

(defn add-role-to-user
  [request]
  (let [input (parse-request request)]
    (catch-error-response #(iam/add-role-to-user (:email input) (:role-name input)))))

(defn remove-role-from-user
  [request]
  (let [input (parse-request request)
        user-email (get-in request [:session :identity])
        modifying-email (:email input)
        role-name (:role-name input)]
    (if (and (= user-email modifying-email) (= role-name (c/application-admin-role)))
      (api-response {:error {:message "Cannot remove admin from yourself."}} :status 400)
      (catch-error-response #(iam/remove-role-from-user (:email input) (:role-name input))))))

(defn list-configuration
  []
  (api-response {:data {:items [{:datomic-endpoint         (c/datomic-endpoint)
                                 :datomic-db-name          (c/datomic-db-name)
                                 :sendgrid-api-key         (c/sendgrid-api-key)
                                 :email-sender             (c/email-sender)
                                 :email-manifest-recipient (c/email-manifest-recipient)
                                 :send-manifest-emails     (c/send-manifest-emails)
                                 :send-vendor-emails       (c/send-vendor-emails)}]}}))

(defn log-in
  [request]
  (if-let [firebase-jwt (:firebase-jwt (parse-request request))]
    (let [authed-session (firebase/add-firebase-auth-to-session (:session request) firebase-jwt)]
      (-> (api-response {:success true})
          (assoc :session authed-session)))
    (api-response {:error {:message "Request missing Firebase JWT"}} :status 400)))

;Method to log in in case of no internet for firebase.
;(defn test-log-in
;  []
;  (let [session (-> {}
;                    (assoc :identity "rschiemann@parkerici.org")
;                    (assoc :roles #{"site-admin"})
;                    (assoc :is-a-user true)
;                    (assoc :email-verified true))]
;    (-> (api-response {:success true})
;        (assoc :session session))))

(defn log-out
  [request]
  (-> (api-response {:success true})
      (assoc :session (firebase/remove-firebase-auth-from-session (:session request)))))