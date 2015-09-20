(ns raiseyourgame.test.users.db-test
  "Database-level tests which ensure that the YeSQL queries defined in the
  resources/sql directory match the database schema."
  (:require [raiseyourgame.db.core :as db]
            [raiseyourgame.db.migrations :as migrations]
            [raiseyourgame.test.helpers :refer [has-values has-approximate-time with-rollback-transaction]]
            [raiseyourgame.test.fixtures :as fixtures]
            [bugsbio.squirrel :refer [to-sql to-clj]]
            [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [clj-time.core :as t]
            [clj-time.coerce :refer [from-date]])
  (:import java.sql.SQLException))

; A word of warning: try to use one transaction per test. An SQLException, even
; an intentional one, will abort the transaction, trashing any remaining tests.

; Since these tests operate at the database level, they use YeSQL-style
; snake_case keywords. Code at all higher levels will use kebab-case keywords.
; The fixtures use kebab-case keywords, but we can get around that by wrapping
; all the deftests in a big let. Crude but effective.

(use-fixtures
  :once
  (fn [f]
    (when (nil? @db/conn) (db/connect!))
    (f)))

(let [user-values (to-sql fixtures/user-values)]
  (deftest test-user-creation
    ; Records created within this form will remain in the test database until the
    ; end of the transaction.
    (with-rollback-transaction [t-conn db/conn]
      (let [user (db/create-user<! user-values)]
        (is (not (nil? user))
            "user creation should return the created user")
        (is (has-values (merge user-values {:last_login nil}) user)
            "user created from params should have those values")
        (is (has-approximate-time (t/now) (from-date (:created_at user)))
            "user created_at should be set to current time")
        (is (has-approximate-time (t/now) (from-date (:updated_at user)))
            "user updated_at should be set to current time"))))

  (deftest test-unique-user-constraint
    (with-rollback-transaction [t-conn db/conn]
      (db/create-user<! user-values)
      (is (thrown? SQLException (db/create-user<! user-values)))))

  (deftest test-user-retrieval
    (with-rollback-transaction [t-conn db/conn]
      ; created user remains in test db until transaction ends
      (let [{:keys [username email]} user-values
            {user_id :user_id} (db/create-user<! user-values)]
        (testing "lookup with valid values"
          (let [user (first (db/get-user-by-user-id {:user_id user_id}))]
            (is (= user_id (:user_id user)) "lookup by id succeeds"))
          (let [user (first (db/get-user-by-username {:username username}))]
            (is (= username (:username user)) "lookup by username succeeds"))
          (let [user (first (db/get-user-by-email {:email email}))]
            (is (= email (:email user))) "lookup by email succeeds")))
      (testing "no results for unknown email"
        (is (empty? (db/get-user-by-user-id {:user_id -1})))
        (is (empty? (db/get-user-by-username {:username "invalid"})))
        (is (empty? (db/get-user-by-email {:email "invalid"}))))))

  (deftest test-user-update
    (with-rollback-transaction [t-conn db/conn]
      (let [old-username (:username user-values)
            new-username "jbogard"
            user (db/create-user<! user-values)]
        (is (= 1 (db/update-user! (assoc user :username new-username)))
            "updating user affects exactly one row")
        (let [updated-user (first (db/get-user-by-user-id user))]
          (is (not (nil? updated-user)))
          (is (= new-username (:username updated-user)))
          (is (has-approximate-time (t/now) (from-date (:updated_at user)))))
        (testing "can look up by new username"
          (let [updated-user (first (db/get-user-by-username {:username new-username}))]
            (is (not (nil? updated-user)))
            (is (= new-username (:username updated-user)))))
        (testing "cannot look up by old username"
          (let [updated-user (first (db/get-user-by-username {:username old-username}))]
            (is (nil? updated-user))))))))
