(ns raiseyourgame.db
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql]))

;; TODO: relocate to a config file
(def db-spec "postgres://postgres:postgres@localhost:5432/raiseyourgame-dev")

;; users
(defn all-users []
  (jdbc/query db-spec
    (sql/select * :users)))

(defn get-user [id]
  (first (jdbc/query db-spec
           (sql/select * :users
             (sql/where {:id id})))))

(defn update-user [id data]
  (jdbc/update! db-spec :users data (sql/where {:id id})))

(defn insert-user [data]
  (jdbc/insert! db-spec :users data))

(defn delete-user [id]
  (jdbc/delete! db-spec :users (sql/where {:id id})))

;; videos
(defn all-videos []
  (jdbc/query db-spec
    (sql/select * :videos)))

(defn get-video [id]
  (first (jdbc/query db-spec
           (sql/select * :videos
             (sql/where {:id id})))))

(defn update-video [id data]
  (jdbc/update! db-spec :videos data (sql/where {:id id})))

(defn insert-video [data]
  (jdbc/insert! db-spec :videos data))

(defn delete-video [id]
  (jdbc/delete! db-spec :videos (sql/where {:id id})))
