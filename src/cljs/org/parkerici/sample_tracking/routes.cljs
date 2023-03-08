(ns org.parkerici.sample-tracking.routes
  (:require [bidi.bidi :as bidi]
            [pushy.core :as pushy]
            [cemerick.url :as cemerick]
            [re-frame.core :refer [dispatch]]))

(def app-routes
  ["/" {""               :kit-shipment
        "not-found"      :not-found
        ["share/" :uuid] :shared-kit-shipment
        "manifest"       :kit-manifest
        "auth"           {"/not-a-user"                  :not-a-user
                          "/unauthorized"                :unauthorized
                          "/auth-error"                  :auth-error
                          ["/verify-email/" :status]     :verify-email
                          ["/recover-email/" :status]    :recover-email
                          ["/reset-password/" :oob-code] :reset-password
                          "/log-out"                     :log-out
                          "/log-in"                      :log-in
                          "/email-handler"               :email-handler}
        "console"        {""         :console
                          "/history" {""          :history
                                      ["/" :uuid] :entity-history}
                          "/type"    {"/upload" :type-upload
                                      "/list"   :type-list}
                          "/kit"     {"/list"          :kit-list
                                      ["/view/" :uuid] :kit-view
                                      ["/edit/" :uuid] :kit-edit
                                      "/propose"       {["/new/" :uuid]  :propose-kit-edits
                                                        "/list"          :proposed-kit-edit-list
                                                        ["/view/" :uuid] :proposed-kit-edit-view}}
                          "/user"    :user-list
                          "/config"  :config-list}}])

(defn set-page! [route]
  (if route
    (dispatch [:set-active-page
               (:handler route)
               (or (:route-params route) {})
               (or (:query-params route) {})])
    (dispatch [:set-active-page :not-found {}])))

; Bidi doesn't support query params.
; This is a little work around to parse out the query params
; and then to merge them with the route params.
(defn match-route-with-query-params
  [route path & {:as options}]
  (let [url-record (cemerick/url path)
        query-params (->> (:query url-record)
                          (map (fn [[k v]] [(keyword k) v]))
                          (into {}))
        matched-route (bidi/match-route* route (:path url-record) options)]
    (assoc matched-route :query-params query-params)))

(def history
  (pushy/pushy set-page! (partial match-route-with-query-params app-routes)))

(defn initialize
  []
  (pushy/start! history))

(defn path-for
  [name & params]
  (apply bidi/path-for (concat [app-routes name] params)))

(defn redirect
  [path]
  (pushy/set-token! history path))