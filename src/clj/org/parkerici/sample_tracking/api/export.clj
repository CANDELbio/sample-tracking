(ns org.parkerici.sample-tracking.api.export
  (:require [clojure.string :as string]
            [org.parkerici.sample-tracking.configuration :as c]
            [org.parkerici.sample-tracking.db.form-value :as form-value-db]
            [org.parkerici.sample-tracking.db.sample :as sample-db]
            [org.parkerici.sample-tracking.db.sample-attribute :as sample-attribute-db]
            [org.parkerici.sample-tracking.db.sample-type :as sample-type-db]
            [org.parkerici.sample-tracking.db.timepoint :as timepoint-db]
            [org.parkerici.sample-tracking.utils.csv :as csv]
            [org.parkerici.sample-tracking.utils.date-time :as dt]))

(defn build-timepoint-map
  [config-map]
  (reduce (fn [m value]
            (let [kit-uuid (:kit-uuid value)
                  cur-values (or (get m kit-uuid) [])
                  updated-values (conj cur-values (:timepoint-name value))]
              (assoc m kit-uuid updated-values))) {} (timepoint-db/list-kit-timepoints config-map)))

(defn add-timepoints-to-samples
  "Add attributes and their values to a map of samples
  Samples should be a list of maps that all have the key :sample-id
  Timepoints should be a list of maps that have they keys :sample-id and :timepoint-name
  Returns the samples list with the timepoint-names that map to each sample added as a comma separated list
  under the key :timepoints."
  [timepoint-map samples]
  (map (fn [sample]
         (let [kit-uuid (:kit-uuid sample)
               timepoint-names (string/join ", " (sort (get timepoint-map kit-uuid)))]
           (assoc sample :timepoints timepoint-names))) samples))

(defn build-sample-join-map
  [values-to-join join-key]
  (reduce (fn [value-map value]
            (let [join-key-value (get value join-key)
                  existing-values (or (get value-map join-key-value) [])
                  updated-values (conj existing-values value)]
              (assoc value-map join-key-value updated-values))) {} values-to-join))

(defn join-to-sample-map
  "Add the values elements in values to join to the sample maps in samples.
  Samples should be a list of maps that all have the key :sample-id
  values-to-join should be a list of maps that have they keys join-key, id-key.
  Calls the join-fn on sample and a list of the values in values-to-join with the same join-key.
  Expects the join-fn to returned the sample map joined with corresponding values from values-to-join.
  Returns the list of samples after joining with the values-to-join and applying join-fn."
  [join-map join-key join-fn samples]
  (map (fn [sample]
         (let [join-key-value (get sample join-key)
               joining-values (get join-map join-key-value)]
           (join-fn sample joining-values))) samples))

(defn parse-and-split-collection-timestamp
  [samples]
  (map (fn [sample]
         (let [timezone (:timezone sample)
               collection-timestamp (:collection-timestamp sample)]
           (-> sample
               (assoc :collection-date (when collection-timestamp (dt/generate-date-string collection-timestamp timezone)))
               (assoc :collection-time (when collection-timestamp (dt/generate-time-string collection-timestamp timezone)))
               (dissoc :collection-timestamp))))
       samples))

(defn attributes-and-values-join-fn
  [sample values]
  (reduce (fn [sample value]
            (assoc sample (:attribute value) (:value value))) sample values))

(defn kit-form-values-join-fn
  [sample values]
  (if (:collected sample)
    (reduce (fn [sample value]
              (if (= (:field-type value) "time")
                (assoc sample
                  (:field-id value)
                  (dt/generate-time-string (:value value) (:timezone sample)))
                (assoc sample
                  (:field-id value)
                  (:value value))))
            sample values)
    sample))

