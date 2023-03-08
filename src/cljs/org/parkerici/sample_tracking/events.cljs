(ns org.parkerici.sample-tracking.events
  (:require [cljs.spec.alpha :as s]
            [re-frame.core :refer [reg-event-db after trim-v reg-event-fx]]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [org.parkerici.sample-tracking.config :as config]
            [org.parkerici.sample-tracking.routes :as routes]
            [org.parkerici.sample-tracking.db :as db]))

(defn check-and-throw
  "Throw an exception if db doesn't match the spec."
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (ex-info (str "spec check failed: " (s/explain-str a-spec db)) {}))))

(def check-spec-interceptor
  (after (partial check-and-throw ::db/db)))

(def default-interceptors
  (if config/debug?
    [check-spec-interceptor trim-v]
    [trim-v]))

(reg-event-fx
  :initialize-db
  (fn [_ _]
    {:db db/default-db}
    ))

(defn add-initialize-event
  [fx-map event]
  (if event
    (assoc fx-map :dispatch [event])
    fx-map))

(defmulti page-initialize-event
          (fn [page] page))

(defmethod page-initialize-event :type-list [_]
  :type-list/initialize)

(defmethod page-initialize-event :kit-shipment [_]
  :kit-form/initialize)

(defmethod page-initialize-event :shared-kit-shipment [_]
  :kit-shipment/initialize-shared-kit)

(defmethod page-initialize-event :kit-list [_]
  :kit-list/initialize)

(defmethod page-initialize-event :kit-edit [_]
  :kit-form/initialize-with-kit)

(defmethod page-initialize-event :propose-kit-edits [_]
  :propose-edits/initialize)

(defmethod page-initialize-event :proposed-kit-edit-list [_]
  :proposed-edits-list/initialize)

(defmethod page-initialize-event :proposed-kit-edit-view [_]
  :proposed-edit/initialize)

(defmethod page-initialize-event :kit-view [_]
  :kit-form/initialize-with-kit)

(defmethod page-initialize-event :history [_]
  :history/initialize)

(defmethod page-initialize-event :entity-history [_]
  :history/initialize)

(defmethod page-initialize-event :user-list [_]
  :user-list/initialize)

(defmethod page-initialize-event :config-list  [_]
  :configuration-list/initialize)

(defmethod page-initialize-event :default [_]
  nil)

(reg-event-fx
  :set-active-page
  default-interceptors
  (fn [{:keys [db]} [page route-params query-params]]
    (let [fx-map {:db (-> db
                          (assoc ::db/page page)
                          (assoc ::db/route-params route-params)
                          (assoc ::db/query-params query-params))}
          initialize-event (page-initialize-event page)]
      (add-initialize-event fx-map initialize-event))))

(reg-event-db
  :clear-user-info
  (fn [db]
    (assoc db ::db/user {})))

(reg-event-db
  :set-user-info
  (fn [db [_ response]]
    (when-let [user-info (first (get-in response [:data :items]))]
      (assoc db ::db/user user-info))))

(reg-event-fx
  :initialize-firebase
  default-interceptors
  (fn [{:keys [_db]} []]
    {:http-xhrio {:method          :get
                  :uri             "/api/firebase-credentials"
                  :timeout         8000
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:parse-firebase-response]
                  :on-failure      [:flash/request-error "Error! Server said: "]}}))

(reg-event-db
  :parse-firebase-response
  (fn [db [_ response]]
    (.initializeApp js/firebase (clj->js response))
    (.setPersistence (.auth js/firebase) (.. js/firebase -auth -Auth -Persistence -LOCAL))
    (assoc db ::db/firebase-initialized true)))

(reg-event-fx
  :initialize-user-info
  default-interceptors
  (fn [{:keys [_db]} []]
    {:http-xhrio {:method          :get
                  :uri             "/api/user/current"
                  :timeout         8000
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:set-user-info]
                  :on-failure      [:flash/request-error "Error! Server said: "]}}))

(reg-event-db
  :fetch-success
  (fn [db [_ db-namespace db-keys response]]
    (let [value (get-in response [:data :items])]
      (if (some? value)
        (assoc-in db (cons db-namespace db-keys) value)
        db))))

(reg-event-db
  :clear-keys
  (fn [db [_ db-keys]]
    (if (some? (get-in db db-keys))
      (update-in db (drop-last db-keys) dissoc (last db-keys))
      db)))

(reg-event-fx
  :redirect
  default-interceptors
  (fn [{:keys [_db]} [page-key page-params]]
    (routes/redirect (apply routes/path-for (concat [page-key] (mapcat seq page-params))))))

(reg-event-fx
  :do-nothing
  default-interceptors
  (fn [{:keys [_db]} []]
    {}))