(ns raiseyourgame.db
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql]
            [environ.core :refer [env]]))

(let [profile (or (keyword (env :db)) :dev)
      config (-> "config/database.edn" slurp read-string)]
  (def spec (-> config profile :spec)))

;; users
(defn all-users []
  (jdbc/query spec
    (sql/select * :users)))

(defn get-user [id]
  (first (jdbc/query spec
           (sql/select * :users
             (sql/where {:id id})))))

(defn update-user [id data]
  (jdbc/update! spec :users data (sql/where {:id id})))

(defn insert-user [data]
  (jdbc/insert! spec :users data))

(defn delete-user [id]
  (jdbc/delete! spec :users (sql/where {:id id})))

;; videos
(defn all-videos []
  (jdbc/query spec
    (sql/select * :videos)))

(defn get-video [id]
  (first (jdbc/query spec
           (sql/select * :videos
             (sql/where {:id id})))))

(defn update-video [id data]
  (jdbc/update! spec :videos data (sql/where {:id id})))

(defn insert-video [data]
  (jdbc/insert! spec :videos data))

(defn delete-video [id]
  (jdbc/delete! spec :videos (sql/where {:id id})))
