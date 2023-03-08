(ns org.parkerici.sample-tracking.configuration
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn environment
  "Retrieves which profile we should be using based on which environment we're deployed to"
  []
  (let [env (System/getenv "DEPLOY_ENVIRONMENT")]
    (if (empty? env)
      :default
      (keyword env))))

(def config (aero/read-config (io/resource "config.edn") {:profile (environment)}))

(defn datomic-config
  []
  (:datomic config))

(defn datomic-endpoint
  []
  (:endpoint (datomic-config)))

(defn datomic-access-key
  []
  (:access-key (datomic-config)))

(defn datomic-secret
  []
  (:secret (datomic-config)))

(defn datomic-db-name
  []
  (:db-name (datomic-config)))

(defn datomic-validate-hostnames
  []
  (:validate-hostnames (datomic-config)))

(defn application-config
  []
  (:application config))

(defn firebase-js-credentials-path
  []
  (:firebase-js-credentials-path (application-config)))

(defn application-roles
  []
  (:roles (application-config)))

(defn application-role-values
  []
  (vals (application-roles)))

(defn application-admin-role
  []
  (:admin (application-roles)))

(defn application-editor-role
  []
  (:editor (application-roles)))

(defn application-viewer-role
  []
  (:viewer (application-roles)))

(defn site-admin-role
  []
  (:site-admin (application-roles)))

(defn site-coordinator-role
  []
  (:site-coordinator (application-roles)))

(defn csv-files-config
  []
  (:csv-files (application-config)))

(defn csv-file-headers
  [file]
  (get-in (csv-files-config) [file :headers]))

(defn email-config
  []
  (:email (application-config)))

(defn sendgrid-api-key
  []
  (:sendgrid-api-key (email-config)))

(defn email-sender
  []
  (:sender (email-config)))

(defn email-manifest-recipient
  []
  (:manifest-recipient (email-config)))

(defn send-manifest-emails
  []
  (= (str/lower-case (:send-manifest-emails (email-config))) "true"))

(defn send-vendor-emails
  []
  (= (str/lower-case (:send-vendor-emails (email-config))) "true"))

(defn temp-path
  []
  (:temp-path (application-config)))

(defn api-key
  []
  (:api-key (application-config)))

(defn sample-export-config
  []
  (:sample-export (application-config)))

(defn sample-export-columns-to-drop
  []
  (:columns-to-drop (sample-export-config)))

(defn sample-export-column-order
  []
  (:column-order (sample-export-config)))

(defn sample-export-columns-to-rename
  []
  (:columns-to-rename (sample-export-config)))