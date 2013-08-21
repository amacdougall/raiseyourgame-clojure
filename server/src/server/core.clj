(ns server.core
  (:require [liberator.core :refer [resource defresource]]
            [ring.adapter.jetty :refer [run-jetty]]
            [compojure.core :refer [defroutes ANY GET POST]]
            [clojure.data.json :as json]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql]))

(def db-spec "postgres://postgres:postgres@localhost:5432/raiseyourgame-dev")

;; TODO: replace with a timestamps-to-strings function instead
(defn resultset-to-users [resultset]
  (map #(assoc % :created (-> % :created .toString)) resultset))

(defresource users []
  :available-media-types ["application/json"]
  :handle-ok
  (fn [_]
    (-> (jdbc/query db-spec (sql/select * :users))
      resultset-to-users
      json/write-str)))

(defresource user [id]
  :available-media-types ["application/json"]
  :handle-ok
  (fn [_]
    ;; TODO: actual implementation
    nil))

(defroutes app
  (GET "/users" [] (users))
  (GET "/user/:id" [id] (user id)))

;; okay to join? false?
(run-jetty #'app {:join? false :port 3000})
