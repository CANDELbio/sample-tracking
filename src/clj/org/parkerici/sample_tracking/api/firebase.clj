(ns org.parkerici.sample-tracking.api.firebase
  (:require [clojure.string :as str]
            [org.parkerici.sample-tracking.api.iam :as iam]
            [taoensso.timbre :as log])
  (:import [com.google.firebase FirebaseApp FirebaseOptions]
           [com.google.auth.oauth2 GoogleCredentials]
           [com.google.firebase.auth FirebaseAuth]))

(defn get-authorization-jwt
  [request]
  (when-let [authorization-header (get-in request [:headers "authorization"])]
    (let [split-header (str/split authorization-header #" " 2)]
      (when (= (first split-header) "Bearer")
        (second split-header)))))

(defn check-initialize-firebase
  []
  (when (empty? (FirebaseApp/getApps))
    (let [firebase-options (-> (FirebaseOptions/builder)
                               (.setCredentials (GoogleCredentials/getApplicationDefault))
                               (.build))]
      (FirebaseApp/initializeApp firebase-options))))

(defn verify-token
  "Verifies that the passed in JWT is valid.
  If it's valid, returns a decoded FirebaseToken"
  [token]
  (check-initialize-firebase)
  (-> (FirebaseAuth/getInstance)
      (.verifyIdToken token true)))

(defn process-firebase-jwt-request
  [session request-jwt]
  (let [decoded-jwt (verify-token request-jwt)
        firebase-email (.getEmail decoded-jwt)
        user (iam/get-user firebase-email)
        is-a-user (and (some? user) (not (:deactivated user)))
        email-verified (.isEmailVerified decoded-jwt)
        roles (set (iam/get-users-roles firebase-email))]
    (merge session
           {:identity firebase-email :roles roles :is-a-user is-a-user :email-verified email-verified})))

(defn add-firebase-auth-to-session
  [session firebase-jwt]
  (try
    (process-firebase-jwt-request session firebase-jwt)
    (catch Exception e
      (log/error e)
      (assoc session :auth-error true))))

(defn remove-firebase-auth-from-session
  [session]
  (dissoc session :identity :roles :is-a-user :email-verified))