(ns org.parkerici.sample-tracking.pages.auth.email-handler
  (:require [re-frame.core :refer [subscribe dispatch]]
            [org.parkerici.sample-tracking.components.header.views :as header]))

; Hacky feeling workaround to make sure that firebase has been initialized before handling the email.
(defn page
  []
  (let [firebase-initialized @(subscribe [:firebase-initialized])]
    (when firebase-initialized (dispatch [:auth/email-handler])))
  [:div
   [header/component]])



