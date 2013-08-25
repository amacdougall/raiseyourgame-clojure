(defproject
  clang "0.1.0-SNAPSHOT"
  :description "ClojureScript on Angular"
  :url "http://www.raiseyourga.me"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]]
  :source-paths ["web"]
  :plugins [[lein-cljsbuild "0.3.2"]]

  ; To get the shegon server:
  ; CLASSPATH=./client:$CLASSPATH lein shegon
  ; -> http://localhost:19000/

  :cljsbuild {:builds
              {:dev
               {:source-paths ["cljs-src"]
                :compiler {:output-to "resources/public/js/main.js"
                           :optimizations :whitespace
                           :pretty-print true}}

               ;; TODO: set up all this stuff
               :prod
               {:source-paths ["cljs-src"]
                :compiler {:output-to "resources/public/js/clang.js"
                           :pretty-print false
                           :optimizations :advanced}}

               :pre-prod
               {:source-paths ["client" "sample"]

                :compiler {:output-to "resources/public/js/clang_pre.js"
                           :optimizations :simple
                           :pretty-print false
                           }}}
              })

