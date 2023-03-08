(ns org.parkerici.sample-tracking.cli
  (:gen-class)
  (:require [org.parkerici.sample-tracking.db.schema :as schema]
            [org.parkerici.sample-tracking.api.iam :as auth]
            [org.parkerici.sample-tracking.db.datomic :as d]
            [org.parkerici.sample-tracking.server :as server]
            [org.parkerici.sample-tracking.api.kit-type :as kit-type]
            [org.parkerici.sample-tracking.api.site :as site]
            [org.parkerici.sample-tracking.api.study :as study]
            [org.parkerici.sample-tracking.api.form-type :as form-type]
            [org.parkerici.sample-tracking.api.migrate :as migrate]
            [org.parkerici.sample-tracking.configuration :as c]
            [clojure.java.io :as io]
            [taoensso.timbre :as log]
            [clojure.tools.cli :as cli]
            [clojure.string :as str]
            [org.parkerici.sample-tracking.api.iam :as iam]))

(def default-port 1777)

(defmulti command
          (fn [command _arguments _options _summary] command))

(defmethod command "predeploy"
  [_ _ _ _]
  (log/info "Transacting schema.")
  (schema/transact-schema)
  (log/info "Running pending migrations.")
  (d/wrap-datomic-fn migrate/run-pending-migrations)
  (log/info "Initializing roles.")
  (d/wrap-datomic-fn #(doall (map auth/find-or-create-role (c/application-role-values)))))

(defmethod command "test-setup"
  [_ _ _ _]
  (schema/transact-schema)
  (d/wrap-datomic-fn #(doall (map auth/find-or-create-role (c/application-role-values))))
  (d/wrap-datomic-fn #(iam/find-or-create-user "test@example.com"))
  (d/wrap-datomic-fn #(kit-type/parse-kit-type-csv-and-save-to-db (io/resource "forms/kit_types.csv")))
  (d/wrap-datomic-fn #(site/parse-site-csv-and-save-to-db (io/resource "forms/sites.csv")))
  (d/wrap-datomic-fn #(study/parse-study-csv-and-save-to-db (io/resource "forms/studies.csv")))
  (d/wrap-datomic-fn #(form-type/parse-form-type-csv-and-save-to-db (io/resource "forms/form_types.csv"))))

(defmethod command "transact-schema"
  [_ _ _ _]
  (log/info "Transacting schema.")
  (schema/transact-schema))

(defmethod command "create-roles"
  [_ _ _ _]
  (d/wrap-datomic-fn #(doall (map auth/find-or-create-role (c/application-role-values)))))

(defmethod command "add-admin"
  [_ arguments _ _]
  (let [user (first arguments)]
    (d/wrap-datomic-fn #(auth/add-role-to-user user (c/application-admin-role)))))

(defmethod command "server"
  [_ _ options _]
  (let [port (if (:port options) (Integer. (:port options)) default-port)]
    (server/start port)))

(defmethod command "run-pending-migrations"
  [_ _ _ _]
  (log/info "Running pending migrations")
  (d/wrap-datomic-fn migrate/run-pending-migrations))

(defn all-commands []
  (sort (keys (dissoc (methods command) :default))))

(defn usage
  [options-summary]
  (->> [""
        "Usage: java -jar sample-tracking.jar [ACTION] [OPTIONS]..."
        ""
        "Actions:"
        (print-str (all-commands))
        ""
        "Options:"
        options-summary]
       (str/join \newline)))

(defmethod command "help"
  [_ _ summary]
  (println (usage summary)))

(defmethod command :default
  [command _ summary]
  (log/error "Unknown command:" command)
  (println (usage summary)))

(def cli-options
  ;; An option with a required argument
  [["-p" "--port PORT" "Port for the server to start on"]])

(defn -main
  [& args]
  (let [{:keys [options arguments summary]} (cli/parse-opts args cli-options)]
    (log/info "Running with environment" (c/environment))
    (command (first arguments) (rest arguments) options summary)))