(ns raiseyourgame.test.db.core
  (:require [raiseyourgame.db.core :as db]
            [raiseyourgame.db.migrations :as migrations]
            [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [clj-time.core :refer [now]]
            [clj-time.coerce :refer [to-sql-time from-sql-time]]
            [conman.core :refer [with-transaction]]
            [environ.core :refer [env]]))

(use-fixtures
  :once
  (fn [f]
    (db/connect!)
    (migrations/migrate ["migrate"])
    (f)))

(defn- has-values [target exemplar]
  (every? (fn [k] (= (target k) (exemplar k))) (keys exemplar)))

(deftest test-users
  (with-transaction [t-conn db/conn]
    (let [timestamp (now)]
      ; always rolls back the transaction
      (jdbc/db-set-rollback-only! t-conn)
      (is (= 1 (db/create-user!
                 {:username "skurosawa"
                  :password "willbehashed"
                  :name "Sho Kurosawa"
                  :profile "Endless Rain"
                  :email "sho@monsterockband.com"
                  :user_level 0})))
      (is (has-values
            {:username "skurosawa"
             :password "willbehashed"
             :name "Sho Kurosawa"
             :profile "Endless Rain"
             :email "sho@monsterockband.com"
             :user_level 0
             :last_login nil
             :created_at timestamp
             :updated_at timestamp}
            (first (db/get-user {:id 1})))))))
