(ns org.parkerici.sample-tracking.utils.path
  (:require [clojure.string :as str]))

(defn split
  "Splits a path delimited with /"
  [path]
  (remove empty? (str/split path #"/")))

(defn join
  "Joins the elements passed in into a path"
  [& args]
  (str "/" (str/join "/" (flatten (map #(split %) (remove empty? args))))))