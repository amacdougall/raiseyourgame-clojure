(ns raiseyourgame.test.users.model-test
  (:require [raiseyourgame.models.user :as user]
            [raiseyourgame.db.core :as db]
            [raiseyourgame.test.fixtures :as fixtures]
            [raiseyourgame.test.helpers :refer :all]
            [clj-time.core :as t]
            [clj-time.coerce :refer [from-date]]
            [clojure.test :refer :all]
            [taoensso.timbre :refer [debug]]
            [conman.core :refer [with-transaction]])
  (:import java.sql.SQLException))

(use-fixtures
  :once
  (fn [f]
    (when (nil? @db/conn) (db/connect!))
    (f)))

(deftest test-user-creation
  (with-rollback-transaction [t-conn db/conn]
    (let [user (user/create! fixtures/user-values)]
      (is (has-values? (dissoc fixtures/user-values :password) user)
          "created user should have supplied values")
      (is (has-approximate-time (t/now) (from-date (:created-at user)))
          "user created-at should be set to current time")
      (is (has-approximate-time (t/now) (from-date (:updated-at user)))
          "user updated-at should be set to current time"))))

(deftest test-unique-user-constraint
  (with-rollback-transaction [t-conn db/conn]
    (user/create! fixtures/user-values)
    (is (nil? (user/create! fixtures/user-values)))))

(deftest test-user-password
  (with-rollback-transaction [t-conn db/conn]
    (let [user (user/create! fixtures/user-values)
          password (:password fixtures/user-values)
          hashed (:password user)]
      (is (not= password hashed)
          "hashed password should be different from input")
      (is (user/valid-password? user password)
          "original password should be valid")
      (is (not (user/valid-password? user "wrong"))
          "wrong password should be invalid"))))

(deftest test-user-lookup
  (with-rollback-transaction [t-conn db/conn]
    (let [user (user/create! fixtures/user-values)
          user-id (:user-id user)
          {:keys [username email]} fixtures/user-values]
      (is (has-values? user (user/lookup {:user-id user-id}))
          "lookup by id should succeed")
      (is (has-values? user (user/lookup {:user-id nil :username username}))
          "lookup by username should succeed")
      (is (has-values? user (user/lookup {:user-id nil :username nil :email email}))
          "lookup by email should succeed")
      (is (nil? (user/lookup {:user-id -1}))
          "lookup by unknown user-id should fail")
      (is (nil? (user/lookup {:username "unknown"}))
          "lookup by unknown username should fail")
      (is (nil? (user/lookup {:email "unknown"}))
          "lookup by unknown email should fail"))))

(deftest test-user-update
  (with-rollback-transaction [t-conn db/conn]
    (let [old-username (:username fixtures/user-values)
          new-username "jbogard"
          user (user/create! fixtures/user-values)]
      (is (not (nil? (user/update! (assoc user :username new-username))))
          "updating user returns user")
      (let [updated-user (user/lookup {:user-id (:user-id user)})]
        (testing "can update user"
          (is (not (nil? updated-user)))
          (is (= new-username (:username updated-user))
              "updated user should have new values on lookup")
          (is (has-approximate-time (t/now) (from-date (:updated-at user)))))
        (testing "can look up by new username"
          (let [updated-user (user/lookup {:username new-username})]
            (is (not (nil? updated-user)))
            (is (= new-username (:username updated-user)))))
        (testing "cannot look up by old username"
          (let [updated-user (user/lookup {:username old-username})]
            (is (nil? updated-user))))))))
