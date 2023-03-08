(defproject sample-tracking "0.2.1"
  :dependencies [[aero "1.1.6"]
                 [bidi "2.1.6"]
                 [bk/ring-gzip "0.3.0"]
                 [buddy/buddy-auth "3.0.1"]
                 [clj-commons/pushy "0.3.10"]
                 [clj-htmltopdf "0.2"]
                 [cheshire "5.10.1"]
                 [clojure.java-time "0.3.3"]
                 [cljs-ajax "0.8.4"]
                 [madvas/cemerick-url "0.1.2"]
                 [cljsjs/firebase "7.5.0-0"]
                 [com.datomic/client-pro "0.9.63"
                  :exclusions [org.eclipse.jetty/jetty-client
                               org.eclipse.jetty/jetty-http
                               org.eclipse.jetty/jetty-util]]
                 [com.google.firebase/firebase-admin "8.1.0"]
                 [com.google.guava/guava "31.0.1-jre"]
                 [com.sendgrid/sendgrid-java "2.2.2"]
                 [com.taoensso/timbre "5.1.2"]
                 [commons-codec/commons-codec "1.15"]
                 [compojure "1.6.2"]
                 [day8.re-frame/http-fx "0.2.4"]
                 [hiccup "1.0.5"]
                 [http-kit "2.5.3"]
                 [listora/again "1.0.0"]
                 [ns-tracker "0.4.0"]
                 [org.apache.httpcomponents/httpclient "4.5.13"]
                 [org.clojure/clojure "1.10.3"]
                 [org.clojure/clojurescript "1.11.4"]
                 [org.clojure/data.csv "1.0.0"]
                 [org.clojure/tools.cli "1.0.206"]
                 [org.eclipse.jetty/jetty-server "9.4.12.v20180830"]
                 [org.eclipse.jetty/jetty-client "9.4.12.v20180830"]
                 [org.eclipse.jetty/jetty-http "9.4.12.v20180830"]
                 [org.eclipse.jetty/jetty-util "9.4.12.v20180830"]
                 [org.eclipse.jetty.websocket/websocket-servlet "9.4.12.v20180830"]
                 [org.eclipse.jetty.websocket/websocket-server "9.4.12.v20180830"]
                 [org.parkerici/alzabo "0.2.7"]
                 [org.parkerici/multitool "0.0.18"]
                 [org.slf4j/slf4j-simple "1.7.32"]          ;required to turn off warning
                 [reagent "0.10.0"]
                 [re-frame "1.2.0"]
                 [ring "1.8.0"]
                 [ring/ring-defaults "0.3.3"]
                 [ring/ring-jetty-adapter "1.7.1"]
                 [ring-logger "1.0.1"]
                 [ring-middleware-format "0.7.4"]
                 [trptcolin/versioneer "0.2.0"]]

  :repositories [["github" {:url           "https://maven.pkg.github.com/ParkerICI/mvn-packages"
                            :sign-releases false
                            :username      :env/github_user
                            :password      :env/github_password}]]

  :ring {:handler org.parkerici.sample-tracking.handler/app}

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-ring "0.12.6"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/cljs" "src/cljc" "src/clj"]
  :test-paths ["test/clj"]

  :target-dir "target"

  :resource-paths ["resources" "test/resources"]

  :clean-targets ^{:protect false} ["target" "resources/public/cljs-out"]

  :aliases {"cli"           ["with-profile" "cli" "run"]
            "fig"           ["trampoline" "run" "-m" "figwheel.main"]
            "fig:build"     ["trampoline" "run" "-m" "figwheel.main" "-b" "dev" "-r"]
            "fig:build-min" ["trampoline" "run" "-m" "figwheel.main" "-O" "advanced" "-bo" "prod" "-s"]
            "fig:min"       ["run" "-m" "figwheel.main" "-O" "advanced" "-bo" "prod"]
            "fig:test"      ["run" "-m" "figwheel.main" "-co" "test.cljs.edn" "-m" "hello-figwheel-main.test-runner"]
            "package"       ["do" "clean" ["fig:min"] ["uberjar"]]}

  :profiles
  {:cli
   {:main         org.parkerici.sample-tracking.cli
    :source-paths ["src/clj"]}

   :test
   {:dependencies [[ring/ring-mock "0.4.0"]]
    :env          {:datomic-endpoint     "localhost:9119"
                   :datomic-db-name      "ereq-test"
                   :send-manifest-emails "false"}}

   :dev
   {:source-paths     ["src/clj" "src/cljc" "src/cljs" "dev"]
    :nrepl-middleware ["cider.nrepl/cider-middleware"
                       "refactor-nrepl.middleware/wrap-refactor"
                       "cider.piggieback/wrap-cljs-repl"]
    :dependencies     [[binaryage/devtools "1.0.4"]
                       [cider/piggieback "0.5.3"]
                       [com.bhauman/figwheel-main "0.2.15"
                        :exclusions [org.eclipse.jetty.websocket/websocket-servlet
                                     org.eclipse.jetty.websocket/websocket-server]]
                       [com.bhauman/rebel-readline-cljs "0.1.4"]
                       [day8.re-frame/tracing "0.6.2"]
                       [day8.re-frame/re-frame-10x "1.2.1"]]}


   :uberjar
   {:dependencies   [[com.bhauman/figwheel-main "0.2.15"
                      :exclusions [org.eclipse.jetty.websocket/websocket-servlet
                                   org.eclipse.jetty.websocket/websocket-server]]
                     [com.bhauman/rebel-readline-cljs "0.1.4"]]
    :omit-source    true
    :cljs-devtools  false
    :jar-name       "sample-tracking.jar"
    :uberjar-name   "sample-tracking-standalone.jar"
    :clean-targets  ^:replace ["target"]
    :resource-paths ^:replace ["resources"]
    :main           ^:skip-aot org.parkerici.sample-tracking.cli
    :aot            :all}}

  :main org.parkerici.sample-tracking.cli
  :aot [org.parkerici.sample-tracking.cli])
