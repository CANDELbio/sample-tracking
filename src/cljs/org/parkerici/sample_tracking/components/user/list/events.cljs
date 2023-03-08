(ns org.parkerici.sample-tracking.components.user.list.events
  (:require [re-frame.core :refer [reg-event-fx reg-event-db]]
            [clojure.set :as set]
            [org.parkerici.sample-tracking.events :as events]
            [ajax.core :as ajax]))

(reg-event-fx
  :user-list/initialize
  events/default-interceptors
  (fn [{:keys [db]} []]
    {:db         (dissoc db :user-list/user-list)
     :dispatch-n [[:user-list/fetch-values "/api/role" [:user-list/roles]]
                  [:user-list/fetch-values "/api/user" [:user-list/users]]]}))

(reg-event-fx
  :user-list/fetch-values
  events/default-interceptors
  (fn [{:keys [_db]} [endpoint db-key]]
    {:http-xhrio {:method          :get
                  :uri             endpoint
                  :timeout         15000
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:fetch-success :user-list/user-list db-key]
                  :on-failure      [:flash/request-error "Error! Server said: "]}}))

(reg-event-db
  :user-list/set-new-user-email
  (fn [db [_ email]]
    (assoc-in db [:user-list/user-list :user-list/new-user-email] email)))

(defn user-xhrio
  [email change]
  {:method          (case change
                      :create :post
                      :delete :delete)
   :uri             "/api/user"
   :params          {:email email}
   :timeout         15000
   :format          (ajax/json-request-format)
   :response-format (ajax/json-response-format {:keywords? true})
   :on-success      (case change
                      :create [:user-list/create-new-user-success]
                      :delete [:user-list/delete-user-success email])
   :on-failure      [:flash/request-error "Error! Server said: "]})

(reg-event-fx
  :user-list/create-new-user
  events/default-interceptors
  (fn [{:keys [db]} []]
    {:http-xhrio (user-xhrio (get-in db [:user-list/user-list :user-list/new-user-email]) :create)}))

(reg-event-db
  :user-list/create-new-user-success
  (fn [db [_ response]]
    (let [uuid (get-in response [:data :uuid])
          new-email (get-in db [:user-list/user-list :user-list/new-user-email])
          current-users (get-in db [:user-list/user-list :user-list/users])
          updated-users (conj current-users {:uuid uuid :email new-email})]
      (-> db
          (assoc-in [:user-list/user-list :user-list/users] updated-users)
          (update-in [:user-list/user-list] dissoc :user-list/new-user-email)))))

(reg-event-fx
  :user-list/delete-user
  events/default-interceptors
  (fn [{:keys [_db]} [email]]
    {:http-xhrio (user-xhrio email :delete)}))

(reg-event-db
  :user-list/delete-user-success
  (fn [db [_ email _response]]
    (let [current-users (get-in db [:user-list/user-list :user-list/users])
          updated-users (filterv #(not= (:email %) email) current-users)]
      (-> db
          (assoc-in [:user-list/user-list :user-list/users] updated-users)))))

(defn update-role-xhrio
  [email role change]
  {:method          (case change
                      :remove :delete
                      :add :post)
   :uri             "/api/user/role"
   :params          {:email email :role-name role}
   :timeout         15000
   :format          (ajax/json-request-format)
   :response-format (ajax/json-response-format {:keywords? true})
   :on-success      (case change
                      :remove [:user-list/remove-role-success email role]
                      :add [:user-list/add-role-success email role])
   :on-failure      [:flash/request-error "Error! Server said: "]})

(reg-event-fx
  :user-list/update-roles
  events/default-interceptors
  (fn [{:keys [db]} [email updated-roles]]
    (let [users (get-in db [:user-list/user-list :user-list/users])
          current-user-role-names (map :name (:roles (first (filter #(= (:email %) email) users))))
          current-roles-set (set current-user-role-names)
          updated-roles-set (set updated-roles)
          removed-roles (seq (set/difference current-roles-set updated-roles-set))
          added-roles (seq (set/difference updated-roles-set current-roles-set))]
      (if removed-roles
        {:http-xhrio (update-role-xhrio email (first removed-roles) :remove)}
        {:http-xhrio (update-role-xhrio email (first added-roles) :add)}))))

(reg-event-db
  :user-list/remove-role-success
  (fn [db [_ email role _response]]
    (let [current-users (get-in db [:user-list/user-list :user-list/users])
          updated-users (mapv (fn [user] (if (= (:email user) email)
                                           (assoc user :roles (filterv #(not= (:name %) role) (:roles user)))
                                           user)) current-users)]
      (-> db
          (assoc-in [:user-list/user-list :user-list/users] updated-users)))))

(reg-event-db
  :user-list/add-role-success
  (fn [db [_ email role _response]]
    (let [current-users (get-in db [:user-list/user-list :user-list/users])
          updated-users (mapv (fn [user] (if (= (:email user) email)
                                           (assoc user :roles (conj (:roles user) {:name role}))
                                           user)) current-users)]
      (-> db
          (assoc-in [:user-list/user-list :user-list/users] updated-users)))))