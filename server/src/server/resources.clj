(ns server.resources
  (:require [liberator.core :refer [resource defresource]]
            [cheshire.core :as cheshire]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql]))

;; TODO: relocate to a config file
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
      nil))
  :post!
  (fn [context]
    (let [body (slurp (get-in context [:request :body]))
          data (cheshire/parse-string body)]
      ;; TODO: insert into database
      nil)))
