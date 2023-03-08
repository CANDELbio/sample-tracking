(ns org.parkerici.sample-tracking.db.sample-attribute
  (:require [org.parkerici.sample-tracking.db.datomic :as d]
            [org.parkerici.sample-tracking.db.core :as db]))

(defn create-sample-attribute
  [name]
  (let [uuid (db/squuid)]
    (db/transact [{:sample-attribute/uuid uuid
                   :sample-attribute/name name}])
    uuid))

; There should only be one sample-attribute with a given name.
; Returns the uuid of the attribute with the passed in name if it exists.
(defn find-sample-attribute
  [name]
  (ffirst (d/q-latest '[:find ?uuid
                        :in $ ?attribute-name
                        :where
                        [?sample-attribute :sample-attribute/uuid ?uuid]
                        [?sample-attribute :sample-attribute/name ?attribute-name]]
                      name)))

(defn create-sample-attribute-value
  [name attribute-uuid]
  (let [uuid (db/squuid)]
    (db/transact [{:sample-attribute-value/uuid      uuid
                   :sample-attribute-value/name      name
                   :sample-attribute-value/attribute [:sample-attribute/uuid attribute-uuid]}])
    uuid))

; There should only be one attribute-value with a given name and attribute.
; Returns the uuid of the attribute-value with the passed in name and attribute if it exists.
(defn find-sample-attribute-value
  [name attribute-uuid]
  (ffirst (d/q-latest '[:find ?sample-attribute-value-uuid
                        :in $ ?value-name ?attribute-uuid
                        :where
                        [?sample-attribute-value :sample-attribute-value/uuid ?sample-attribute-value-uuid]
                        [?sample-attribute-value :sample-attribute-value/name ?value-name]
                        [?sample-attribute-value :sample-attribute-value/attribute ?attribute]
                        [?attribute :sample-attribute/uuid ?attribute-uuid]]
                      name attribute-uuid)))

(defn list-sample-attributes-and-values-for-export
  ([config-map]
   (let [kit-uuid (:uuid config-map)
         query {:find  '[?sample-id ?sample-attribute-name ?attribute-value-name]
                :keys  '[sample-id attribute value]
                :where '[[?kit :kit/uuid ?kit-uuid]
                         [?kit :kit/kit-id ?kit-id]
                         [?kit :kit/samples ?sample]
                         [?sample :sample/sample-id ?sample-id]
                         [?sample :sample/sample-type ?sample-type]
                         [?sample-type :sample-type/attribute-values ?attribute-value]
                         [?attribute-value :sample-attribute-value/name ?attribute-value-name]
                         [?attribute-value :sample-attribute-value/attribute ?sample-attribute]
                         [?sample-attribute :sample-attribute/name ?sample-attribute-name]]}
         results (d/q-latest query)]
     (cond-> results
             kit-uuid (db/reducer-filter :kit-uuid kit-uuid)))))