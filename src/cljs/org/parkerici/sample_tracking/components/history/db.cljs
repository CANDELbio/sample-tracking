(ns org.parkerici.sample-tracking.components.history.db
  (:require [cljs.spec.alpha :as s]))

(s/def :history/histories vector?)

(s/def :history/history (s/keys :opt [:history/histories]))
