(ns org.parkerici.sample-tracking.api.propose-edits-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as rm]
            [org.parkerici.sample-tracking.test-utils :as tu]
            [clojure.edn :as edn]))

(deftest propose-edit-kit-test
  []
  (let [kit-request-body (tu/kit-request-body)
        create-response (tu/standard-web-app (-> (rm/request :post "/api/kit")
                                                 (rm/json-body kit-request-body)))
        created-uuid (tu/get-response-uuid create-response)
        initial-pending-proposed-edits (tu/get-proposed-kit-edit-list {:status "pending"})
        propose-edit-kit-request-body (-> kit-request-body
                                          (assoc :uuid created-uuid)
                                          (assoc :kit-id "87654321"))
        _propose-edit-response (tu/site-coordinator-authed-web-app (-> (rm/request :post "/api/kit/propose-edit")
                                                                       (rm/json-body propose-edit-kit-request-body)))
        pending-proposed-edits (tu/get-proposed-kit-edit-list {:status "pending"})]
    (is (= 1 (- (count pending-proposed-edits) (count initial-pending-proposed-edits))))))

(deftest approve-propose-edit-kit-test
  []
  (let [kit-request-body (tu/kit-request-body)
        create-response (tu/standard-web-app (-> (rm/request :post "/api/kit")
                                                 (rm/json-body kit-request-body)))
        created-uuid (tu/get-response-uuid create-response)
        propose-edit-kit-request-body (-> kit-request-body
                                          (assoc :uuid created-uuid)
                                          (assoc :kit-id "87654321"))
        propose-edit-response (tu/site-coordinator-authed-web-app (-> (rm/request :post "/api/kit/propose-edit")
                                                                      (rm/json-body propose-edit-kit-request-body)))
        propose-edit-uuid (tu/get-response-uuid propose-edit-response)
        initial-approved-proposed-edits (tu/get-proposed-kit-edit-list {:status "approved"})
        _set-edit-status-response (tu/admin-authed-web-app (rm/request :post "/api/kit/propose-edit/set-status"
                                                                       {:uuid propose-edit-uuid :status "approved"}))
        approved-proposed-edits (tu/get-proposed-kit-edit-list {:status "approved"})
        edited-kit (first (tu/get-kit-list {:uuid created-uuid}))
        edited-kit-history (tu/get-entity-history created-uuid)]
    (is (= 1 (- (count approved-proposed-edits) (count initial-approved-proposed-edits))))
    (is (= (:kit-id propose-edit-kit-request-body) (:kit-id edited-kit)))
    (is (= 1 (count edited-kit-history)))))

(deftest deny-propose-edit-kit-test
  []
  (let [kit-request-body (tu/kit-request-body)
        create-response (tu/standard-web-app (-> (rm/request :post "/api/kit")
                                                 (rm/json-body kit-request-body)))
        created-uuid (tu/get-response-uuid create-response)
        propose-edit-kit-request-body (-> kit-request-body
                                          (assoc :uuid created-uuid)
                                          (assoc :kit-id "87654321"))
        propose-edit-response (tu/site-coordinator-authed-web-app (-> (rm/request :post "/api/kit/propose-edit")
                                                                      (rm/json-body propose-edit-kit-request-body)))
        propose-edit-uuid (tu/get-response-uuid propose-edit-response)
        initial-approved-proposed-edits (tu/get-proposed-kit-edit-list {:status "denied"})
        _set-edit-status-response (tu/admin-authed-web-app (rm/request :post "/api/kit/propose-edit/set-status"
                                                                       {:uuid propose-edit-uuid :status "denied"}))
        denied-proposed-edits (tu/get-proposed-kit-edit-list {:status "denied"})
        edited-kit (first (tu/get-kit-list {:uuid created-uuid}))
        edited-kit-history (tu/get-entity-history created-uuid)]
    (is (= 1 (- (count denied-proposed-edits) (count initial-approved-proposed-edits))))
    (is (= (:kit-id kit-request-body) (:kit-id edited-kit)))
    (is (= 0 (count edited-kit-history)))))

(deftest edit-proposed-edit-test
  []
  (let [kit-request-body (tu/kit-request-body)
        create-response (tu/standard-web-app (-> (rm/request :post "/api/kit")
                                                 (rm/json-body kit-request-body)))
        created-kit-uuid (tu/get-response-uuid create-response)
        initial-pending-proposed-edits (tu/get-proposed-kit-edit-list {:status "pending"})
        first-propose-edit-kit-request-body (-> kit-request-body
                                                (assoc :uuid created-kit-uuid)
                                                (assoc :kit-id "87654321"))
        first-propose-edit-response (tu/site-coordinator-authed-web-app (-> (rm/request :post "/api/kit/propose-edit")
                                                                            (rm/json-body first-propose-edit-kit-request-body)))
        first-proposed-edit-uuid (tu/get-response-uuid first-propose-edit-response)
        first-pending-proposed-edits (tu/get-proposed-kit-edit-list {:status "pending"})
        final-kit-id "12344321"
        final-propose-edit-kit-request-body (-> kit-request-body
                                                (assoc :uuid created-kit-uuid)
                                                (assoc :kit-id final-kit-id))
        final-propose-edit-response (tu/site-coordinator-authed-web-app (-> (rm/request :post "/api/kit/propose-edit")
                                                                            (rm/json-body final-propose-edit-kit-request-body)))
        final-proposed-edit-uuid (tu/get-response-uuid final-propose-edit-response)
        final-pending-proposed-edits (tu/get-proposed-kit-edit-list {:status "pending"})
        final-pending-edits-for-kit (filter #(= created-kit-uuid (:kit-uuid %)) final-pending-proposed-edits)
        final-pending-edit-update-map (edn/read-string (:update-map (first final-pending-edits-for-kit)))]
    (is (= 1 (- (count first-pending-proposed-edits) (count initial-pending-proposed-edits))))
    (is (= first-proposed-edit-uuid final-proposed-edit-uuid))
    (is (= 0 (- (count first-pending-proposed-edits) (count final-pending-proposed-edits))))
    (is (= 1 (count final-pending-edits-for-kit)))
    (is (= (:kit-id final-pending-edit-update-map) final-kit-id))))