(ns org.parkerici.sample-tracking.db.datomic
  (:require [datomic.client.api :as d]
            [org.parkerici.multitool.core :as u]
            [org.parkerici.sample-tracking.configuration :as c]))

;;; Source https://gist.github.com/natural/871d7a3ddfb6ae5f72fb141e549ca3bb
(def ^{:dynamic true :doc "A Datomic database value used over the life of a Ring or CLI request."} *db*)
(def ^{:dynamic true :doc "A Datomic connection bound for the life of a Ring or CLI request."} *connection*)

(defn config
  []
  {:server-type        :peer-server
   :access-key         (c/datomic-access-key)
   :secret             (c/datomic-secret)
   :endpoint           (c/datomic-endpoint)
   :validate-hostnames (c/datomic-validate-hostnames)})

;;; Ben@Cognitect says that this caches behind the scenes, no need to do ourselves
(defn conn
  []
  (let [client (d/client (config))]
    (d/connect client {:db-name (c/datomic-db-name)})))

;;; In general this should not be used; instead, use wrap-datomic-fn or equivalent
(defn latest-db
  []
  (d/db (conn)))

(def default-timeout 60000)                                 ;Far too long for web app, need TODO performance tuning / paging

(defn q
  [query & args]
  ;  (prn :q query :args args :db *db*)
  (d/q {:query query :args (cons *db* args) :timeout 60000}))

; Chat with Mike about using this.
; If we don't query latest when creating a bunch of records from the uploaded files, we end up with duplicates instead of finding new values.
(defn q-latest
  [query & args]
  ;  (prn :q query :args args :db *db*)
  (d/q {:query query :args (cons (d/db *connection*) args) :timeout 60000}))

(defn pull
  [spec eid]
  (d/pull *db* spec eid))

(defn q-as-of
  "Return a function that works like q but on a historical version of the database"
  [as-of]
  (fn [query & args]
    (apply d/q query (d/as-of (d/db *connection*) as-of) args)))

(defn pull-as-of
  [as-of]
  (fn [spec eid]
    (d/pull (d/as-of (d/db *connection*) as-of) spec eid)))

(defn q-history
  [query & args]
  (d/q {:query query :args (cons (d/history *db*) args) :timeout 60000}))

(defn q1
  "Query for a single result. Errors if there is more than one row returned."
  [query & args]
  (let [res (apply q query args)]
    (if (> (count res) 1)
      (throw (Error. (str "Multiple results where at most one expected: " query " " res)))
      (first res))))

(defn q11
  "Query for a single value in a single result. Errors if there is more than one row returned."
  [query & args]
  (let [res (apply q1 query args)]
    (if (> (count res) 1)
      (throw (Error. (str "Multiple results where at most one expected: " query " " res)))
      (first res))))

(defn transact
  [txn]
  (d/transact *connection* {:tx-data txn}))

(defn get-entity
  [id]
  (first
    (q1 '[:find (pull ?id [*])
          :in $ ?id]
        id)))

(defn wrap-datomic
  "A Ring middleware that provides a request-consistent database connection and
  value for the life of a request."
  [handler]
  (fn [request]
    (let [connection (conn)]
      (binding [*connection* connection
                *db* (d/db connection)]
        (handler request)))))

;;; TODO the doall-safe is to try to make sure lazy lists are realized within the scope of the db binding
;;; but it doesn't really work because inner elements might be lazy. Really needs to do a walk of the structure.
(defn wrap-datomic-fn
  [f]
  ((wrap-datomic (fn [& _] (u/doall-safe (f)))) nil))

(defn update->txn
  "`entity` is an entity map, update is an updated version of it (can be incomplete). Generates a txn. Not recursive (but maybe should be)."
  [entity update]
  (for [[key val] update
        :when (not (= (get entity key) val))]
    [:db/add (:db/id entity) key val]))