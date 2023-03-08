(ns org.parkerici.sample-tracking.db.form-value
  (:require [org.parkerici.sample-tracking.db.datomic :as d]
            [org.parkerici.sample-tracking.db.core :as db]))

(defn create-or-update-form-value
  [uuid field field-type value]
  (let [uuid-to-return (or uuid (db/squuid))
        txn (cond-> {:form-value/field [:form-type-field/uuid field]}
                    uuid (assoc :db/id [:form-value/uuid uuid])
                    (nil? uuid) (assoc :form-value/uuid uuid-to-return)
                    (= field-type "boolean") (assoc :form-value/value_boolean value)
                    (= field-type "int") (assoc :form-value/value_long value)
                    (= field-type "select") (assoc :form-value/value_string value)
                    (= field-type "string") (assoc :form-value/value_string value)
                    (= field-type "time") (assoc :form-value/value_instant value))]
    (db/transact [txn])
    uuid-to-return))

(defn list-form-values
  [config-map]
  (let [kit-uuid (:uuid config-map)
        query {:find  '[?kit-uuid ?uuid ?field-id ?value ?type]
               :keys  '[kit-uuid uuid field-id value field-type]
               :where '[[?kit :kit/uuid ?kit-uuid]
                        [?kit :kit/form-values ?form-value]
                        [?form-value :form-value/uuid ?uuid]
                        [?form-value :form-value/field ?field]
                        [?field :form-type-field/field-id ?field-id]
                        [?field :form-type-field/field-type ?field-type]
                        [?field-type :db/doc ?type]
                        [(get-some $ ?form-value
                                   :form-value/value_string :form-value/value_long :form-value/value_float
                                   :form-value/value_instant :form-value/value_boolean) [_attr ?value]]]}
        query-fn (if-let [tx-id (:tx-id config-map)] (d/q-as-of tx-id) d/q-latest)
        results (query-fn query)]
    (cond-> results
            kit-uuid (db/reducer-filter :kit-uuid kit-uuid))))