(ns org.parkerici.sample-tracking.db
  (:require [cljs.spec.alpha :as s]))

(s/def ::page keyword?)
(s/def ::route-params map?)
(s/def ::query-params map?)
(s/def ::user map?)
(s/def ::firebase-initialized boolean?)

(s/def ::db (s/keys :req [::page ::route-params ::user ::firebase-initialized]
                    :opt [:flash/flash :kit-form/kit-form :kit-list/kit-list :type-list/type-list :history/history]))

; Want to start with the default blank page so that it doesn't flicker between some initial page and the page requested.
(def default-db
  {::page                 :default
   ::route-params         {}
   ::query-params         {}
   ::user                 {}
   ::firebase-initialized false})
