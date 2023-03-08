(ns org.parkerici.sample-tracking.api.kit-type
  "This file is focused on taking in a csv with kit and sample type definitions
  along with the studies, cohorts, and timepoints they belong to,
  parsing that csv, and then creating the appropriate values in the database."
  (:require [org.parkerici.sample-tracking.utils.csv :as csv]
            [org.parkerici.sample-tracking.configuration :as config]
            [clojure.string :as str]
            [clojure.set :as set]
            [org.parkerici.sample-tracking.db.study :as study-db]
            [org.parkerici.sample-tracking.db.cohort :as cohort-db]
            [org.parkerici.sample-tracking.db.kit-type :as kit-type-db]
            [org.parkerici.sample-tracking.db.timepoint :as timepoint-db]
            [org.parkerici.sample-tracking.db.sample-type :as sample-type-db]
            [org.parkerici.sample-tracking.db.sample-attribute :as sample-attribute-db]))

; Used with filter to remove rows in the input CSV that don't have the required values.
(defn row-has-required-values
  [row]
  (not (or (empty? (:study-name row))
           (empty? (:cohort-name row))
           (empty? (:kit-item-no row))
           (empty? (:kit-name row))
           (empty? (:sample-id-suffix row))
           (empty? (:sample-name row))
           (empty? (:kit-timepoints row))
           (empty? (:ships-with-kit row)))))

(defn read-csv
  [fpath]
  (filter row-has-required-values (drop 1 (csv/read-csv-file fpath))))

(defn find-or-create-study
  [study-name]
  (or (:uuid (study-db/find-study-by-name study-name)) (study-db/create-study study-name)))

(defn find-or-create-cohort-and-add-to-study
  [study-uuid cohort-name]
  (let [cohort-uuid (or (:uuid (cohort-db/find-cohort-by-name-and-study cohort-name study-uuid) (cohort-db/create-cohort cohort-name study-uuid)))]
    (when-not (study-db/cohort-associated-with-study study-uuid cohort-uuid)
      (study-db/add-cohort-to-study study-uuid cohort-uuid))
    cohort-uuid))

(defn create-kit-type-and-add-to-cohort
  [cohort-uuid kit-name kit-item-number vendor-email collection-date-required air-waybill-required]
  (let [kit-type-uuid (kit-type-db/create-kit-type kit-name kit-item-number vendor-email collection-date-required air-waybill-required)]
    (cohort-db/add-kit-type-to-cohort cohort-uuid kit-type-uuid)
    kit-type-uuid))

(defn add-attribute-to-sample-type
  [sample-type-uuid attribute value]
  (let [attribute-uuid (or (sample-attribute-db/find-sample-attribute attribute) (sample-attribute-db/create-sample-attribute attribute))
        value-uuid (or (sample-attribute-db/find-sample-attribute-value value attribute-uuid) (sample-attribute-db/create-sample-attribute-value value attribute-uuid))]
    (sample-type-db/add-attribute-value-to-sample-type sample-type-uuid value-uuid)))

(defn find-or-create-timepoint-and-add-to-kit-type
  [kit-type-uuid timepoint-name]
  (let [timepoint-uuid (or (timepoint-db/find-timepoint-uuid-from-name timepoint-name) (timepoint-db/create-timepoint timepoint-name))]
    (kit-type-db/add-timepoint-to-kit-type timepoint-uuid kit-type-uuid)))

(defn create-sample-type-and-add-to-kit-type
  [kit-type-uuid sample-name sample-id-suffix sample-ships-with-kit sample-reminder attributes-and-values]
  (let [sample-type-uuid (sample-type-db/create-sample-type sample-name sample-id-suffix sample-ships-with-kit sample-reminder)]
    (doseq [[attribute value] attributes-and-values]
      (add-attribute-to-sample-type sample-type-uuid attribute value))
    (kit-type-db/add-sample-type-to-kit-type sample-type-uuid kit-type-uuid)
    sample-type-uuid))

(defn parse-boolean
  [value]
  (case (str/lower-case value)
    "yes" true
    "no" false))

; Attributes and values are taken from any extra columns in the input CSV.
; The first n columns are expected to map to the column names in (config/csv-file-headers :kit-type)
; Any remaining columns after the first column are taken as attributes and values.
; The column header is used as the attribute and the cell value for the row is used as the value for that attribute.
(defn get-attributes-and-values
  [row]
  (let [attributes (set/difference (set (keys row)) (set (config/csv-file-headers :kit-type)))]
    (select-keys row attributes)))

