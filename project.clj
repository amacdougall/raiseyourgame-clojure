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
                 [com.rpl/specter "0.9.2"]
                 [com.cognitect/transit-clj "0.8.285"]
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
                 [com.cognitect/transit-cljs "0.8.237"]
                 [reagent "0.6.0-alpha"]
                 [reagent-utils "0.1.7"]
                 [re-frame "0.7.0-alpha-2"]
                 [re-com "0.8.0"]
                 [secretary "1.2.3"]
                 [venantius/accountant "0.1.7"]
                 [cljs-ajax "0.5.3"]]

  :min-lein-version "2.5.3"
  :uberjar-name "raiseyourgame.jar"
  :jvm-opts ["-server"]

  :main raiseyourgame.core
  :migratus {:store :database
             :migration-dir "migrations"}
  ; migratus searches for :migration-dir in all classpath directories; since
  ; $PROJECT/resources is on the classpath, we use resources/migrations.

  :plugins [[lein-auto "0.1.2"]
            [lein-environ "1.0.0"]
            [migratus-lein "0.1.7"]
            [lein-cljsbuild "1.1.2" :exclusions [[org.clojure/clojure]]]
            [lein-sassc "0.10.4"]]
  :sassc [{:src "resources/scss/screen.scss"
           :output-to "resources/public/css/screen.css"
           :style "nested"
           :import-path "resources/scss"}
          {:src "resources/scss/admin_screen.scss"
           :output-to "resources/public/css/admin_screen.css"
           :style "nested"
           :import-path "resources/scss"}]

  :auto {:default {:log-color :gray}
         "sassc"  {:paths ["resources/scss"]
                   :file-pattern #"\.scss$"}}
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
                                 [binaryage/devtools "0.5.2"]
                                 [mvxcvi/puget "0.8.1"]]
                  :plugins [[lein-figwheel "0.5.0-6"]
                            ; WARNING: this is a locally installed dev version!
                            ; see (PR number))
                            [lein-doo "0.1.8-SNAPSHOT"]
                            [com.jakemccrary/lein-test-refresh "0.10.0"]]
                  :test-refresh {:notify-command ["lein-test-refresh-notify"]
                                 :notify-on-success true
                                 :quiet true}
                  :doo {:paths {:karma "node_modules/karma/bin/karma"}}
                  :cljsbuild
                  {:builds
                   [{:id "admin-dev"
                     :source-paths ["src-cljs/admin"]
                     :figwheel true
                     :compiler {:main raiseyourgame.core
                                :asset-path "/js/compiled/admin/out"
                                :output-to "resources/public/js/compiled/admin/admin.js"
                                :output-dir "resources/public/js/compiled/admin/out"
                                :source-map-timestamp true
                                :optimizations :none}}
                    {:id "admin-test"
                     :source-paths ["src-cljs/admin" "test-cljs/admin"]

                     ; WARNING: including asset-path and output-dir in the
                     ; compiler settings of test build will prevent `lein doo
                     ; phantom` and `lein doo slimer` from working. I don't
                     ; have the grit to even begin to figure out why.
                     :compiler {:main raiseyourgame.runner
                                :output-to "resources/public/js/compiled/admin-test/test.js"
                                :optimizations :none}}
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
   ; essentially unused; lein test-refresh uses the dev profile
   :project/test {:env {:test       true
                        :port       3001
                        :nrepl-port 7001}}
   :profiles/dev {}
   :profiles/test {}})
