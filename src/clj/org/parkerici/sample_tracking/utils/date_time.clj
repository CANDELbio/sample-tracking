(ns org.parkerici.sample-tracking.utils.date-time
  (:require [java-time :as time]
            [org.parkerici.sample-tracking.utils.str :as str]))

(defn parse-zoned-date-time
  [date-time-string timezone-string]
  (let [timezone-id (time/zone-id timezone-string)]
    (time/zoned-date-time (time/java-date date-time-string) timezone-id)))

(defn timestamp-parsable
  [timestamp]
  (and (some? timestamp) (if (string? timestamp) (str/not-blank? timestamp) true)))

(defn generate-date-string
  [timestamp timezone]
  (when (timestamp-parsable timestamp)
    (time/format "MM/dd/yyyy" (parse-zoned-date-time timestamp timezone))))

(defn generate-time-string
  [timestamp timezone]
  (when (timestamp-parsable timestamp)
    (time/format "HH:mm" (parse-zoned-date-time timestamp timezone))))