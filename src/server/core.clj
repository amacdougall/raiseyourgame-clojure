(ns server.core
  (:require [server.resources :refer :all]
            [ring.adapter.jetty :refer [run-jetty]]
            [compojure.core :refer [defroutes ANY GET POST]]))

;; Apply API version 1 prefix to supplied route.
(defn v1 [route]
  (str "/api/v1" route))

(defroutes app
  (ANY (v1 "/users") [] (users))
  (ANY (v1 "/users/:id") [id] (users id))
  ;; TODO: "/users/:id/videos" for get/post

  (ANY (v1 "/videos") [] (videos))
  (ANY (v1 "/videos/:id") [id] (videos id))
  ;; TODO: "/videos/:id/annotations" for get/post

  (GET "/static/*" [] static))


(run-jetty #'app {:join? false :port 3000})
