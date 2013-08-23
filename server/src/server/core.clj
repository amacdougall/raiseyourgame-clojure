(ns server.core
  (:require [server.resources :refer :all]
            [ring.adapter.jetty :refer [run-jetty]]
            [compojure.core :refer [defroutes ANY GET POST]]))

(defroutes app
  (GET "/users" [] (users))
  (GET "/user/:id" [id] (user id))
  (POST "/user" [] (user))

  (GET "/videos" [] (videos))
  (GET "/video/:id" [id] (video id))
  (POST "/video" [] (video)))

(run-jetty #'app {:join? false :port 3000})
