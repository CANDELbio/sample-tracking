(ns org.parkerici.sample-tracking.views
  (:require [re-frame.core :refer [subscribe]]
            [org.parkerici.sample-tracking.pages.console]
            [org.parkerici.sample-tracking.pages.not-found]
            [org.parkerici.sample-tracking.pages.blank]
            [org.parkerici.sample-tracking.pages.type.list]
            [org.parkerici.sample-tracking.pages.type.upload]
            [org.parkerici.sample-tracking.pages.kit.list]
            [org.parkerici.sample-tracking.pages.kit.edit]
            [org.parkerici.sample-tracking.pages.kit.propose-edits.form]
            [org.parkerici.sample-tracking.pages.kit.propose-edits.list]
            [org.parkerici.sample-tracking.pages.kit.propose-edits.view]
            [org.parkerici.sample-tracking.pages.kit.view]
            [org.parkerici.sample-tracking.pages.kit.shipment]
            [org.parkerici.sample-tracking.pages.kit.manifest]
            [org.parkerici.sample-tracking.pages.history]
            [org.parkerici.sample-tracking.pages.user.list]
            [org.parkerici.sample-tracking.pages.configuration.list]
            [org.parkerici.sample-tracking.pages.auth.unauthorized]
            [org.parkerici.sample-tracking.pages.auth.log-in]
            [org.parkerici.sample-tracking.pages.auth.error]
            [org.parkerici.sample-tracking.pages.auth.not-a-user]
            [org.parkerici.sample-tracking.pages.auth.log-out]
            [org.parkerici.sample-tracking.pages.auth.verify-email]
            [org.parkerici.sample-tracking.pages.auth.recover-email]
            [org.parkerici.sample-tracking.pages.auth.reset-password]
            [org.parkerici.sample-tracking.pages.auth.email-handler]))

(defmulti page (fn [name _] name))

(defmethod page :console [_ _]
  [org.parkerici.sample-tracking.pages.console/page])

(defmethod page :kit-shipment [_ _]
  [org.parkerici.sample-tracking.pages.kit.shipment/page])

(defmethod page :shared-kit-shipment [_ _]
  [org.parkerici.sample-tracking.pages.kit.shipment/page])

(defmethod page :kit-manifest [_ _]
  [org.parkerici.sample-tracking.pages.kit.manifest/page])

(defmethod page :type-upload [_ _]
  [org.parkerici.sample-tracking.pages.type.upload/page])

(defmethod page :type-list [_ _]
  [org.parkerici.sample-tracking.pages.type.list/page])

(defmethod page :kit-list [_ _]
  []
  [org.parkerici.sample-tracking.pages.kit.list/page])

(defmethod page :kit-edit [_ _]
  []
  [org.parkerici.sample-tracking.pages.kit.edit/page])

(defmethod page :propose-kit-edits [_ _]
  []
  [org.parkerici.sample-tracking.pages.kit.propose-edits.form/page])

(defmethod page :proposed-kit-edit-list [_ _]
  []
  [org.parkerici.sample-tracking.pages.kit.propose-edits.list/page])

(defmethod page :proposed-kit-edit-view [_ _]
  []
  [org.parkerici.sample-tracking.pages.kit.propose-edits.view/page])

(defmethod page :kit-view [_ _]
  []
  [org.parkerici.sample-tracking.pages.kit.view/page])

(defmethod page :history [_ _]
  []
  [org.parkerici.sample-tracking.pages.history/page])

(defmethod page :entity-history [_ _]
  []
  [org.parkerici.sample-tracking.pages.history/page])

(defmethod page :user-list [_ _]
  []
  [org.parkerici.sample-tracking.pages.user.list/page])

(defmethod page :config-list [_ _]
  []
  [org.parkerici.sample-tracking.pages.configuration.list/page])

(defmethod page :unauthorized [_ _]
  [org.parkerici.sample-tracking.pages.auth.unauthorized/page])

(defmethod page :auth-error [_ _]
  [org.parkerici.sample-tracking.pages.auth.error/page])

(defmethod page :verify-email [_ _]
  [org.parkerici.sample-tracking.pages.auth.verify-email/page])

(defmethod page :recover-email [_ _]
  [org.parkerici.sample-tracking.pages.auth.recover-email/page])

(defmethod page :reset-password [_ _]
  [org.parkerici.sample-tracking.pages.auth.reset-password/page])

(defmethod page :not-a-user [_ _]
  [org.parkerici.sample-tracking.pages.auth.not-a-user/page])

(defmethod page :log-out [_ _]
  [org.parkerici.sample-tracking.pages.auth.log-out/page])

(defmethod page :log-in [_ _]
  [org.parkerici.sample-tracking.pages.auth.log-in/page])

(defmethod page :email-handler [_ _]
  [org.parkerici.sample-tracking.pages.auth.email-handler/page])

(defmethod page :default [_ _]
  [org.parkerici.sample-tracking.pages.blank/page])

(defmethod page :not-found [_ _]
  [org.parkerici.sample-tracking.pages.not-found/page])

(defn main []
  (fn []
    (let [[key params] @(subscribe [:page])]
      [:div (page key params)])))
