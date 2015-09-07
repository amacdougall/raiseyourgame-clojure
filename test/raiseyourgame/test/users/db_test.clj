(ns raiseyourgame.test.users.db-test
  (:require [raiseyourgame.db.core :as db]
            [raiseyourgame.db.migrations :as migrations]
            [raiseyourgame.test.helpers :refer [has-values has-approximate-time]]
            [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [clj-time.core :as t]
            [clj-time.coerce :refer [from-date]]
            [conman.core :refer [with-transaction]]))

(use-fixtures
  :once
  (fn [f]
    (db/connect!)
    (migrations/migrate ["migrate"])
    (f)))

(def user-values
  {:username "tbogard"
   :password "willbehashed"
   :name "Terry Bogard"
   :profile "Are you okay?"
   :email "tbogard@hakkyokuseiken.org"
   :user_level 0})

(def moderator-values
  {:username "kkaphwan"
   :password "willbehashed"
   :name "Kim Kaphwan"
   :profile "YATATATATATATATATA"
   :email "kkaphwan@taekwondo.kr"
   :user_level 1})

(deftest test-user-creation
  (with-transaction [t-conn db/conn]
    (jdbc/db-set-rollback-only! t-conn)
    (testing "can create and retrieve user"
      (is (= 1 (db/create-user! user-values)))
      (let [user (first (db/get-user-by-email {:email "tbogard@hakkyokuseiken.org"}))]
        (is (has-values (merge user-values {:last_login nil}) user))
        (is (has-approximate-time (t/now) (from-date (:created_at user))))
        (is (has-approximate-time (t/now) (from-date (:updated_at user))))))
    (testing "can retrieve user by username"
      (let [user (first (db/get-user-by-username {:username "tbogard"}))]
        (is (= "tbogard" (:username user)))))))
