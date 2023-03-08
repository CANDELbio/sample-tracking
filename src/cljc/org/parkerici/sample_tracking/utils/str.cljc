(ns org.parkerici.sample-tracking.utils.str
  (:require [clojure.string :as str]))

(def blank? str/blank?)

(def not-blank? (complement str/blank?))
