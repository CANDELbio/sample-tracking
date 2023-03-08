(ns org.parkerici.sample-tracking.api.form-type
  "Form types are custom, configurable forms that are associated with
  kit types to collect information for that kit type outside of the
  default fields collected.

  This file is focused on taking in a csv with form type definitions
  along with the kit item numbers of the kits they are used for,
  parsing that csv, and then creating the appropriate values in the database."
  (:require [clojure.edn :as edn]
            [org.parkerici.sample-tracking.utils.csv :as csv]
            [org.parkerici.sample-tracking.configuration :as config]
            [org.parkerici.sample-tracking.db.kit-type :as kit-type-db]
            [org.parkerici.sample-tracking.db.form-type :as form-type-db]))

; Used with filter to remove rows in the input CSV that don't have the required values.
(defn row-has-required-values
  [row]
  (not (or (empty? (:form-type-name row))
           (empty? (:kit-item-no row)))))

; Does not create a form-type if form-type-fields is missing from the row.
(defn create-form-type
  [row]
  (when-not (empty? (:form-type-fields row))
    (let [raw-form-type-fields (edn/read-string (:form-type-fields row))
        form-type-fields (map #(vector :form-type-field/uuid (form-type-db/create-form-type-field %)) raw-form-type-fields)
        form-type (form-type-db/create-form-type (:form-type-name row) form-type-fields)]
    form-type)))

; Tries to find form-type by name. Returns it if found, otherwise creates it with row information.
(defn find-or-create-form-type
  [row]
  (let [form-type-uuid (form-type-db/find-form-type (:form-type-name row))]
    (if form-type-uuid
      form-type-uuid
      (create-form-type row))))

(defn create-row-in-db
  [row]
  (let [form-type (find-or-create-form-type row)
        kit-item-numbers (csv/split-csv-string (:kit-item-no row))]
    (doseq [kit-item-number kit-item-numbers]
      (when (and form-type
                 (not (kit-type-db/kit-type-has-form-type kit-item-number)))
        (kit-type-db/add-form-type-to-kit-type form-type (Integer/parseInt kit-item-number))))))

(defn parse-form-type-csv-and-save-to-db
  [fpath]
  (let [csv-headers (config/csv-file-headers :form-type)
        csv-data (csv/read-csv-into-map fpath csv-headers row-has-required-values)]
    (doseq [row csv-data] (create-row-in-db row))))