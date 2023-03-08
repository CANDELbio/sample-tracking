(ns org.parkerici.sample-tracking.components.kit.list.db
  (:require [cljs.spec.alpha :as s]))

(s/def :kit-list/kits vector?)
(s/def :kit-list/filter string?)

(s/def :kit-list/kit-list (s/keys :opt [:kit-list/kits :kit-list/filter]))