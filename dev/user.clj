(ns user
  (:require [figwheel.main.api :as fig]
            [org.parkerici.sample-tracking.api.export :as export]
            [org.parkerici.sample-tracking.api.iam :as auth]
            [org.parkerici.sample-tracking.configuration :as c]
            [org.parkerici.sample-tracking.db.datomic :as db-d]
            [org.parkerici.sample-tracking.db.kit-type :as kit-type-db]
            [org.parkerici.sample-tracking.db.schema :as schema]
            [org.parkerici.sample-tracking.db.site :as site-db]
            [org.parkerici.sample-tracking.server :as server])
  (:import (java.util UUID)))

(defn string->stream
  ([s] (string->stream s "UTF-8"))
  ([s encoding]
   (-> s
       (.getBytes encoding)
       (java.io.ByteArrayInputStream.))))

(comment
  :transact-schema
  (schema/transact-schema))

(comment
  :add-admin
  (db-d/wrap-datomic-fn #(doall (map auth/find-or-create-role (c/application-role-values))))
  (db-d/wrap-datomic-fn #(auth/add-role-to-user "rschiemann@parkerici.org" (c/application-admin-role))))

(comment
  :test-db-methods
  (clojure.pprint/pprint (db-d/wrap-datomic-fn #(site-db/list-study-tuples)))
  (clojure.pprint/pprint (db-d/wrap-datomic-fn #(site-db/list-all-sites)))
  (clojure.pprint/pprint (db-d/wrap-datomic-fn #(kit-type-db/find-active-kit-type-by-name-and-cohort "Blood Sample Collection Kit" (UUID/fromString "5fab14f6-f01c-4f96-bfd1-1b5629fccf17"))))
  )

(comment
  :test-export
  (clojure.pprint/pprint (db-d/wrap-datomic-fn #(export/get-samples-for-export {}))))


(comment
  :figwheel
  ; Starts figwheel and attempts to launch a repl. Use the below command if piggyback fails.
  (fig/start "dev")
  ; Starts figwheel without launching a repl
  (fig/start {:mode :serve} "dev")
  (fig/stop "dev"))

(comment
  :server-start-stop
  (server/start 5526)
  (server/stop))