(defn kit-type->sample-type-map-reduce-fn
  "A reduce function that expects a sample-type map as input.
  Builds an output map of the form
  {kit-type-uuid: {sample-type-uuid: {:name 'sample-type-name' :id-suffix 'sample-type-id-suffix'}}"
  [m sample-type]
  (let [kit-type-uuid (:kit-type-uuid sample-type)
        cur-values (or (get m kit-type-uuid) {})
        sample-type-map {:name   (:name sample-type)
                         :suffix (:id-suffix sample-type)}
        updated-values (assoc cur-values
                         (:uuid sample-type)
                         sample-type-map)]
    (assoc m kit-type-uuid updated-values)))

(defn kit->sample-map-reduce-fn
  "A reduce function that expects a sample map as an input.
  Builds an output map of the form {kit-uuid: {sample-type-uuid: sample}}"
  [m sample]
  (let [kit-uuid (:kit-uuid sample)
        cur-values (or (get m kit-uuid) {})
        updated-values (assoc cur-values
                         (:sample-type-uuid sample) sample)]
    (assoc m kit-uuid updated-values)))

(defn kit-sample-type-uuids
  "For a given kit, finds the kit-type for that kit and then finds the sample-types for that kit-type.
  Returns a list of the sample-type-uuids for a given kit."
  [kit-uuid kit-type-sample-type-map kit-sample-map]
  (let [samples-map (get kit-sample-map kit-uuid)
        first-sample (first (vals samples-map))
        kit-type-uuid (get first-sample :kit-type-uuid)
        kit-sample-types (get kit-type-sample-type-map kit-type-uuid)]
    (keys kit-sample-types)))

(defn build-uncollected-sample
  "Given a kit and a sample-type for that kit, builds an uncollected sample by taking the first collected sample for
  that kit and then clearing any sample specific information and adding the correct sample-type-name and sample-id."
  [kit-uuid sample-type-uuid kit-type-sample-type-map kit-sample-map]
  (let [samples-map (get kit-sample-map kit-uuid)
        first-sample (first (vals samples-map))
        kit-type-uuid (get first-sample :kit-type-uuid)
        cur-sample-type (get-in kit-type-sample-type-map [kit-type-uuid sample-type-uuid])]
    (-> first-sample
        (dissoc :collection-timestamp)
        (assoc :sample-type-name (:name cur-sample-type))
        (assoc :sample-id (str (:kit-id first-sample) (:suffix cur-sample-type)))
        (assoc :collected false)
        (assoc :shipped false)
        (assoc :air-waybill ""))))

(defn add-uncollected-samples
  "Expects a sample map and a boolean flag to add uncollected. Normally a sample map only contains samples that have
  been collected. If add-uncollected is true, iterates over the sample-types for the kits represented in samples
  and then generates samples for the uncollected samples. If add-uncollected is false just returns the passed in
  samples."
  [kit-type-sample-type-map add-uncollected]
  (fn [xf]
    (fn
      ([] (xf))
      ([processed-samples] (xf processed-samples))
      ([processed-samples new-samples]
       (if add-uncollected
         (let [kit-sample-map (reduce kit->sample-map-reduce-fn {} new-samples)
               full-kit-sample-map (for [cur-kit-uuid (keys kit-sample-map)
                                         cur-sample-type-uuid (kit-sample-type-uuids cur-kit-uuid kit-type-sample-type-map
                                                                                     kit-sample-map)]
                                     (if-let [cur-kit-sample (get-in kit-sample-map [cur-kit-uuid cur-sample-type-uuid])]
                                       cur-kit-sample
                                       (build-uncollected-sample cur-kit-uuid cur-sample-type-uuid kit-type-sample-type-map
                                                                 kit-sample-map)))]
           (xf processed-samples full-kit-sample-map))
         (xf processed-samples new-samples))))))

(defn remove-unused-columns
  [samples]
  (map #(apply dissoc % (c/sample-export-columns-to-drop)) samples))

; TODO - Could possibly use some refactoring here.
; Could be cleaner to get all kit types and sample types, and then iterate through all existing kits and samples.
; If a sample is present for a kit, get the sample type and fill in the collected sample's specific information.
; Otherwise we would emit a "base" uncollected sample.
;
; Readability isn't the best to decrease memory usage.
(defn get-samples-for-export
  "Gets all of the samples for export as a list of maps ready to pass to csv/write-csv-file.
  Config map can have a :uuid key to export only a specific kit, or an :include-uncollected key
  if we want to include any uncollected samples in the export."
  ([config-map]
   (let [samples (sample-db/list-samples-for-export config-map)
         kit-type-sample-type-map (reduce kit-type->sample-type-map-reduce-fn {} (sample-type-db/list-sample-types nil))
         timepoint-map (build-timepoint-map config-map)
         attributes-and-values-join-map (build-sample-join-map
                                          (sample-attribute-db/list-sample-attributes-and-values-for-export config-map)
                                          :sample-id)
         kit-form-join-map (build-sample-join-map
                             (form-value-db/list-form-values config-map)
                             :kit-uuid)
         sample-transducer (comp
                             (map (partial join-to-sample-map kit-form-join-map :kit-uuid kit-form-values-join-fn))
                             (map (partial join-to-sample-map attributes-and-values-join-map :sample-id attributes-and-values-join-fn))
                             (map (partial add-timepoints-to-samples timepoint-map))
                             (map parse-and-split-collection-timestamp)
                             (add-uncollected-samples kit-type-sample-type-map (:include-uncollected config-map)))
         ]
     (->> (transduce sample-transducer concat (vals (group-by :kit-uuid samples)))
          (sort-by (juxt :kit-uuid :sample-id))
          (remove-unused-columns)))))

(defn export-options
  []
  {:column-order (c/sample-export-column-order) :columns-to-rename (c/sample-export-columns-to-rename)})

(defn export-samples-to-csv
  [config-map csv-path]
  (csv/write-csv-file csv-path (get-samples-for-export config-map) (export-options)))

(defn export-samples-to-streaming-csv
  [config-map]
  (csv/csv-output-stream-fn (get-samples-for-export config-map) (export-options)))