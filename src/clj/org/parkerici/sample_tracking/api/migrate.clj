(ns org.parkerici.sample-tracking.api.migrate
  (:require [org.parkerici.sample-tracking.db.migration :as migration]
            [org.parkerici.sample-tracking.db.migration.air-waybill-required :as air-waybill-migration]
            [taoensso.timbre :as log]))

(defn migrate-kit-types-without-air-waybill-required
  []
  (doseq [to-migrate (air-waybill-migration/list-kit-types-without-air-waybill-required)]
    (air-waybill-migration/set-kit-type-air-waybill-required (:uuid to-migrate) true)))

(defn run-migration
  [name fn]
  (when-not (migration/migration-has-been-run name)
    (log/info "Running migration" name)
    (fn)
    (migration/create-migration name)))

(defn run-pending-migrations
  []
  (run-migration "add-kit-type-air-waybill" migrate-kit-types-without-air-waybill-required))