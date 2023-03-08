(ns org.parkerici.sample-tracking.utils.time)

(defn timestamp-string-to-formatted-string
  [timestamp]
  (when (some? timestamp)
    (let [js-date (js/Date. timestamp)]
      (str (.toLocaleDateString js-date) " " (.format (js/dayjs js-date) "HH:mm")))))