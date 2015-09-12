(ns raiseyourgame.test.users.model-test
  (:require [raiseyourgame.models.user :as user]
            [raiseyourgame.db.core :as db]
            [raiseyourgame.db.migrations :as migrations]
            [raiseyourgame.test.helpers :refer [has-values with-rollback-transaction]]
            [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [conman.core :refer [with-transaction]]))

(use-fixtures
  :once
  (fn [f]
    (db/connect!)
    (migrations/migrate ["migrate"])
    (f)))

(def user-values
  {:username "tbogard"
   :password "buster wolf"
   :name "Terry Bogard"
   :profile "Are you okay?"
   :email "tbogard@hakkyokuseiken.org"
   :user_level 0})

(def moderator-values
  {:username "skusanagi"
   :password "eye of the metropolis"
   :name "Saishu Kusanagi"
   :profile "Yoasobi wa kiken ja zo."
   :email "skusanagi@magatama.org"
   :user_level 1})

(deftest user-create-test
  (with-rollback-transaction [t-conn db/conn]
    (let [user (user/create-user! user-values)]
      (is (has-values (dissoc user-values :password) user))
      (is (has-values (dissoc user :password)
                      (first (db/get-user-by-email {:email (:email user-values)})))))))

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
          id (:id user)
          {:keys [username email]} user-values]
      (is (has-values user (user/lookup {:id id})))
      (is (has-values user (user/lookup {:username username})))
      (is (has-values user (user/lookup {:email email})))
      (is (nil? (user/lookup {:username "invalid"}))))))
