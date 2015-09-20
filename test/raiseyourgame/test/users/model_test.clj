(ns raiseyourgame.test.users.model-test
  (:require [raiseyourgame.models.user :as user]
            [raiseyourgame.db.core :as db]
            [raiseyourgame.db.migrations :as migrations]
            [raiseyourgame.test.fixtures :refer [user-values]]
            [raiseyourgame.test.helpers :refer [has-values with-rollback-transaction]]
            [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [conman.core :refer [with-transaction]]))

(use-fixtures
  :once
  (fn [f]
    (when (nil? @db/conn) (db/connect!))
    (f)))

(deftest user-create-test
  (with-rollback-transaction [t-conn db/conn]
    (let [user (user/create-user! user-values)]
      (is (has-values (dissoc user-values :password) user))
      (is (has-values (dissoc user :password)
                      (user/lookup {:email (:email user-values)}))))))

(deftest user-password-test
  (with-rollback-transaction [t-conn db/conn]
    (let [user (user/create-user! user-values)
          password (:password user-values)
          hashed (:password user)]
      (is (has-values (dissoc user-values :password) user))
      (is (not= password hashed))
      (is (user/valid-password? user password)))))

(deftest user-password-invalid-test
  (with-rollback-transaction [t-conn db/conn]
    (let [user (user/create-user! user-values)]
      (is (not (user/valid-password? user "invalid"))))))

(deftest user-lookup-test
  (with-rollback-transaction [t-conn db/conn]
    (let [user (user/create-user! user-values)
          user-id (:user-id user)
          {:keys [username email]} user-values]
      (is (has-values user (user/lookup {:user-id user-id})))
      (is (has-values user (user/lookup {:user-id nil :username username})))
      (is (has-values user (user/lookup {:user-id nil :username nil :email email})))
      (is (nil? (user/lookup {:username "invalid"}))))))
