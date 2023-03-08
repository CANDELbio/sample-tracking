(ns org.parkerici.sample-tracking.db.form-type
  (:require [org.parkerici.sample-tracking.db.datomic :as d]
            [org.parkerici.sample-tracking.db.core :as db]))

;TODO - This errors silently if field-type is not one of the valid enums. Should raise an error.
(defn create-form-type-field
  [field-map]
  (let [uuid (db/squuid)
        {:keys [field-id type required label options]} field-map
        txn (cond-> {:form-type-field/uuid       uuid
                     :form-type-field/field-id   field-id
                     :form-type-field/field-type (keyword "form-type-field-type" type)
                     :form-type-field/required   required
                     :form-type-field/label      label}
                    (some? options) (assoc :form-type-field/options options))]
    (db/transact [txn])
    uuid))

(defn find-form-type
  [name]
  (ffirst (d/q-latest '[:find ?uuid
                        :in $ ?form-type-name
                        :where
                        [?form-type :form-type/name ?form-type-name]
                        [?form-type :form-type/uuid ?uuid]]
                      name)))

(defn create-form-type
  [name fields]
  (let [uuid (db/squuid)
        txn {:form-type/name name :form-type/uuid uuid :form-type/fields fields}]
    (db/transact [txn])
    uuid))

(defn process-form-type-field-pull
  [results]
  (map (fn [result]
         (let [result-map (first result)]
           (-> result-map
               (assoc :options (into {} (:options result-map)))
               (assoc :type (get-in result-map [:form-type-field/field-type :type]))
               (dissoc :form-type-field/field-type)))) results))

(defn get-form-type-fields
  [kit-type-uuid]
  (let [results (d/q-latest '[:find (pull ?field [[:form-type-field/uuid :as :uuid]
                                                  [:form-type-field/field-id :as :field-id]
                                                  [:form-type-field/required :as :required]
                                                  [:form-type-field/label :as :label]
                                                  [:form-type-field/options :as :options]
                                                  {:form-type-field/field-type [[:db/doc :as :type]]}])
                              :in $ ?kit-type-uuid
                              :where
                              [?kit-type :kit-type/uuid ?kit-type-uuid]
                              [?kit-type :kit-type/form-type ?form-type]
                              [?form-type :form-type/fields ?field]] kit-type-uuid)]
    (process-form-type-field-pull results)))

