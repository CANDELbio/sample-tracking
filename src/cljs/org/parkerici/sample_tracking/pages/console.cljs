(ns org.parkerici.sample-tracking.pages.console
  (:require
    [re-frame.core :refer [subscribe]]
    [org.parkerici.sample-tracking.utils.user :as user-utils]
    [org.parkerici.sample-tracking.components.header.views :as header]
    [org.parkerici.sample-tracking.routes :as routes]))

(def console-pages
  [[:all :kit-shipment "Kit Shipment Form"]
   [:view :kit-list "List Kits"]
   [:site :kit-list "Propose Kit Edits"]
   [:admin :proposed-kit-edit-list "View Proposed Edits"]
   [:view :history "Audit History"]
   [:admin :type-list "List Types"]
   [:admin :type-upload "Upload Type Data"]
   [:admin :user-list "User List"]
   [:admin :config-list "Configuration List"]])

(defn page
  []
  (let [user @(subscribe [:user])
        user-is-admin (user-utils/user-is-admin user)
        user-can-view (or user-is-admin (user-utils/user-is-editor user) (user-utils/user-is-viewer user))
        site-user (user-utils/user-is-site-user user)]
    [:div
     [header/component]
     [:div.page-body
      [:h1 "Console"]
      (for [[access page label] console-pages]
        (when (case access
                :view user-can-view
                :admin user-is-admin
                :site site-user
                :all true)
          [:div {:key page} [:a {:href (routes/path-for page)} label]]))]]))