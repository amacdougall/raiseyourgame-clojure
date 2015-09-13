(ns raiseyourgame.test.users.api-test
  (:require [raiseyourgame.db.core :as db]
            [raiseyourgame.db.migrations :as migrations]
            [raiseyourgame.test.helpers :refer [has-values with-rollback-transaction]]
            [raiseyourgame.models.user :as user]
            [raiseyourgame.handler :refer [app]]
            [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [ring.mock.request :refer :all]
            [cheshire.core :as cheshire]
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

(deftest test-username-available
  (with-transaction [t-conn db/conn]
    (jdbc/db-set-rollback-only! t-conn)
    (user/create-user! user-values)
    (let [response (app (request :get "/api/users/available/tbogard"))]
      (is (= 200 (:status response)))
      (is "false" (slurp (:body response))))
    (let [response (app (request :get "/api/users/available/iyagami"))]
      (is (= 200 (:status response)))
      (is "true" (slurp (:body response))))))

(deftest test-login
  (with-rollback-transaction [t-conn db/conn]
    (user/create-user! user-values)

    (testing "with valid credentials"
      (let [credentials (select-keys user-values #{:username :password})
            req (-> (request :post "/api/users/login")
                  (content-type "application/json")
                  (body (cheshire/generate-string credentials)))
            res (app req)]
        (is (= 200 (:status res))
            "returned 200 response")
        (let [user (cheshire/parse-string (slurp (:body res)))
              session (:session res)]
          (is (= (credentials "username") (:username user))
              "returned the authenticated user"))))

    (testing "with invalid credentials"
      (let [credentials {:username (:username user-values) :password "invalid"}
            req (-> (request :post "/api/users/login")
                  (content-type "application/json")
                  (body (cheshire/generate-string credentials)))
            res (app req)]
        (is (= 401 (:status res)))))))
