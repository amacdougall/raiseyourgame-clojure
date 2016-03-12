(ns raiseyourgame.routes.home
  (:require [raiseyourgame.layout :as layout]
            [compojure.core :refer [defroutes GET]]
            [ring.util.http-response :refer [ok]]
            [clojure.java.io :as io]))

(defn home-page []
  (layout/render "home.html"))

(defn admin-page []
  (layout/render "admin.html"))

(defroutes home-routes
  (GET "/admin" [] (admin-page))
  (GET "/admin/*" [] (admin-page))
  (GET "*" [] (home-page)))
