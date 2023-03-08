(ns org.parkerici.sample-tracking.api.set-active-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as rm]
            [org.parkerici.sample-tracking.test-utils :as tu]))

(defn toggle-study-active-test
  [study-name]
  (let [studies-list (tu/get-studies-list)
        toggling-study (tu/get-element-with-name studies-list study-name)
        _ (tu/admin-authed-web-app (-> (rm/request :post "/api/set-active")
                                       (rm/json-body {:study (:uuid toggling-study) :active "false"})))
        post-toggle-studies-list (tu/get-studies-list)
        inactive-study (tu/get-element-with-name post-toggle-studies-list study-name)
        _ (tu/admin-authed-web-app (-> (rm/request :post "/api/set-active")
                                       (rm/json-body {:study (:uuid toggling-study) :active "true"})))
        reverted-studies-list (tu/get-studies-list)
        reactivated-study (tu/get-element-with-name reverted-studies-list study-name)]
    (is (= 4 (count studies-list)))
    (is (= 3 (count post-toggle-studies-list)))
    (is (= 4 (count reverted-studies-list)))
    (is (some? toggling-study))
    (is (nil? inactive-study))
    (is (some? reactivated-study))
    toggling-study))

(defn toggle-site-active-test
  [study site-name]
  (let [sites-list (tu/get-sites-list (:uuid study))
        toggling-site (tu/get-element-with-name sites-list site-name)
        _ (tu/admin-authed-web-app (-> (rm/request :post "/api/set-active")
                                       (rm/json-body {:study (:uuid study) :site (:uuid toggling-site) :active "false"})))
        post-toggle-sites-list (tu/get-sites-list (:uuid study))
        inactive-site (tu/get-element-with-name post-toggle-sites-list site-name)
        _ (tu/admin-authed-web-app (-> (rm/request :post "/api/set-active")
                                       (rm/json-body {:study (:uuid study) :site (:uuid toggling-site) :active "true"})))
        reverted-sites-list (tu/get-sites-list (:uuid study))
        reverted-site (tu/get-element-with-name reverted-sites-list site-name)]
    (is (= 4 (count sites-list)))
    (is (= 3 (count post-toggle-sites-list)))
    (is (= 4 (count reverted-sites-list)))
    (is (some? toggling-site))
    (is (nil? inactive-site))
    (is (some? reverted-site))))

(defn toggle-cohort-active-test
  [study cohort-name]
  (let [cohorts-list (tu/get-cohorts-list (:uuid study))
        toggling-cohort (tu/get-element-with-name cohorts-list cohort-name)
        _ (tu/admin-authed-web-app (-> (rm/request :post "/api/set-active")
                                       (rm/json-body {:study (:uuid study) :cohort (:uuid toggling-cohort) :active "false"})))
        post-toggle-cohorts-list (tu/get-cohorts-list (:uuid study))
        inactive-cohort (tu/get-element-with-name post-toggle-cohorts-list cohort-name)
        _ (tu/admin-authed-web-app (-> (rm/request :post "/api/set-active")
                                       (rm/json-body {:study (:uuid study) :cohort (:uuid toggling-cohort) :active "true"})))
        reverted-cohorts-list (tu/get-cohorts-list (:uuid study))
        reverted-cohort (tu/get-element-with-name reverted-cohorts-list cohort-name)]
    (is (= 1 (count cohorts-list)))
    (is (= 0 (count post-toggle-cohorts-list)))
    (is (= 1 (count reverted-cohorts-list)))
    (is (some? toggling-cohort))
    (is (nil? inactive-cohort))
    (is (some? reverted-cohort))
    toggling-cohort))

(defn toggle-kit-type-active-test
  [cohort kit-type-name]
  (let [kit-types-list (tu/get-kit-types-list (:uuid cohort))
        toggling-kit-type (tu/get-element-with-name kit-types-list kit-type-name)
        _ (tu/admin-authed-web-app (-> (rm/request :post "/api/set-active")
                                       (rm/json-body {:kit-type (:uuid toggling-kit-type) :cohort (:uuid cohort) :active "false"})))
        post-toggle-kit-types-list (tu/get-kit-types-list (:uuid cohort))
        inactive-kit-type (tu/get-element-with-name post-toggle-kit-types-list kit-type-name)
        _ (tu/admin-authed-web-app (-> (rm/request :post "/api/set-active")
                                       (rm/json-body {:kit-type (:uuid toggling-kit-type) :cohort (:uuid cohort) :active "true"})))
        reverted-kit-types-list (tu/get-kit-types-list (:uuid cohort))
        reverted-kit-type (tu/get-element-with-name reverted-kit-types-list kit-type-name)]
    (is (= 6 (count kit-types-list)))
    (is (= 5 (count post-toggle-kit-types-list)))
    (is (= 6 (count reverted-kit-types-list)))
    (is (some? toggling-kit-type))
    (is (nil? inactive-kit-type))
    (is (some? reverted-kit-type))))

(deftest set-active-tests
  (let [study (toggle-study-active-test "STUDY13")
        cohort (toggle-cohort-active-test study "N/A")]
    (toggle-site-active-test study "CCCC")
    (toggle-kit-type-active-test cohort "STUDY13 Blood-Serum Manifest Form")))