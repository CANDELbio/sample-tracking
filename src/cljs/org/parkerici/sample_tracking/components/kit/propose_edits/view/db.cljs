(ns org.parkerici.sample-tracking.components.kit.propose-edits.view.db
  (:require [cljs.spec.alpha :as s]))

(s/def :proposed-edit/proposed-edit map?)

(s/def :proposed-edit/proposed-edit (s/keys :opt [:proposed-edit/proposed-edit]))