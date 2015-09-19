(ns raiseyourgame.test.users.api-test
  (:require [raiseyourgame.db.core :as db]
            [raiseyourgame.db.migrations :as migrations]
            [raiseyourgame.test.helpers :refer [has-values with-rollback-transaction]]
            [raiseyourgame.models.user :as user]
            [raiseyourgame.handler :refer [app]]
            [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [peridot.core :refer [session request]]
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

(deftest test-user-lookup
  (with-rollback-transaction [t-conn db/conn]
    ; we're doing this in a let because we'll need the user id later
    (let [user (user/create-user! user-values)
          test-success
          (fn [criteria]
            (let [response (-> (session app)
                             (request "/api/users/lookup"
                                              :params criteria)
                             :response)]
              (is (= 200 (:status response)))
              (let [result (user/json->user (slurp (:body response)))]
                (is (has-values (dissoc user-values :password :email) result)
                    "resulting user has expected values")
                (is (empty? (filter #{:password :email} result))
                    "resulting user does not have password or email values"))))
          test-not-found
          (fn [criteria]
            (let [response (-> (session app)
                             (request "/api/users/lookup"
                                              :params criteria)
                             :response)]
              (is (= 404 (:status response))
                  "looking up nonexistent user returns 404")))]

      (testing "looking up user by id"
        (test-success {:id (:id user)})
        (test-not-found {:id 0}))

      (testing "looking up user by username"
        (test-success {:username (:username user)})
        (test-not-found {:username "iyagami"}))

      (testing "looking up user by email"
        (test-success {:email (:email user)})
        (test-not-found {:email "iyagami@magatama.org"}))

      (testing "looking up user with invalid parameters"
        (let [response (-> (session app)
                         (request "/api/users/lookup"
                                          :params {:something "wrong"})
                         :response)]
          (is (= 400 (:status response))))))))

(deftest test-login
  (with-rollback-transaction [t-conn db/conn]
    (user/create-user! user-values)

    (testing "with valid credentials"
      (let [credentials (select-keys user-values #{:username :password})
            response
            (-> (session app)
              (request "/api/users/login"
                               :request-method :post
                               :content-type "application/json"
                               :body (cheshire/generate-string credentials))
              :response)]
        (is (= 200 (:status response)) "returned 200 response")
        (let [user (user/json->user (slurp (:body response)))]
          (is (= (:username credentials) (:username user))
              "returned the authenticated user"))))

    (testing "with invalid credentials"
      (let [credentials {:username (:username user-values) :password "invalid"}
            response
            (-> (session app)
              (request "/api/users/login"
                               :request-method :post
                               :content-type "application/json"
                               :body (cheshire/generate-string credentials))
              :response)]
        (is (= 401 (:status response)))))))
