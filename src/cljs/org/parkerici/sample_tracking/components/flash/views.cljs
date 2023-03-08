(ns org.parkerici.sample-tracking.components.flash.views
  (:require [re-frame.core :refer [dispatch subscribe]]
            [org.parkerici.sample-tracking.components.flash.db]
            [org.parkerici.sample-tracking.components.flash.events]
            [org.parkerici.sample-tracking.components.flash.subs]))

(defn close-button
  []
  [:button.close
   {:type         "button"
    :data-dismiss "alert"
    :aria-label   "Close"
    :on-click     (fn [evt]
                    (.preventDefault evt)
                    (dispatch [:flash/clear-flash]))}
   [:span {:aria-hidden "true"} "✖"]])

(defn component
  []
  (fn []
    (if-let [data @(subscribe [:flash/flash])]
      [:div {:class (str "alert alert-" (:flash/priority data))
             :role  "alert"}
       [:span {:dangerouslySetInnerHTML {:__html (:flash/message data)}}]
       [close-button]])))
