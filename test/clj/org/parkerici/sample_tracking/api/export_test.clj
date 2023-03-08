(ns org.parkerici.sample-tracking.api.export-test
  (:require [clojure.test :refer :all]
            [clojure.data.csv :as csv]
            [ring.mock.request :as rm]
            [org.parkerici.sample-tracking.test-utils :as tu]))

(deftest export-collected-samples-test
  []
  (let [kit-request-body (tu/kit-request-body)
        create-response (tu/standard-web-app (-> (rm/request :post "/api/kit")
                                                 (rm/json-body kit-request-body)))
        created-uuid (tu/get-response-uuid create-response)
        export-response (tu/admin-authed-web-app (rm/request :get "/api/sample/export" {:uuid created-uuid}))
        export-csv (csv/read-csv (slurp (:body export-response)))]
    (is (= 4 (count export-csv)))
    (is (= 23 (count (first export-csv))))))

(deftest export-all-samples-test
  []
  (let [kit-request-body (tu/kit-request-body)
        create-response (tu/standard-web-app (-> (rm/request :post "/api/kit")
                                                 (rm/json-body kit-request-body)))
        created-uuid (tu/get-response-uuid create-response)
        export-response (tu/admin-authed-web-app
                          (rm/request :get "/api/sample/export" {:uuid created-uuid :uncollected true}))
        export-csv (csv/read-csv (slurp (:body export-response)))]
    (is (= 15 (count export-csv)))
    (is (= 23 (count (first export-csv))))))
