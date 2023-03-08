(ns org.parkerici.sample-tracking.components.kit.propose-edits.list.db
  (:require [cljs.spec.alpha :as s]))

(s/def :proposed-edits-list/proposed-edits vector?)
(s/def :proposed-edits-list/filter string?)


(s/def :proposed-edits-list/proposed-edits-list (s/keys :opt [:proposed-edits-list/proposed-edits :proposed-edits-list/filter]))