(ns org.parkerici.sample-tracking.utils.ring
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [ring.util.io :refer [piped-input-stream]]))

(defn json-response
  [data & {:keys [status] :or {status 200}}]
  {:status  status
   :headers {"Content-Type" "application/json"}
   :body    (json/generate-string data)})

(defn csv-response
  [streaming-csv filename]
  (let [disposition     (str "attachment; filename=\"" filename "\"")]
    {:headers {"Content-Type" "text/csv"
               "Content-Disposition" disposition}
     :body    (piped-input-stream #(streaming-csv (io/make-writer % {})))}))
