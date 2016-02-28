(defproject raiseyourgame "0.1.0-SNAPSHOT"
  :description "Raise Your Game"
  :url "https://raiseyourgame.com"

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.nrepl "0.2.10"]

                 ; templating
                 [selmer "0.9.0"]
                 [markdown-clj "0.9.69"]

                 ; logging
                 [com.taoensso/timbre "4.1.1"]
                 [ring-logger-timbre "0.7.0"]
                 [com.taoensso/tower "3.0.2"]

                 ; load environment variables and profile.clj props
                 [environ "1.0.0"]

                 ; data wrangling
                 [camel-snake-kebab "0.3.2"]
                 [clj-time "0.11.0"]

                 ; HTTP webapp system
                 [compojure "1.4.0"]
                 [ring-webjars "0.1.1"]
                 [ring/ring-defaults "0.1.5"]
                 [ring-ttl-session "0.1.1"]
                 [ring "1.4.0"
                  :exclusions [ring/ring-jetty-adapter]]
                 [org.immutant/web "2.0.2"] ; instead of jetty
                 [metosin/ring-middleware-format "0.6.0"]
                 [metosin/ring-http-response "0.6.3"]
                 [clout "2.1.2"]

                 ; fancy API routes
                 [metosin/compojure-api "1.0.1"]
                 [metosin/ring-swagger-ui "2.1.4-0"]

                 ; validation
                 [bouncer "0.3.3"] ; manual validation
                 [prismatic/schema "1.0.4"] ; implicit validation
                 [org.clojure/test.check "0.9.0"] ; required for Prismatic Schema random generators

                 ; exception reporting middleware
                 [prone "0.8.2"]

                 ; authentication
                 [buddy "0.6.1"]

                 ; database
                 [org.postgresql/postgresql "9.4-1201-jdbc41"]
                 [migratus "0.8.4"]
                 [conman "0.1.7"]
                 [squirrel "0.1.1"]

                 ;; CLOJURESCRIPT
                 ; core
                 [org.clojure/clojurescript "1.7.170" :scope "provided"]
                 [org.clojure/tools.reader "0.9.2"]
                 [org.clojure/core.async "0.2.374"]
                 [reagent "0.6.0-alpha"]
                 [reagent-utils "0.1.7"]
                 [re-frame "0.7.0-alpha-2"]
                 [secretary "1.2.3"]
                 [cljs-ajax "0.5.3"]]

  :min-lein-version "2.5.3"
  :uberjar-name "raiseyourgame.jar"
  :jvm-opts ["-server"]

  :main raiseyourgame.core
  :migratus {:store :database
             :migration-dir "migrations"}
  ; migratus searches for :migration-dir in all classpath directories; since
  ; $PROJECT/resources is on the classpath, we use resources/migrations.

  :plugins [[lein-environ "1.0.0"]
            [migratus-lein "0.1.7"]
            [lein-cljsbuild "1.1.2" :exclusions [[org.clojure/clojure]]]
            [lein-sassc "0.10.4"]]
  :sassc [{:src "resources/scss/screen.scss"
           :output-to "resources/public/css/screen.css"
           :style "nested"
           :import-path "resources/scss"}]

  :hooks [leiningen.sassc]
  :clean-targets ^{:protect false} [:target-path [:cljsbuild :builds :app :compiler :output-dir] [:cljsbuild :builds :app :compiler :output-to]]
  :cljsbuild
  {:builds
   ; for dev/test builds, see the profile-specific configs below
   [{:id "admin-prod"
     :source-paths ["src-cljs/admin"]
     :compiler {:output-to "resources/public/js/compiled/admin/admin.js"
                :main raiseyourgame.core
                :optimizations :advanced
                :pretty-print false}}]}

  :profiles
  {:uberjar {:omit-source true
             :env {:production true}
             :hooks [leiningen.cljsbuild]
             :cljsbuild
             {:jar true
              :builds
              ; TODO: get this working
              {:app
               {:source-paths ["env/prod/cljs"]
                :compiler {:optimizations :advanced :pretty-print false}}}} 

             :aot :all}
   :dev           [:project/dev :profiles/dev]
   :test          [:project/test :profiles/test]
   :project/dev  {:dependencies [[ring/ring-devel "1.4.0"]
                                 [peridot "0.4.1"]
                                 [pjstadig/humane-test-output "0.7.0"]
                                 [org.clojure/tools.nrepl "0.2.10"]
                                 [com.cemerick/piggieback "0.2.1"]
                                 [lein-figwheel "0.5.0-6"]
                                 [mvxcvi/puget "0.8.1"]]
                  :plugins [[lein-figwheel "0.5.0-6"]
                            [com.jakemccrary/lein-test-refresh "0.10.0"]]
                  :test-refresh {:notify-command ["lein-test-refresh-notify"]
                                 :notify-on-success true
                                 :quiet true}
                  :cljsbuild
                  {:builds
                   [{:id "admin-dev"
                     :source-paths ["src-cljs/admin"]
                     :figwheel true
                     :compiler {:main raiseyourgame.core
                                :asset-path "js/compiled/admin/out"
                                :output-to "resources/public/js/compiled/admin/admin.js"
                                :output-dir "resources/public/js/compiled/admin/out"
                                :source-map-timestamp true}}
                    {:id "app-dev"
                     :source-paths ["src-cljs/app"]
                     :figwheel {:on-jsload "raiseyourgame.core/main"}
                     :compiler {:main raiseyourgame.core
                                :asset-path "js/compiled/app/out"
                                :output-to "resources/public/js/compiled/app/app.js"
                                :output-dir "resources/public/js/compiled/app/out"
                                :source-map-timestamp true}}]}
                  :figwheel
                  {:http-server-root "public"
                   :server-port 3449
                   :nrepl-port 7002
                   :css-dirs ["resources/public/css"]
                   :ring-handler raiseyourgame.handler/app}

                  :repl-options {:init-ns raiseyourgame.core
                                 :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                  :injections [(require 'pjstadig.humane-test-output)
                               (pjstadig.humane-test-output/activate!)]
                  ;;when :nrepl-port is set the application starts the nREPL server on load
                  :env {:dev        true
                        :port       3000
                        :nrepl-port 7000}}
   :project/test {:env {:test       true
                        :port       3001
                        :nrepl-port 7001}}
   :profiles/dev {}
   :profiles/test {}})
