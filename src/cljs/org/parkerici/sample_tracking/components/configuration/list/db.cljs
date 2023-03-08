(ns org.parkerici.sample-tracking.components.configuration.list.db
  (:require [cljs.spec.alpha :as s]))

(s/def :configuration-list/configuration-map map?)

(s/def :configuration-list/configuration-list (s/keys :opt [:configuration-list/configuration-map]))
