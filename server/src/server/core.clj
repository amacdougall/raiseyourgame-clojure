(ns server.core
  (:require [server.resources]
            [ring.adapter.jetty :refer [run-jetty]]
            [compojure.core :refer [defroutes ANY GET POST]]))

(defroutes app
  (GET "/users" [] (users))
  (GET "/user/:id" [id] (user id))
  (POST "/user" [] (user)))

(run-jetty #'app {:join? false :port 3000})
