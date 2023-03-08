(ns ^:figwheel-hooks org.parkerici.sample-tracking.core
  (:require [reagent.dom :as reagent]
            [re-frame.core :as re-frame]
            [day8.re-frame.http-fx]
            [firebase]                                      ;Firebase JS component https://www.npmjs.com/package/firebase
            [cljsjs.firebase]                               ;Firebase cljsjs to prevent errors with advanced cljs transpilation
            [dayjs]                                         ;JS library https://www.npmjs.com/package/dayjs
            [org.parkerici.sample-tracking.events]
            [org.parkerici.sample-tracking.subs]
            [org.parkerici.sample-tracking.views :as views]
            [org.parkerici.sample-tracking.config :as config]
            [org.parkerici.sample-tracking.routes :as routes]
            [org.parkerici.sample-tracking.components.auth.events]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [:initialize-db])
  (re-frame/dispatch-sync [:initialize-firebase])
  (re-frame/dispatch-sync [:initialize-user-info])
  (routes/initialize)
  (dev-setup)
  (mount-root))
