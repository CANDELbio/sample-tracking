(ns org.parkerici.sample-tracking.test-utils
  (:require [ring.mock.request :as rm]
            [cheshire.core :as json]
            [org.parkerici.sample-tracking.db.datomic :as d]
            [org.parkerici.sample-tracking.handler :as handlers]
            [org.parkerici.sample-tracking.configuration :as config]))

(defmacro with-datomic-context [& body]
  `(d/wrap-datomic-fn (fn [] ~@body)))

(def standard-web-app (handlers/make-app {:disable-logger true}))

(defn admin-auth-wrapper
  [handler]
  (fn [request]
    (handler (let [current-session (:session request)
                   updated-session (merge current-session
                                          {:identity       "test@example.com"
                                           :roles          (set [(config/application-admin-role)])
                                           :is-a-user      true
                                           :email-verified true})]
               (assoc request :session updated-session)))))

(def admin-authed-web-app (handlers/make-app {:auth-wrapper admin-auth-wrapper :disable-logger true}))

(defn site-coordinator-auth-wrapper
  [handler]
  (fn [request]
    (handler (let [current-session (:session request)
                   updated-session (merge current-session
                                          {:identity       "test@example.com"
                                           :roles          (set [(config/site-coordinator-role)])
                                           :is-a-user      true
                                           :email-verified true})]
               (assoc request :session updated-session)))))

(def site-coordinator-authed-web-app (handlers/make-app {:auth-wrapper admin-auth-wrapper :disable-logger true}))

(defn get-element-with-name
  [list name]
  (first (filter #(= (:name %) name) list)))

(defn get-response-items
  [response]
  (get-in (json/parse-string (:body response) true) [:data :items]))

(defn get-response-uuid
  [response]
  (get-in (json/parse-string (:body response) true) [:data :uuid]))

(defn get-studies-list
  []
  (let [studies-response (standard-web-app (rm/request :get "/api/study" {:active "true"}))]
    (get-response-items studies-response)))

(defn get-study-by-name
  [study-name]
  (let [studies-list (get-studies-list)]
    (get-element-with-name studies-list study-name)))

(defn get-sites-list
  [study-uuid]
  (let [sites-response (standard-web-app (rm/request :get "/api/site" {:study study-uuid :active "true"}))]
    (get-response-items sites-response)))

(defn get-site-by-name
  [study-uuid site-name]
  (let [sites-list (get-sites-list study-uuid)]
    (get-element-with-name sites-list site-name)))

(defn get-cohorts-list
  [study-uuid]
  (let [cohorts-response (standard-web-app (rm/request :get "/api/cohort" {:study study-uuid :active "true"}))]
    (get-response-items cohorts-response)))

(defn get-cohort-by-name
  [study-uuid cohort-name]
  (let [cohorts-list (get-cohorts-list study-uuid)]
    (get-element-with-name cohorts-list cohort-name)))

(defn get-kit-types-list
  [cohort-uuid]
  (let [kit-types-response (standard-web-app (rm/request :get "/api/kit-type" {:cohort cohort-uuid :active "true"}))]
    (get-response-items kit-types-response)))

(defn get-kit-type-by-name
  [cohort-uuid kit-type-name]
  (let [kit-types-list (get-kit-types-list cohort-uuid)]
    (get-element-with-name kit-types-list kit-type-name)))

(defn get-timepoint-list
  [kit-type-uuid]
  (let [timepoints-response (standard-web-app (rm/request :get "/api/timepoint" {:kit-type kit-type-uuid}))]
    (get-response-items timepoints-response)))

(defn get-timepoint-by-name
  [kit-type-uuid timepoint-name]
  (let [timepoint-list (get-timepoint-list kit-type-uuid)]
    (get-element-with-name timepoint-list timepoint-name)))

(defn get-sample-types-list
  [kit-type-uuid]
  (let [sample-types-response (standard-web-app (rm/request :get "/api/sample-type" {:kit-type kit-type-uuid}))]
    (get-response-items sample-types-response)))

(defn get-form-types-list
  [kit-type-uuid]
  (let [form-types-response (standard-web-app (rm/request :get "/api/form-type-fields" {:kit-type kit-type-uuid}))]
    (get-response-items form-types-response)))

(defn get-kit-list
  [params]
  (let [kit-response (admin-authed-web-app (rm/request :get "/api/kit" params))]
    (get-response-items kit-response)))

(defn get-proposed-kit-edit-list
  [params]
  (let [kit-response (admin-authed-web-app (rm/request :get "/api/kit/propose-edit/list" params))]
    (get-response-items kit-response)))

(defn get-entity-history
  [uuid]
  (get-response-items (admin-authed-web-app (rm/request :get "/api/history" {:uuid uuid}))))

(defn get-sample-type-with-suffix
  [sample-types suffix]
  (first (filter #(= (:id-suffix %) suffix) sample-types)))

(defn build-create-sample-map
  [sample-types]
  (let [a01 (get-sample-type-with-suffix sample-types "-A01")
        a02 (get-sample-type-with-suffix sample-types "-A02")
        a12 (get-sample-type-with-suffix sample-types "-A12")]
    {(keyword (:uuid a01)) {:collected true :sample-id "12345678-A01"}
     (keyword (:uuid a02)) {:collected true :sample-id "BAZ-BAT"}
     (keyword (:uuid a12)) {:collected true :shipped true :sample-id "12345678-A12"}}))

(defn build-create-request-body
  [study site cohort kit-type timepoint sample-types]
  {:study                  (:uuid study)
   :site                   (:uuid site)
   :cohort                 (:uuid cohort)
   :kit-type               (:uuid kit-type)
   :timepoints             [(:uuid timepoint)]
   :samples                (build-create-sample-map sample-types)
   :kit-id                 "12345678"
   :participant-id         "STUDY13-101-0011"
   :air-waybill            "123456789012"
   :completing-first-name  "Foo"
   :completing-last-name   "Bar"
   :completing-email       "test@example.com"
   :collection-timestamp   "2020-12-01T21:13:00.000Z"
   :timezone               "America/Los_Angeles"
   :comments               "Comments go here"
   :form-type-field-values {:processing-time "2020-12-01T22:14:00.000Z"}})

(defn kit-request-body
  []
  (let [study (get-study-by-name "STUDY13")
        site (get-site-by-name (:uuid study) "CCCC")
        cohort (get-cohort-by-name (:uuid study) "N/A")
        kit-type (get-kit-type-by-name (:uuid cohort) "STUDY13 Blood-Serum Manifest Form")
        timepoint (get-timepoint-by-name (:uuid kit-type) "BL")
        sample-types (get-sample-types-list (:uuid kit-type))]
    (build-create-request-body study site cohort kit-type timepoint sample-types)))