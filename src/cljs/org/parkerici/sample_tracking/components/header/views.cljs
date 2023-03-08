(ns org.parkerici.sample-tracking.components.header.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [org.parkerici.sample-tracking.routes :as routes]))

(defn log-in-out-link
  [user]
  (if (some? (:email user))
    [:a {:on-click #(dispatch [:auth/log-out])} "Log Out"]
    [:a {:href (routes/path-for :log-in)} "Log In"]))

(defn console-link
  [user]
  (let [{:keys [email is-a-user email-verified]} user]
    (when (and email is-a-user email-verified)
      [:a {:href (routes/path-for :console)} "Console"])))

(defn component
  []
  (fn []
    (let [user @(subscribe [:user])]
      [:div.header
       [:div.content
        [:img {:src "/images/logo_dark.png"}]
        [:div.title
         [:h1 "Sample Tracking"]]
        [:div.log-in-out-link (log-in-out-link user)]
        [:div.console-link (console-link user)]]
       [:div.bars
        [:img {:src "/images/bars.png"}]]])))
