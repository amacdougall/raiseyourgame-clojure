(defproject raiseyourgame "0.1.0-SNAPSHOT"
  :description "raiseyourga.me site code"
  :url "http://raiseyourga.me"

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2030"]
                 [org.clojure/core.async "0.1.256.0-1bf8cf-alpha"]
                 [liberator "0.9.0"] ; Webmachine-style REST
                 [compojure "1.1.3"] ; routing
                 [ring/ring-jetty-adapter "1.1.0"] ; server
                 [org.clojure/java.jdbc "0.3.0-alpha4"]
                 [postgresql/postgresql "8.4-702.jdbc4"]
                 [cheshire "5.2.0"] ; json
                 [enlive "1.1.4"] ; templating
                 ; clojurescript-specific
                 [enfocus "2.0.1"] ; DOM interaction, templating
                 [secretary "0.2.0-SNAPSHOT"]] ; client-side routing

  :plugins [[lein-cljsbuild "0.3.2"]]

  :cljsbuild {:builds
              {:dev
               {:source-paths ["cljs-src"]

                ; the following works, but yields a less useful source map, at least
                ; in ClojureScript 0.0-2030.
                ; :compiler {:output-to "static/js/main.js"
                ;            :output-dir "out" ; relative to location of main.js
                ;            :source-map "static/js/main.js.map"
                ;            :optimizations :whitespace
                ;            }}}}

                ; This one requires an explicit link to goog/base.js in the
                ; HTML header, and an explicit goog.require of the main
                ; namespace, but it results in a much more accurate source map.
                :compiler {:output-to "static/js/main.js"
                           :output-dir "static/js/out"
                           :optimizations :none
                           :source-map true}}}}
  :profiles {:dev
             {:plugins [[com.cemerick/austin "0.1.1"]]}}
  :main raiseyourgame.core)
