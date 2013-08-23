(ns server.core
  (:require [liberator.core :refer [resource defresource]]
            [ring.adapter.jetty :refer [run-jetty]]
            [compojure.core :refer [defroutes ANY GET POST]]
            [cheshire.core :as cheshire]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql]))

(def db-spec "postgres://postgres:postgres@localhost:5432/raiseyourgame-dev")

(defresource users []
  :available-media-types ["application/json"]
  :exists?
  (fn [_]
    {:users (jdbc/query db-spec (sql/select * :users))})
  :handle-ok
  (fn [context]
    (cheshire/generate-string (:users context))))

(defresource user [id]
  :allowed-methods [:get]
  :available-media-types ["application/json"]
  :exists?
  (fn [_]
    (if-let [resultset (jdbc/query db-spec
                         (sql/select * :users
                           (sql/where {:id (. Integer parseInt id)})))]
      {:user (first resultset)}))
  :handle-ok
  (fn [context]
    (cheshire/generate-string (:user context)))
  :put!
  (fn [context]
    (let [body (slurp (get-in context [:request :body]))
          data (cheshire/parse-string body)]
      ;; TODO: update in database
      nil)))

(defresource create-user []
  :allowed-methods [:get :post]
  :available-media-types ["text/plain"]
  ;; TODO: authenticate
  :handle-ok
  (fn [_] "Please post to this URL with a JSON body.")
  :post!
  (fn [context]
    (let [body (slurp (get-in context [:request :body]))
          data (cheshire/parse-string body)]
      ;; TODO: insert into database
      nil)))

(defroutes app
  (GET "/users" [] (users))
  (GET "/user/:id" [id] (user id))
  (POST "/user" [] (create-user)))

(run-jetty #'app {:join? false :port 3000})
