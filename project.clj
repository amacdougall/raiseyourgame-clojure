(defproject raiseyourgame "0.1.0-SNAPSHOT"
  :description "raiseyourga.me site code"
  :url "http://raiseyourga.me"

  ;; core.async repo
  :repositories {"sonatype-staging"
                 "https://oss.sonatype.org/content/groups/staging/"}

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [liberator "0.9.0"] ;; Webmachine-style REST
                 [compojure "1.1.3"]
                 [ring/ring-jetty-adapter "1.1.0"]
                 [org.clojure/java.jdbc "0.3.0-alpha4"]
                 [postgresql/postgresql "8.4-702.jdbc4"]
                 [cheshire "5.2.0"] ;; json
                 [enlive "1.1.4"] ;; templating
                 [org.clojure/core.async "0.1.222.0-83d0c2-alpha"]
                 ;; clojurescript-specific
                 [enfocus "2.0.0-beta3"]
                 [shoreleave/shoreleave-browser "0.3.0"] ;; HTML5 history
                 [secretary "0.2.0-SNAPSHOT"]] ;; client-side routing

  :plugins [[lein-cljsbuild "0.3.2"]]

  :cljsbuild {:builds
              {:dev
               {:source-paths ["cljs-src"]
                :compiler {:output-to "resources/public/js/main.js"
                           :optimizations :whitespace
                           :pretty-print true}}}}
  :profiles {:dev
             {:plugins [[com.cemerick/austin "0.1.1"]]}}
  :main server.core)
