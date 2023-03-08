(ns org.parkerici.sample-tracking.api.study
  "This file deals with reading in a csv with studies along with validation information for participant and kit ids,
  parsing that csv, and then creating the appropriate associations of studies with validation information."
  (:require [org.parkerici.sample-tracking.utils.csv :as csv]
            [org.parkerici.sample-tracking.configuration :as config]
            [org.parkerici.sample-tracking.db.study :as study-db]
            [org.parkerici.sample-tracking.utils.str :as str]))

; Used with filter to remove rows in the input CSV that don't have the required values.
(defn row-has-required-values
  [row]
  (str/not-blank? (:study row)))

(defn add-validation-to-study
  [row]
  (let [{:keys [study participant-id-prefix participant-id-regex participant-id-validation-message kit-id-prefix
                kit-id-regex kit-id-validation-message]} row]
    (when (str/not-blank? participant-id-regex)
      (study-db/add-participant-id-validation-to-study study participant-id-prefix participant-id-regex participant-id-validation-message))
    (when (str/not-blank? kit-id-regex)
      (study-db/add-kit-id-validation-to-study study kit-id-prefix kit-id-regex kit-id-validation-message))))

(defn parse-study-csv-and-save-to-db
  [fpath]
  (let [csv-headers (config/csv-file-headers :study)
        csv-data (csv/read-csv-into-map fpath csv-headers row-has-required-values)]
    (doseq [row csv-data]
      (add-validation-to-study row))))