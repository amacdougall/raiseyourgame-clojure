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

(deftest test-username-available
  (with-rollback-transaction [t-conn db/conn]
    (user/create! fixtures/user-values)
    (is (= true (user/username-available? "jhigashi")))
    (is (= false (user/username-available? (:username fixtures/user-values))))))

(deftest test-email-available
  (with-rollback-transaction [t-conn db/conn]
    (user/create! fixtures/user-values)
    (is (= true (user/email-available? "jhigashi@muaythai.org")))
    (is (= false (user/email-available? (:email fixtures/user-values))))))

(deftest test-unique-user-constraint
  (with-rollback-transaction [t-conn db/conn]
    (user/create! fixtures/user-values)
    (let [with-dupe-username (assoc fixtures/user-values :email "available@example.com")
          with-dupe-email (assoc fixtures/user-values :username "available")]
      (is (nil? (user/create! with-dupe-username)))
      (is (nil? (user/create! with-dupe-email))))))

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
    (let [new-values {:username "jbogard"
                      :password "rising tackle"
                      :email "jbogard@hakkyokuseiken.org"}
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
      (testing "email was changed"
        (let [found-user (user/lookup {:email (:email new-values)})]
          (is (not (nil? found-user))
              "should be able to look up user by new email")
          (is (= (:email new-values) (:email found-user))
              "looking up user by new email should get updated user"))
        (let [found-user (user/lookup (:email fixtures/user-values))]
          (is (nil? found-user)
              "looking up user by old email should fail")))
      (testing "password was changed"
        (is (user/valid-password? updated-user (:password new-values))
            "new password should be valid for updated user")
        (is (not (user/valid-password? updated-user (:password fixtures/user-values)))
            "old password should not be valid for updated user")))))

(deftest test-user-update-constraints
  (with-rollback-transaction [t-conn db/conn]
    (let [user (user/create! fixtures/user-values)
          moderator (user/create! fixtures/moderator-values)]
      (is (nil? (user/update! (assoc user :username (:username moderator))))
          "should be impossible to change username to one already in use")
      (is (nil? (user/update! (assoc user :email (:email moderator))))
          "should be impossible to change email to one already in use"))))

(deftest test-can-view-private-data
  (with-rollback-transaction [t-conn db/conn]
    (let [user (fixtures/create-test-user!)
          user-two (fixtures/create-test-user-two!)
          moderator (fixtures/create-test-moderator!)
          moderator-two (fixtures/create-test-moderator-two!)
          admin (fixtures/create-test-admin!)
          admin-two (fixtures/create-test-admin-two!)]
      ; user should be able to view own private data only
      (is (user/can-view-private-data? user user)
          "user should be able to view own data")
      (is (not (user/can-view-private-data? user moderator))
          "user should not be able to view higher data")

      ; moderator should be able to view private data only of users
      (is (user/can-view-private-data? moderator user)
          "moderator should be able to view user data")
      (is (not (user/can-view-private-data? moderator moderator-two))
          "moderator should not be able to view other moderators' data")
      (is (not (user/can-view-private-data? moderator admin))
          "moderator should not be able to view admins' data")

      ; admins should be able to view all data
      (is (user/can-view-private-data? admin user)
          "admins should be able to view user data")
      (is (user/can-view-private-data? admin moderator)
          "admins should be able to view moderator data")
      (is (user/can-view-private-data? admin admin-two)
          "admins should be able to view admin data"))))

(deftest test-can-update-user
  (let [user {:user-level (:user user/user-levels)}
        moderator {:user-level (:moderator user/user-levels)}]
    (is (= true (user/can-update-user? moderator user))
        "moderator should be able to update user")
    (is (= false (user/can-update-user? user moderator))
        "user should not be able to update moderator")))

(deftest test-can-remove-user
  (with-rollback-transaction [t-conn db/conn]
    (let [user (fixtures/create-test-user!)
          moderator (fixtures/create-test-moderator!)
          admin (fixtures/create-test-admin!)]

      (is (= false (user/can-remove-user? user user))
          "user should not be able to remove self")
      (is (= false (user/can-remove-user? user moderator))
          "user should not be able to remove moderator")
      (is (= false (user/can-remove-user? user admin))
          "user should not be able to remove admin")

      (is (= false (user/can-remove-user? moderator user))
          "moderator should not be able to remove user")
      (is (= false (user/can-remove-user? moderator moderator))
          "moderator should not be able to remove self")
      (is (= false (user/can-remove-user? moderator admin))
          "moderator should not be able to remove admin")

      (is (= true (user/can-remove-user? admin user))
          "admin should be able to remove user")
      (is (= true (user/can-remove-user? admin moderator))
          "admin should be able to remove moderator")
      (is (= false (user/can-remove-user? admin admin))
          "admin should not be able to remove self"))))

(deftest test-user-remove
  (with-rollback-transaction [t-conn db/conn]
    (let [user (user/create! fixtures/user-values)
          removed-user (user/remove! user)]
      (is (not (nil? removed-user)))
      (is (map? removed-user))
      (is (= (:active removed-user) false)
          "removed user should not be active"))))
