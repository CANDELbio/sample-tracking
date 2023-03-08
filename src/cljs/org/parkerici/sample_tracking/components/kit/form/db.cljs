(ns org.parkerici.sample-tracking.components.kit.form.db
  (:require [cljs.spec.alpha :as s]))

(s/def :form-values/uuid string?)
(s/def :form-values/kit-id string?)
(s/def :form-values/participant-id string?)
(s/def :form-values/air-waybill string?)
(s/def :form-values/collection-date inst?)
(s/def :form-values/collection-time inst?)
(s/def :form-values/completing-first-name string?)
(s/def :form-values/completing-last-name string?)
(s/def :form-values/completing-email string?)
(s/def :form-values/comments string?)
(s/def :form-values/samples map?)
(s/def :form-values/complete boolean?)
(s/def :form-values/form-type-field-values map?)
(s/def :form-values/timezone string?)
(s/def :form-values/archived boolean?)
(s/def :form-values/pending-edits vector?)
(s/def :form-values/history vector?)

(s/def :kit-form/form-values (s/keys :opt [:form-values/uuid
                                           :form-values/kit-id
                                           :form-values/participant-id
                                           :form-values/air-waybill
                                           :form-values/collection-date
                                           :form-values/collection-time
                                           :form-values/completing-first-name
                                           :form-values/completing-last-name
                                           :form-values/completing-email
                                           :form-values/comments
                                           :form-values/samples
                                           :form-values/complete
                                           :form-values/form-type-field-values
                                           :form-values/timezone
                                           :form-values/archived
                                           :form-values/pending-edits
                                           :form-values/history]))

(s/def :kit-form/kit-id-submitted boolean?)

(s/def :kit-form/kit-form (s/keys :opt [:kit-form/form-values :kit-form/kit-id-submitted]))