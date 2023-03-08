(ns org.parkerici.sample-tracking.api.list-test
  (:require [clojure.test :refer :all]
            [org.parkerici.sample-tracking.test-utils :as tu]))

(defn list-studies-test
  [study-name]
  (let [studies-list (tu/get-studies-list)
        pici0014-study (tu/get-element-with-name studies-list study-name)]
    (is (= 4 (count studies-list)))
    (is (some? pici0014-study))
    pici0014-study))

(defn list-sites-test
  [study site-name]
  (let [sites-list (tu/get-sites-list (:uuid study))
        mskcc-site (tu/get-element-with-name sites-list site-name)]
    (is (= 4 (count sites-list)))
    (is (some? mskcc-site))
    mskcc-site))

(defn list-cohorts-test
  [study cohort-name]
  (let [cohorts-list (tu/get-cohorts-list (:uuid study))
        na-cohort (tu/get-element-with-name cohorts-list cohort-name)]
    (is (= 1 (count cohorts-list)))
    (is (some? na-cohort))
    na-cohort))

(defn list-kit-types-test
  [cohort kit-type-name]
  (let [kit-types-list (tu/get-kit-types-list (:uuid cohort))
        blood-serum-kit (tu/get-element-with-name kit-types-list kit-type-name)]
    (is (= 6 (count kit-types-list)))
    (is (some? blood-serum-kit))
    blood-serum-kit))

(defn list-timepoints-test
  [kit-type]
  (let [timepoints-list (tu/get-timepoint-list (:uuid kit-type))]
    (is (= 7 (count timepoints-list)))))

(defn list-sample-types-test
  [kit-type]
  (let [sample-types-list (tu/get-sample-types-list (:uuid kit-type))]
    (is (= 14 (count sample-types-list)))))

(defn list-form-types-test
  [kit-type]
  (let [form-types-list (tu/get-form-types-list (:uuid kit-type))]
    (is (= 1 (count form-types-list)))))

(deftest list-tests
  (let [study (list-studies-test "STUDY13")
        cohort (list-cohorts-test study "N/A")
        kit-type (list-kit-types-test cohort "STUDY13 Blood-Serum Manifest Form")]
    (list-sites-test study "CCCC")
    (list-timepoints-test kit-type)
    (list-sample-types-test kit-type)
    (list-form-types-test kit-type)))