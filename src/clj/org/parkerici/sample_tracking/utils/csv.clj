(ns org.parkerici.sample-tracking.utils.csv
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.data.csv :as csv]))

; Might be worth switching to https://github.com/jimpil/clj-bom if we have more encoding issues in the future.
(defn read-csv-file
  "Encoding can be any valid encoding like 'UTF-8' or 'UTF-16LE'. Defaults to 'UTF-8'.
  Delimiter for read-csv-file defaults to comma, but can be any character (e.g. \tab)"
  [fpath & {:keys [encoding delimiter] :or {delimiter \,}}]
  (with-open [reader (io/reader fpath :encoding encoding)]
    (doall
      (csv/read-csv reader :separator delimiter))))

(defn split-csv-string
  [value]
  (doall (map str/trim (str/split value #","))))

(defn generate-raw-headers
  "Generates a list of all of the distinct keys from the maps in data-seq as strings"
  [data-seq column-order]
  (let [headers-set (set (reduce (fn [headers row] (concat headers (keys row))) [] data-seq))
        headers-missing-from-order (set/difference headers-set (set column-order))]
    (concat column-order (vec headers-missing-from-order))))

(defn generate-csv-rows
  [headers data-seq]
  (map (fn [row] (mapv row headers)) data-seq))

(defn generate-csv-data
  "Takes input of a seq of maps
  Outputs a seq of seqs of the format csv/write-csv expects.
  The first seq being the header and the rest being data.
  Expects a list or nil for column order. If passed in uses it to order the columns.
  Any missing columns are appended to the end in random order.
  Expects map or nil for renamed-columns. If passed in uses the values in the map to rename the columns.
  Otherwise uses (name)."
  [data-seq column-order columns-to-rename]
  (let [raw-headers (generate-raw-headers data-seq column-order)
        string-headers (map (fn [header]
                              (let [renamed-header (get columns-to-rename header)]
                                (if renamed-header
                                  renamed-header
                                  (name header)))) raw-headers)]
    (cons string-headers (generate-csv-rows raw-headers data-seq))))

(defn csv-output-stream-fn
  "Takes input of a seq of maps
  Outputs a function that writes the csv data to a stream"
  [data-seq options]
  (let [csv-data (generate-csv-data data-seq (:column-order options) (:columns-to-rename options))]
    (fn [out-stream] (csv/write-csv out-stream csv-data)
      (.flush out-stream))))

(defn write-csv-file
  [fpath data-seq options]
  (with-open [w (io/writer fpath)]
    (doall
      (csv/write-csv w (generate-csv-data data-seq (:column-order options) (:columns-to-rename options))))))

(defn read-csv-into-map
  "Reads a CSV at fpath. Expects it to have a header
  Returns a list of maps where each column value is keyed on the csv-headers passed in
  If csv-headers is empty or missing values will use the column/header names from the file.
  Applies required-values-fn to each map value to filter out bad rows."
  [fpath csv-headers required-values-fn]
  (let [raw-csv (read-csv-file fpath)
        csv-header (first raw-csv)
        extra-headers (subvec csv-header (count csv-headers))
        all-headers (concat csv-headers extra-headers)
        csv-rows (drop 1 raw-csv)
        csv-map (map #(zipmap all-headers %) csv-rows)]
    (filter required-values-fn csv-map)))