(ns org.parkerici.sample-tracking.handler
  (:require [compojure.core :refer [defroutes context routes POST GET PATCH DELETE make-route]]
            [compojure.route :as route]
            [ring.middleware.format-params :refer [wrap-transit-json-params]]
            [ring.middleware.format-response :refer [wrap-transit-json-response]]
            [ring.logger :as logger]
            [ring.middleware.session.memory :as ring-memory]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [taoensso.timbre :as log]
            [ring.middleware.defaults :as middleware]
            [ring.util.response :as response]
            [org.parkerici.sample-tracking.api :as api]
            [org.parkerici.sample-tracking.utils.ring :as ring-utils]
            [org.parkerici.sample-tracking.handlers.auth :as auth]
            [org.parkerici.sample-tracking.db.datomic :as datomic]))

(defroutes site-routes
           ;; Things handled by SPA
           (GET "*" [] (response/content-type (response/resource-response "index.html" {:root "public"}) "text/html")))

;;; Weird that this isn't a standard part of ring
(defn wrap-exception-handling
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch clojure.lang.ExceptionInfo e
        {:status 400 :headers {} :body (str "Error: " (ex-message e))})
      (catch Throwable e
        {:status 500 :headers {} :body (str "Error: " (print-str e))}))))

;;; Weird that this isn't a standard part of ring
(defn wrap-no-read-eval
  [handler]
  (fn [request]
    (binding [*read-eval* false]
      (handler request))))

;;; Ensure API and site pages use the same store, so authentication works for API.
(def common-store (ring-memory/memory-store))

;;; Note: static resources are handled by middleware, see middleware/site-defaults
(def site-defaults
  (-> middleware/site-defaults
      auth/set-auth-site-defaults
      (assoc-in [:security :anti-forgery] false)            ;necessary for upload (TODO not great from sec viewpoint)
      (assoc :cookies true)
      (assoc-in [:params :multipart] true)                  ;to support file uploads
      (assoc-in [:session :flash] false)
      (assoc-in [:session :store] common-store)))

(defn wrap-logger
  "Hook Ring logger to timbre unless logger is disabled"
  [handler options]
  (if (:disable-logger options)
    handler
    (logger/wrap-with-logger
      handler
      {:log-fn (fn [{:keys [level throwable message]}]
                 (log/log level throwable message))})))

(defn make-site
  [options]
  (-> site-routes
      (auth/wrap-auth options)
      datomic/wrap-datomic
      (middleware/wrap-defaults site-defaults)
      wrap-no-read-eval
      (wrap-logger options)
      wrap-exception-handling))

(defroutes api-routes
           (context "/api" []
             (GET "/" [] (api/api-version))
             (context "/kit" []
               (POST "/" request (api/submit-kit-shipment request))
               (PATCH "/" request (api/update-kit-shipment request))
               (GET "/" request (api/list-kit-shipments request))
               (GET "/submitted" [kit-id] (api/kit-shipment-submitted kit-id))
               (PATCH "/set-archived" [uuid archived :as request] (api/set-kit-shipment-archived request uuid archived))
               (context "/share" []
                 (POST "/" request (api/create-incomplete-kit-shipment request))
                 (PATCH "/" request (api/update-incomplete-kit-shipment request))
                 (GET "/" [uuid] (api/get-incomplete-kit-shipment uuid)))
               (context "/propose-edit" []
                 (GET "/" request (api/get-kit-shipment-or-proposed-edit request))
                 (GET "/view" request (api/get-proposed-kit-shipment-edit-for-view request))
                 (GET "/list" [status] (api/list-proposed-kit-shipment-edits status))
                 (POST "/" request (api/propose-kit-shipment-edit request))
                 (POST "/set-status" request (api/set-kit-shipment-edit-status request))))
             (context "/upload" []
               (POST "/kit-type" request (api/upload-kit-types request))
               (POST "/form-type" request (api/upload-form-types request))
               (POST "/site" request (api/upload-sites request))
               (POST "/study" request (api/upload-studies request)))
             (context "/site" []
               (GET "/" [study active] (api/list-sites study active))
               (POST "/" request (api/update-site request)))
             (context "/study" []
               (GET "/" [active] (api/list-studies active))
               (POST "/" request (api/update-study request)))
             (context "/cohort" []
               (GET "/" [study active] (api/list-cohorts study active))
               (POST "/" request (api/update-cohort request)))
             (context "/kit-type" []
               (GET "/" [cohort active] (api/list-kit-types cohort active))
               (POST "/" request (api/update-kit-type request)))
             (context "/timepoint" []
               (GET "/" [kit-type] (api/list-timepoints kit-type))
               (POST "/" request (api/update-timepoint request)))
             (context "/sample-type" []
               (GET "/" [kit-type] (api/list-sample-types kit-type))
               (POST "/" request (api/update-sample-type request)))
             (context "/sample" []
               (GET "/export" [uuid complete archived uncollected]
                 (api/export-samples->csv uuid complete archived uncollected)))
             (context "/user" []
               (GET "/" [] (api/list-users))
               (POST "/" request (api/create-user request))
               (DELETE "/" request (api/deactivate-user request))
               (GET "/current" request (api/user-info request))
               (context "/role" []
                 (POST "/" request (api/add-role-to-user request))
                 (DELETE "/" request (api/remove-role-from-user request))))
             (GET "/role" [] (api/list-roles))
             (GET "/form-type-fields" [kit-type] (api/get-form-type-fields kit-type))
             (GET "/history" [uuid] (api/list-history uuid))
             (POST "/set-active" request (api/set-active request))
             (GET "/configuration" [] (api/list-configuration))
             (POST "/log-in" request (api/log-in request))
             (POST "/log-out" request (api/log-out request))
             (GET "/firebase-credentials" [] (api/firebase-js-credentials))
             (GET "/health" [] (ring-utils/json-response {:success true} :status 200))
             (route/not-found (ring-utils/json-response {:error "Not Found"} :status 404))))

(def api-defaults
  (-> middleware/api-defaults
      auth/set-auth-site-defaults
      (assoc :cookies true)
      (assoc-in [:session :flash] false)
      (assoc-in [:session :store] common-store)))

;;; Must be something built-in for this?
(defn wrap-filter
  [handler path]
  (make-route nil path handler))

(defn make-api
  [options]
  (-> api-routes
      (auth/wrap-auth options)
      (middleware/wrap-defaults api-defaults)
      wrap-no-read-eval
      datomic/wrap-datomic
      wrap-transit-json-params
      (wrap-logger options)
      wrap-exception-handling
      wrap-transit-json-response
      wrap-gzip
      (wrap-filter "/api/*")))

; Returns a blank response for anything called to /__/auth/*
; Used for firebase auth stuff. Hopefully works.
(defn make-firebase
  []
  (wrap-filter (GET "/__/auth/*" [] {}) "/__/auth/*"))

(defn make-app
  [options]
  (routes (make-api options) (make-firebase) (make-site options)))

(def app
  (make-app {}))