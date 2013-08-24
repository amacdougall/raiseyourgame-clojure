(ns server.core
  (:require [server.resources :refer :all]
            [ring.adapter.jetty :refer [run-jetty]]
            [compojure.core :refer [defroutes ANY GET POST]]))

(defroutes app
  (ANY "/users" [] (users))
  (ANY "/users/:id" [id] (users id))
  ;; TODO: "/users/:id/videos" for get/post

  (ANY "/videos" [] (videos))
  (ANY "/videos/:id" [id] (videos id)))
  ;; TODO: "/videos/:id/annotations" for get/post

(run-jetty #'app {:join? false :port 3000})
