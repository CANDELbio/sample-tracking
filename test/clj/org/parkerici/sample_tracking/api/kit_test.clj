(ns org.parkerici.sample-tracking.api.kit-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as rm]
            [org.parkerici.sample-tracking.test-utils :as tu]))

(deftest create-kit-test
  []
  (let [initial-kit-list (tu/get-kit-list {:complete "true"})
        kit-request-body (tu/kit-request-body)
        create-response (tu/standard-web-app (-> (rm/request :post "/api/kit")
                                                 (rm/json-body kit-request-body)))
        created-uuid (tu/get-response-uuid create-response)
        post-create-kit-list (tu/get-kit-list {:complete "true"})
        created-kit (first (filter #(= (:uuid %) created-uuid) post-create-kit-list))]
    (is (some? created-uuid))
    (is (some? created-kit))
    (is (= 1 (- (count post-create-kit-list) (count initial-kit-list))))))

(deftest share-kit-test
  []
  (let [initial-incomplete-kit-list (tu/get-kit-list {:complete "false"})
        initial-complete-kit-list (tu/get-kit-list {:complete "true"})
        complete-kit-request-body (tu/kit-request-body)
        share-kit-request-body (dissoc complete-kit-request-body :samples :completing-email)
        share-response (tu/standard-web-app (-> (rm/request :post "/api/kit/share")
                                                (rm/json-body share-kit-request-body)))
        shared-uuid (tu/get-response-uuid share-response)
        created-kit (first (tu/get-response-items (tu/standard-web-app (rm/request :get "/api/kit/share" {:uuid shared-uuid}))))
        post-share-incomplete-kit-list (tu/get-kit-list {:complete "false"})
        shared-kit-from-list (first (filter #(= (:uuid %) shared-uuid) post-share-incomplete-kit-list))
        submit-response (tu/standard-web-app (-> (rm/request :post "/api/kit")
                                                 (rm/json-body (assoc complete-kit-request-body :uuid shared-uuid))))
        submitted-uuid (tu/get-response-uuid submit-response)
        submitted-kit-from-share (first (tu/get-response-items (tu/standard-web-app (rm/request :get "/api/kit/share" {:uuid shared-uuid}))))
        post-submitted-incomplete-kit-list (tu/get-kit-list {:complete "false"})
        post-submitted-complete-kit-list (tu/get-kit-list {:complete "true"})
        incomplete-submitted-kit (first (filter #(= (:uuid %) shared-uuid) post-submitted-incomplete-kit-list))
        complete-submitted-kit (first (filter #(= (:uuid %) shared-uuid) post-submitted-complete-kit-list))]
    (is (some? shared-uuid))
    (is (some? submitted-uuid))
    (= shared-uuid submitted-uuid)                          ; The UUID returned from the share endpoint should stay the same after submitting
    (is (some? created-kit))
    (is (some? shared-kit-from-list))
    (is (nil? submitted-kit-from-share))                    ; Users should not be able to access a submitted kit via share
    (is (nil? incomplete-submitted-kit))                    ; Submitted kit should not be available in incomplete list
    (is (some? complete-submitted-kit))
    (is (= 1 (- (count post-share-incomplete-kit-list) (count initial-incomplete-kit-list))))
    (is (= 1 (- (count post-submitted-complete-kit-list) (count initial-complete-kit-list))))))

(deftest edit-kit-test
  []
  (let [kit-request-body (tu/kit-request-body)
        create-response (tu/standard-web-app (-> (rm/request :post "/api/kit")
                                                 (rm/json-body kit-request-body)))
        created-uuid (tu/get-response-uuid create-response)
        edit-kit-request-body (-> kit-request-body
                                  (assoc :uuid created-uuid)
                                  (assoc :kit-id "87654321"))
        edit-response (tu/admin-authed-web-app (-> (rm/request :patch "/api/kit")
                                                   (rm/json-body edit-kit-request-body)))
        edited-uuid (tu/get-response-uuid edit-response)
        edited-kit (first (tu/get-kit-list {:uuid edited-uuid}))
        edited-kit-history (tu/get-entity-history edited-uuid)]
    (is (some? created-uuid))
    (is (some? edited-uuid))
    (is (= created-uuid edited-uuid))                       ; The UUID returned from the edit endpoint should stay the same after submitting
    (is (= (:kit-id edit-kit-request-body) (:kit-id edited-kit)))
    (is (= 1 (count edited-kit-history)))))

(deftest archive-kit-test
  (let [kit-request-body (tu/kit-request-body)
        create-response (tu/standard-web-app (-> (rm/request :post "/api/kit")
                                                 (rm/json-body kit-request-body)))
        created-uuid (tu/get-response-uuid create-response)
        pre-archive-archived-kit-list (tu/get-kit-list {:archived "true"})
        _ (tu/admin-authed-web-app (rm/request :patch "/api/kit/set-archived" {:uuid created-uuid :archived "true"}))
        post-archive-archived-kit-list (tu/get-kit-list {:archived "true"})
        archived-kit (first (filter #(= (:uuid %) created-uuid) post-archive-archived-kit-list))
        _ (tu/admin-authed-web-app (rm/request :patch "/api/kit/set-archived" {:uuid created-uuid :archived "false"}))
        post-unarchive-archived-kit-list (tu/get-kit-list {:archived "true"})
        missing-archived-kit (first (filter #(= (:uuid %) created-uuid) post-unarchive-archived-kit-list))
        post-unarchive-kit-list (tu/get-kit-list {})
        unarchived-kit (first (filter #(= (:uuid %) created-uuid) post-unarchive-kit-list))
        kit-history (tu/get-entity-history created-uuid)]
    (is (some? created-uuid))
    (is (= 1 (- (count post-archive-archived-kit-list) (count pre-archive-archived-kit-list))))
    (is (= 1 (- (count post-archive-archived-kit-list) (count post-unarchive-archived-kit-list))))
    (is (some? archived-kit))
    (is (nil? missing-archived-kit))
    (is (some? unarchived-kit))
    (is (= 2 (count kit-history)))))