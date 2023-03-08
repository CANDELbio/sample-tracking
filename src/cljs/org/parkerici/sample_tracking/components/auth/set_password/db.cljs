(ns org.parkerici.sample-tracking.components.auth.set-password.db
  (:require [cljs.spec.alpha :as s]))

(s/def :set-password/new-password string?)
(s/def :set-password/confirm-password string?)
(s/def :set-password/validity-map map?)

(s/def :set-password/set-password (s/keys :opt [:set-password/new-password
                                                :set-password/confirm-password
                                                :set-password/validity-map]))
