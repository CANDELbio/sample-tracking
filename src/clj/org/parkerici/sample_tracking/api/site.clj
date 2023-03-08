(ns org.parkerici.sample-tracking.api.site
  "This file deals with reading in a csv with sites along with the studies they are running,
  parsing that csv, and then creating the appropriate associations of sites with studies in the database."
  (:require [org.parkerici.sample-tracking.utils.csv :as csv]
            [org.parkerici.sample-tracking.configuration :as config]
            [org.parkerici.sample-tracking.db.study :as study-db]
            [org.parkerici.sample-tracking.db.site :as site-db]))

; Used with filter to remove rows in the input CSV that don't have the required values.
(defn row-has-required-values
  [row]
  (not (or (empty? (:site row))
           (empty? (:study-names row)))))

(defn find-or-create-site
  [name]
  (or (:id (site-db/find-site-by-name name)) (site-db/create-site name)))

(defn create-site
  [row]
  (let [site-uuid (find-or-create-site (:site row))
        study-names (csv/split-csv-string (:study-names row))]
    (doseq [study-name study-names]
      (when-not (study-db/site-is-associated-with-study site-uuid study-name)
        (study-db/add-site-to-study site-uuid study-name)))))

(defn parse-site-csv-and-save-to-db
  [fpath]
  (let [csv-headers (config/csv-file-headers :site)
        csv-data (csv/read-csv-into-map fpath csv-headers row-has-required-values)]
    (doseq [row csv-data] (create-site row))))