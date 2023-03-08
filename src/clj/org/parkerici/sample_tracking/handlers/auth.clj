(ns org.parkerici.sample-tracking.handlers.auth
  (:require [clojure.string :as str]
            [ring.util.response :as response]
            [buddy.auth :refer [authenticated?]]
            [buddy.auth.backends.session :refer [session-backend]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [buddy.auth.accessrules :refer [wrap-access-rules]]
            [org.parkerici.sample-tracking.utils.ring :as ring-utils]
            [org.parkerici.sample-tracking.configuration :as config]))

(defn set-auth-site-defaults
  [site-defaults]
  (assoc-in site-defaults [:session :cookie-attrs :same-site] :lax))

; Default unauthorized handler. If the request is to an API page return a json response
; Otherwise redirect the user to the unauthorized page.
(defn unauthorized-handler
  [request _metadata]
  (if (str/starts-with? (:uri request) "/api")
    (if (authenticated? request)
      (ring-utils/json-response {:error "Unauthorized"} :status 403)
      (ring-utils/json-response {:error "Unauthorized"} :status 401))
    (let [{:keys [auth-error is-a-user email-verified]} (:session request)]
      (cond
        (false? is-a-user) (response/redirect "/auth/not-a-user")
        (false? email-verified) (response/redirect "/auth/verify-email")
        auth-error (response/redirect "/auth/auth-error")
        :else (response/redirect "/auth/unauthorized")))))

; Create an instance of session auth backend.
(def session-auth-backend
  (session-backend {:unauthorized-handler unauthorized-handler}))

; Checks if the logged in user has been added to the database
; if their email is verified and if there were any errors during authentication
(defn auth-successful?
  [request]
  (let [{:keys [auth-error is-a-user email-verified]} (:session request)]
    (and is-a-user email-verified (not auth-error))))

; Admin access handler.
; Checks if the session roles has the admin role in it.
(defn admin-access
  [request]
  (let [roles (get-in request [:session :roles])]
    (contains? roles (config/application-admin-role))))

(defn admin-or-editor-access
  [request]
  (let [roles (get-in request [:session :roles])]
    (or
      (contains? roles (config/application-admin-role))
      (contains? roles (config/application-editor-role)))))

(defn internal-access
  [request]
  (let [roles (get-in request [:session :roles])]
    (or
      (contains? roles (config/application-admin-role))
      (contains? roles (config/application-editor-role))
      (contains? roles (config/application-viewer-role)))))

(defn site-access
  [request]
  (let [roles (get-in request [:session :roles])]
    (or
      (contains? roles (config/site-admin-role))
      (contains? roles (config/site-coordinator-role)))))

(defn valid-api-key?
  [request]
  (let [auth-header (get-in request [:headers "authorization"])
        api-key (if auth-header (second (str/split auth-header #" ")) nil)]
    (= api-key (config/api-key))))

; Open access. Always returns true.
(defn open-access
  [_request]
  true)

; Access rules for the buddy-auth system
(def rules
  [{:uris           ["/" "/auth/*" "/manifest" "/api/" "/api/study" "/api/cohort" "/api/timepoint" "/api/kit-type"
                     "/api/sample-type" "/api/form-type-fields" "/index.html" "/favicon.ico" "/api/health" "/api/site"
                     "/cljs-out/main.js" "/css/*" "/oauth2/*" "/__/auth/*" "/api/current-user" "/share/*" "/images/*"
                     "/api/user/current" "/api/kit/submitted" "/api/firebase-credentials"]
    :handler        open-access
    :request-method :get}
   {:uris           ["/api/kit" "/api/log-in" "/api/log-out"]
    :handler        open-access
    :request-method :post}
   {:uri            "/api/kit/share"
    :handler        open-access
    :request-method #{:post :patch :get}}
   {:uris           ["/console" "/console/kit/list" "/console/kit/propose/new/*" "/api/kit"]
    :handler        {:and [auth-successful? {:or [internal-access site-access]}]}
    :request-method #{:get}}
   {:uri            "/api/kit/propose-edit"
    :handler        {:and [auth-successful? {:or [internal-access site-access]}]}
    :request-method #{:post :get}}
   {:uris           ["/console/*" "/api/history"]
    :handler        {:and [auth-successful? internal-access]}
    :request-method #{:get}}
   {:uri            "/api/sample/export"
    :handler        {:or [valid-api-key? {:and [auth-successful? internal-access]}]}
    :request-method #{:get}}
   {:uri            "/api/kit/set-archived"
    :handler        {:and [auth-successful? admin-or-editor-access]}
    :request-method #{:patch}}
   {:uris           ["/console/kit/propose/*" "/api/kit/propose-edit*"]
    :handler        admin-or-editor-access
    :request-method #{:get :post}}
   {:uris           ["/api/upload/*" "/api/site" "/api/study" "/api/cohort" "/api/timepoint" "/api/kit-type" "/api/kit"
                     "/api/sample-type" "/api/set-active" "/api/user" "/api/user/*" "/api/role" "/api/configuration"]
    :handler        {:and [auth-successful? admin-access]}
    :request-method #{:post :patch :get :delete}}])

(defn wrap-auth
  [handler options]
  (let [buddy-auth-handler (-> handler
                               (wrap-access-rules {:rules  rules
                                                   :policy :reject})
                               (wrap-authorization session-auth-backend)
                               (wrap-authentication session-auth-backend))]
    (if-let [auth-wrapper-override (:auth-wrapper options)]
      (auth-wrapper-override buddy-auth-handler)
      buddy-auth-handler)))