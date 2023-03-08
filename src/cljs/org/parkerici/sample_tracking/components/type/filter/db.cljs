(ns org.parkerici.sample-tracking.components.type.filter.db
  (:require [cljs.spec.alpha :as s]))

(s/def :options/studies vector?)
(s/def :options/sites vector?)
(s/def :options/cohorts vector?)
(s/def :options/timepoints vector?)
(s/def :options/kit-types vector?)
(s/def :options/sample-types vector?)
(s/def :options/form-type-fields vector?)

(s/def :selected-options/study string?)
(s/def :selected-options/site string?)
(s/def :selected-options/cohort string?)
(s/def :selected-options/timepoints (s/* string?))
(s/def :selected-options/kit-type string?)

(s/def :type-filter/options (s/keys :opt [:options/sites
                                          :options/studies
                                          :options/cohorts
                                          :options/timepoints
                                          :options/kit-types
                                          :options/form-type-fields]))

(s/def :type-filter/selected-options (s/keys :opt [:selected-options/site
                                                   :selected-options/study
                                                   :selected-options/cohort
                                                   :selected-options/timepoints
                                                   :selected-options/kit-type]))

(s/def :type-filter/type-filter (s/keys :opt [:type-filter/options
                                              :type-filter/selected-options
                                              :type-filter/form-types]))
