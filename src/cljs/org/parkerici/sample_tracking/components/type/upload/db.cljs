(ns org.parkerici.sample-tracking.components.type.upload.db
  (:require [cljs.spec.alpha :as s]))

(s/def :type-upload/uploading boolean?)

(s/def :type-upload/type-upload (s/keys :opt [:type-upload/uploading]))