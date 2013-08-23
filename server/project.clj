(defproject server "0.1.0-SNAPSHOT"
  :description "raiseyourga.me server"
  :url "http://raiseyourga.me"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [liberator "0.9.0"]
                 [compojure "1.1.3"]
                 [ring/ring-jetty-adapter "1.1.0"]
                 [org.clojure/java.jdbc "0.3.0-alpha4"]
                 [postgresql/postgresql "8.4-702.jdbc4"]
                 [cheshire "5.2.0"]]
  :main server.core)
