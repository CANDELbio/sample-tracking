(ns org.parkerici.sample-tracking.components.type.filter.utils)

(defn selected-type-db-keys
  [selected-type-key]
  [:type-filter/type-filter :type-filter/selected-options (keyword "selected-options" selected-type-key)])

(defn type-db-keys
  [type-key]
  [:type-filter/type-filter :type-filter/options (keyword "options" type-key)])

(defn selected-form-type-fields
  [db]
  (get-in db [:type-filter/type-filter :type-filter/options :options/form-type-fields]))