(ns org.parkerici.sample-tracking.components.auth.log-in.views
  (:require [re-frame.core :refer [dispatch dispatch-sync subscribe]]
            [reagent.core :as reagent]
            [react-firebase-auth]                           ;Firebase UI JS component https://www.npmjs.com/package/firebaseui
            [org.parkerici.sample-tracking.components.auth.set-password.views :as set-password]
            [org.parkerici.sample-tracking.utils.str :as str]))

(defn handle-set-email
  [state]
  (let [email (:email @state)]
    (.then (.fetchSignInMethodsForEmail (.auth js/firebase) email)
           (fn [sign-in-methods]
             (if (> (count sign-in-methods) 0)
               (swap! state assoc :new-user false)
               (swap! state assoc :new-user true))))))

(defn prevent-submit
  [e]
  (.preventDefault e)
  false)

(defn set-email-component
  [state]
  (let [email (or (:email @state) "")]
    [:form {:on-submit prevent-submit}
     [:table {:width "100%"}
      [:tbody
       [:tr
        [:td [:label "Email Address"]]
        [:td [:input.form-control {:type      "text"
                                   :name      "username"
                                   :id        "username"
                                   :value     email
                                   :on-change #(swap! state assoc :email (-> % .-target .-value))}]]]]]
     [:input.btn.btn-secondary
      {:type     "button"
       :on-click #(swap! state dissoc :email-log-in)
       :value    "Cancel"}]
     [:div.spacer]
     [:input.btn.btn-secondary
      {:type     "submit"
       :value    "Next"
       :disabled (not (re-matches #".+\@.+\..+" email))
       :on-click #(handle-set-email state)}]]))

(defn handle-create-email-account
  [state password]
  (let [email (:email @state)]
    (-> (.createUserWithEmailAndPassword (.auth js/firebase) email password)
        (.then (fn [userCredential]
                 (let [user (. userCredential -user)]
                   (.sendEmailVerification user)
                   (dispatch [:set-password/clear])
                   (-> (.getIdToken user true)
                       (.then #(dispatch-sync [:auth/log-in %]))))))
        (.catch (fn [error] (dispatch [:flash/request-error "Error! Firebase said: " (. error -message)]))))))

(defn create-email-account-component
  [state]
  (let [password @(subscribe [:set-password/new-password])
        submit-enabled @(subscribe [:set-password/valid-password])]
    [:<>
     [:table {:width "100%"}
      [:tbody
       [:tr
        [:td [:label "Email Address"]]
        [:td (:email @state)]]]]
     [set-password/component]
     [:button.btn.btn-secondary
      {:type     "button"
       :on-click #(swap! state dissoc :new-user)}
      "Cancel"]
     [:div.spacer]
     [:button.btn.btn-secondary
      {:type     "button"
       :disabled (not submit-enabled)
       :on-click #(handle-create-email-account state password)}
      "Save"]]))

(defn handle-email-log-in
  [state]
  (let [email (:email @state)
        password (:password @state)]
    (-> (.signInWithEmailAndPassword (.auth js/firebase) email password)
        (.then (fn [userCredential]
                 (let [user (. userCredential -user)]
                   (-> (.getIdToken user true)
                       (.then #(dispatch-sync [:auth/log-in %]))))))
        (.catch (fn [error] (dispatch [:flash/request-error "Error! Firebase said: " (. error -message)]))))))

(defn send-reset-email-fn
  [email]
  (fn []
    ((try
       (.sendPasswordResetEmail (.auth js/firebase) email)
       (. js/window alert "Passsword reset email has been sent. Please check your spam folder if you are unable to find it.")
       (catch :default _e
         (. js/window alert "There was an error sending a password reset email. Please contact an administrator."))
       (finally
         (.remove (.getElementById js/document "reset-link")))))))

(defn email-login-component
  [state]
  [:form {:on-submit prevent-submit}
   [:table {:width "100%"}
    [:tbody
     [:tr
      [:td [:label "Email Address"]]
      [:td (:email @state)]]
     [:tr
      [:td [:label "Password"]]
      [:td [:input.form-control {:type      "password"
                                 :name      "password"
                                 :id        "password"
                                 :value     (or (:password @state) "")
                                 :on-change #(swap! state assoc :password (-> % .-target .-value))}]]]]]
   [:input.btn.btn-secondary
    {:type     "button"
     :on-click #(swap! state dissoc :new-user)
     :value    "Cancel"}]
   [:div.spacer]
   [:input.btn.btn-secondary
    {:type     "button"
     :disabled (str/blank? (:password @state))
     :on-click #(handle-email-log-in state)
     :value    "Log In"}]
   [:div
    [:a {:on-click (send-reset-email-fn (:email @state)) :id "reset-link"} "Forgot password."]]])

(defn email-auth-component
  [state]
  (case (:new-user @state)
    true (create-email-account-component state)
    false (email-login-component state)
    (set-email-component state)))

; Bunch of style stuff to match the Google login component from FirebaseUI.
(defn sign-in-with-email-button
  [state]
  [:button.firebaseui-idp-button.mdl-button.mdl-js-button.mdl-button--raised.firebaseui-idp-password.firebaseui-id-idp-button
   {:style {:background-color "#db4437"} :on-click #(swap! state assoc :email-log-in true)}
   [:span.firebaseui-idp-icon-wrapper
    [:img.firebaseui-idp-icon {:src "https://www.gstatic.com/firebasejs/ui/2.0.0/images/auth/mail.svg"}]]
   [:span.firebaseui-idp-text.firebaseui-idp-text-long "Sign in with email"]])

(defn handle-firebaseui-sign-in-success
  [auth-result]
  (let [user (. auth-result -user)]
    (when (.. auth-result -additionalUserInfo -isNewUser)
      (.sendEmailVerification user))
    (.then
      (.getIdToken user true)
      #(dispatch-sync [:auth/log-in %])))
  false)

(def firebase-ui-config
  (clj->js {:signInFlow    "popup"
            :signInOptions [(.. js/firebase -auth -GoogleAuthProvider -PROVIDER_ID)]
            :callbacks     {:signInSuccessWithAuthResult handle-firebaseui-sign-in-success
                            :signInFailure               (fn [error]
                                                           (dispatch [:flash/request-error "Error! Firebase said: " (. error -message)]))}}))

; Gives the user a choice between logging in with Google OAuth through Firebase or email/password.
(defn log-in-choice-component
  [state]
  [:<> [:> react-firebase-auth
        {:ui-config     firebase-ui-config
         :firebase-auth (.auth js/firebase)}]
   (sign-in-with-email-button state)])

(defn component []
  (let [state (reagent/atom {})]
    (fn []
      ; This checks if the user has logged in.
      ; If they have, they are redirected to one of a few pages depending on their account status.
      (let [user @(subscribe [:user])
            {:keys [email is-a-user email-verified auth-error]} user]
        (cond
          (false? is-a-user) (dispatch-sync [:redirect :not-a-user])
          (false? email-verified) (dispatch-sync [:redirect :verify-email {:status "unverified"}])
          (true? auth-error) (dispatch-sync [:redirect :auth-error])
          email (dispatch-sync [:redirect :console])))
      (if (:email-log-in @state)
        (email-auth-component state)
        (log-in-choice-component state)))))
