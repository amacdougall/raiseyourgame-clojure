(ns raiseyourgame.test.users.api-test
  (:require [raiseyourgame.db.core :as db]
            [raiseyourgame.db.migrations :as migrations]
            [raiseyourgame.test.helpers :refer :all]
            [raiseyourgame.models.user :as user]
            [raiseyourgame.handler :refer [app]]
            [raiseyourgame.test.fixtures :as fixtures]
            [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [peridot.core :refer [session request]]
            [cheshire.core :as cheshire]
            [taoensso.timbre :refer [debug]]
            [conman.core :refer [with-transaction]]))

(use-fixtures
  :once
  (fn [f]
    (when (nil? @db/conn) (db/connect!))
    (f)))

(deftest test-user-create
  (with-rollback-transaction [t-conn db/conn]
    (let [body (cheshire/generate-string fixtures/user-values)
          response (-> (session app)
                     (request "/api/users"
                              :request-method :post
                              :content-type "application/json"
                              :body body)
                     :response)
          user (response->clj response)]
      ; The HTTP 201 Created response includes a Location header where the new
      ; resource can be found, and includes the resource itself in the body.
      ; NOTE: strings are not fn-able, so get-in is more convenient.
      (is (= 201 (:status response))
          "response should be 200 Created")
      (is (has-values? (user/private fixtures/user-values) user)
          "response body should be the created user")
      (is (string? (get-in response [:headers "Location"]))
          "response should include a Location header")
      (is (= (format "/api/users/%d" (:user-id user))
             (get-in response [:headers "Location"]))
          "Location header should match the resource URL"))))

(deftest test-get-by-id
  (with-rollback-transaction [t-conn db/conn]
    (let [user (user/create! fixtures/user-values)]
      (testing "with correct id"
        (let [path (format "/api/users/%d" (:user-id user))
              response (-> (session app) (request path) :response)]
          (is (= 200 (:status response))
              "getting known user by id should return 200")
          (let [result (response->clj response)]
            (is (has-values? (dissoc fixtures/user-values :password :email) result)
                "resulting user has expected values")
            (is (empty? (filter #{:password :email} result))
                "resulting user does not have password or email values"))))
      (testing "with unknown id"
        (let [path "/api/users/0"
              response (-> (session app) (request path) :response)]
          (is (= 404 (:status response))
              "getting user by nonexistent id should return 404"))))))

(deftest test-user-lookup
  (with-rollback-transaction [t-conn db/conn]
    ; we're doing this in a let because we'll need the user-id later
    (let [user (user/create! fixtures/user-values)
          test-success
          (fn [criteria]
            (let [response (-> (session app)
                             (request "/api/users/lookup" :params criteria)
                             :response)]
              (is (= 200 (:status response)))
              (let [result (response->clj response)]
                (is (has-values? (dissoc fixtures/user-values :password :email) result)
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

      (testing "looking up user by user-id"
        (test-success {:user-id (:user-id user)})
        (test-not-found {:user-id 0}))

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
    (user/create! fixtures/user-values)

    (testing "with valid credentials"
      (let [credentials (select-keys fixtures/user-values #{:username :password})
            response
            (-> (session app)
              (request "/api/users/login"
                       :request-method :post
                       :content-type "application/json"
                       :body (cheshire/generate-string credentials))
              :response)]
        (is (= 200 (:status response)) "login returned 200 response")
        (let [user (response->clj response)]
          (is (= (:username credentials) (:username user))
              "login returned the authenticated user"))

        ; log in, then hit the /api/users/current route to test logged-in-ness
        (let [response
              (-> (session app)
                (request "/api/users/login"
                         :request-method :post
                         :content-type "application/json"
                         :body (cheshire/generate-string credentials))
                (request "/api/users/current")
                :response)]
          (is (= 200 (:status response))
              "after login, current returned 200 response")
          (let [user (response->clj response)]
            (is (= (:username credentials) (:username user))
                "after login, current returned logged-in user")))))

    (testing "with invalid credentials"
      (let [credentials {:username (:username fixtures/user-values) :password "invalid"}
            response
            (-> (session app)
              (request "/api/users/login"
                       :request-method :post
                       :content-type "application/json"
                       :body (cheshire/generate-string credentials))
              :response)]
        (is (= 401 (:status response)))))))
