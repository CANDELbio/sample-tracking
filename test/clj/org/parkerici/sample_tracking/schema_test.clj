(ns org.parkerici.sample-tracking.schema-test
  (:require [clojure.test :refer :all]
            [org.parkerici.sample-tracking.db.schema :as sc]
            [org.parkerici.alzabo.schema :as alz]
            [org.parkerici.sample-tracking.test-utils :as tu]))

(deftest validate-schema
  ;; will throw error if invalid
  (alz/validate-schema sc/schema))

(deftest test-datomic-schema
  (tu/with-datomic-context
    (sc/transact-schema)))
