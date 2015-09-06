(ns raiseyourgame.test.db.users-test
  (:require [raiseyourgame.db.core :as db]
            [raiseyourgame.db.migrations :as migrations]
            [raiseyourgame.test.helpers :refer [has-values has-approximate-time]]
            [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [clj-time.core :as t]
            [clj-time.coerce :refer [from-date]]
            [conman.core :refer [with-transaction]]
            [environ.core :refer [env]]))

(use-fixtures
  :once
  (fn [f]
    (db/connect!)
    (migrations/migrate ["migrate"])
    (f)))

(deftest test-user-creation
  (with-transaction [t-conn db/conn]
    ; always rolls back the transaction
    (jdbc/db-set-rollback-only! t-conn)
    (is (= 1 (db/create-user!
               {:username "skurosawa"
                :password "willbehashed"
                :name "Sho Kurosawa"
                :profile "Endless Rain"
                :email "sho@monsterockband.com"
                :user_level 0})))
    (let [user (first (db/get-user-by-email {:email "sho@monsterockband.com"}))]
      (is (has-values
            {:username "skurosawa"
             :password "willbehashed"
             :name "Sho Kurosawa"
             :profile "Endless Rain"
             :email "sho@monsterockband.com"
             :user_level 0
             :last_login nil}
            user))
      (is (has-approximate-time (t/now) (from-date (:created_at user))))
      (is (has-approximate-time (t/now) (from-date (:updated_at user)))))))
