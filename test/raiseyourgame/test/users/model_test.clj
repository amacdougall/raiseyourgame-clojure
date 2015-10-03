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
    (let [new-values {:username "jbogard" :password "rising tackle"}
          user (user/create! fixtures/user-values)
          updated-user (user/update! (conj user new-values))]
      (testing "can update user"
        (is (not (nil? updated-user)))
        (is (map? updated-user))
        (is (= (:username new-values) (:username updated-user))
            "updated user should have new values on lookup")
        (is (has-approximate-time (t/now) (from-date (:updated-at user)))))
      (testing "username was changed"
        (let [found-user (user/lookup new-values)]
          (is (not (nil? found-user))
              "should be able to look up user by new username")
          (is (= (:username new-values) (:username found-user))
              "looking up user by new username should get updated user"))
        (let [found-user (user/lookup fixtures/user-values)]
          (is (nil? found-user)
              "looking up user by old username should fail")))
      (testing "password was changed"
        (is (user/valid-password? updated-user (:password new-values))
            "new password should be valid for updated user")
        (is (not (user/valid-password? updated-user (:password fixtures/user-values)))
            "old password should not be valid for updated user")))))