(defn update-sample-types-map
  [row kit-type-map]
  (let [sample-types (or (:sample-types kit-type-map) [])
        sample-attributes-and-values (get-attributes-and-values row)
        sample-type {:name                  (:sample-name row)
                     :id-suffix             (:sample-id-suffix row)
                     :ships-with-kit        (parse-boolean (:ships-with-kit row))
                     :reminders             (:sample-reminders row)
                     :attributes-and-values sample-attributes-and-values}
        updated-sample-types (conj sample-types sample-type)]
    updated-sample-types))

(defn update-kit-type-map
  [row kit-name cohort-map]
  (let [kit-type-map (or (get cohort-map kit-name) {})
        timepoints (or (:timepoints kit-type-map) (csv/split-csv-string (:kit-timepoints row)))
        item-number (or (:item-number kit-type-map) (Integer/parseInt (:kit-item-no row)))
        vendor-email (or (:vendor-email kit-type-map) (:vendor-email row))
        collection-date-required (or (:collection-date-required kit-type-map) (not (= (str/lower-case (:collection-date-optional row)) "true")))
        air-waybill-required (or (:air-waybill-required kit-type-map) (not (= (str/lower-case (:air-waybill-optional row)) "true")))
        sample-types (update-sample-types-map row kit-type-map)
        updated-kit-type-map (-> kit-type-map
                                 (assoc :sample-types sample-types)
                                 (assoc :timepoints timepoints)
                                 (assoc :item-number item-number)
                                 (assoc :vendor-email vendor-email)
                                 (assoc :collection-date-required collection-date-required)
                                 (assoc :air-waybill-required air-waybill-required))]
    updated-kit-type-map))

(defn update-cohort-map
  [row cohort-name study-map]
  (let [cohort-map (or (get study-map cohort-name) {})
        kit-name (:kit-name row)
        kit-type-map (update-kit-type-map row kit-name cohort-map)
        updated-cohort-map (assoc cohort-map kit-name kit-type-map)]
    updated-cohort-map))

(defn update-study-map
  [row study-name m]
  (let [study-map (or (get m study-name) {})
        cohort-name (:cohort-name row)
        cohort-map (update-cohort-map row cohort-name study-map)
        updated-study-map (assoc study-map cohort-name cohort-map)]
    updated-study-map))

(defn build-type-map
  [csv-data]
  (reduce (fn [type-map row]
            (let [study-name (:study-name row)
                  study-map (update-study-map row study-name type-map)
                  updated-m (assoc type-map study-name study-map)]
              updated-m)) {} csv-data))

(defn process-sample-type-map
  [kit-type-uuid sample-type-map]
  (create-sample-type-and-add-to-kit-type kit-type-uuid
                                          (:name sample-type-map)
                                          (:id-suffix sample-type-map)
                                          (:ships-with-kit sample-type-map)
                                          (:reminders sample-type-map)
                                          (:attributes-and-values sample-type-map)))

(defn process-kit-type-map
  [cohort-uuid kit-type-name kit-type-map]
  (when-not (kit-type-db/find-active-kit-type-by-name-and-cohort kit-type-name cohort-uuid)
    (let [kit-type-uuid (create-kit-type-and-add-to-cohort
                          cohort-uuid kit-type-name (:item-number kit-type-map) (:vendor-email kit-type-map)
                          (:collection-date-required kit-type-map) (:air-waybill-required kit-type-map))]
      (doseq [timepoint (:timepoints kit-type-map)]
        (find-or-create-timepoint-and-add-to-kit-type kit-type-uuid timepoint))
      (doseq [sample-type (:sample-types kit-type-map)]
        (process-sample-type-map kit-type-uuid sample-type)))))

(defn process-cohort-map
  [study-uuid cohort-name cohort-map]
  (let [cohort-uuid (find-or-create-cohort-and-add-to-study study-uuid cohort-name)]
    (doseq [kit-type (keys cohort-map)]
      (process-kit-type-map cohort-uuid kit-type (get cohort-map kit-type)))))

(defn process-study-map
  [study-name study-map]
  (let [study-uuid (find-or-create-study study-name)]
    (doseq [cohort (keys study-map)]
      (process-cohort-map study-uuid cohort (get study-map cohort)))))

(defn process-type-map
  [type-map]
  (doseq [study (keys type-map)]
    (process-study-map study (get type-map study))))

(defn parse-kit-type-csv-and-save-to-db
  [fpath]
  (let [csv-headers (config/csv-file-headers :kit-type)
        csv-data (csv/read-csv-into-map fpath csv-headers row-has-required-values)
        type-map (build-type-map csv-data)]
    (process-type-map type-map)))