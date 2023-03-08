(ns org.parkerici.sample-tracking.server
  (:require [ring.adapter.jetty :as jetty]
            [taoensso.timbre :as log]
            [trptcolin.versioneer.core :as version]
            [org.parkerici.sample-tracking.handler :as handler]))

(def server (atom nil))

(defn stop
  []
  (when @server
    (.stop @server)))

(defn start
  ([port] (start port handler/app))
  ([port handler]
   (log/infof "Starting sample-tracking server version %s at port %s" (version/get-version "sample-tracking" "sample-tracking") port)
   (stop)
   (reset! server (jetty/run-jetty handler {:port port :join? false}))))