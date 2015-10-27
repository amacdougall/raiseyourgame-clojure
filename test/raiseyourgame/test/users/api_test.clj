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

;; Creates a test user and returns a private representation, as if loaded from
;; the API by an owner/admin.
(defn create-test-user! []
  (user/private (user/create! fixtures/user-values)))

;; Creates a test user with the moderator user-level and returns a private
;; representation, as if loaded from the API by an owner/admin.
(defn create-test-moderator! []
  (-> fixtures/moderator-values
    (user/create!) ; all users are created with user-level 0
    (user/update! update :user-level inc)
    (user/private)))

;; When testing updates, timestamps can confuse the issue.
(defn- without-timestamps [user]
  (dissoc user :last-login :updated-at :created-at))

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
          "response should be 201 Created")
      (is (has-values? (user/private fixtures/user-values) user)
          "response body should be the created user")
      (is (string? (get-in response [:headers "Location"]))
          "response should include a Location header")
      (is (= (format "/api/users/%d" (:user-id user))
             (get-in response [:headers "Location"]))
          "Location header should match the resource URL"))))

;; It should not be possible to create a user with an email or username which
;; is already in use.
(deftest test-user-create-duplicate
  (with-rollback-transaction [t-conn db/conn]
    (user/create! fixtures/user-values) ; make sure user already exists
    (let [with-dupe-username (assoc fixtures/user-values :email "available@example.com")
          with-dupe-email (assoc fixtures/user-values :username "available")
          test-user-creation
          (fn [user-values error-keys]
            (let [body (cheshire/generate-string user-values)
                  response (-> (session app)
                             (request "/api/users"
                                      :request-method :post
                                      :content-type "application/json"
                                      :body body)
                             :response)
                  response-body (response->clj response)]
              (is (= 400 (:status response))
                  "response should be 400 Bad Request")))]
      (test-user-creation fixtures/user-values [:username :email])
      (test-user-creation with-dupe-username [:username])
      (test-user-creation with-dupe-email [:email]))))

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
                (is (has-values? (user/public fixtures/user-values) result)
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

;; Given a user and a {:username String :password String} map, test login using
;; the API. This helper method should be used to test logins after user
;; creation/update. It conveniently also attempts to log in with an invalid
;; password.
; Not the greatest name, I know.
(defn- exercise-login-routes [credentials]
  (let [response
        (-> (session app)
          (request "/api/users/login"
                   :request-method :post
                   :content-type "application/json"
                   :body (cheshire/generate-string credentials))
          :response)
        authenticated-user (response->clj response)]
    (is (= 200 (:status response)) "login returned 200 response")
    (is (= (:username credentials) (:username authenticated-user))
        "login returned the expected user"))

  ; log in, then hit the /api/users/current route to test logged-in-ness
  (let [response
        (-> (session app)
          (request "/api/users/login"
                   :request-method :post
                   :content-type "application/json"
                   :body (cheshire/generate-string credentials))
          (request "/api/users/current")
          :response)
        current-user (response->clj response)]
    (is (= 200 (:status response))
        "after login, current returned 200 response")
    (is (= (:username credentials) (:username current-user))
        "after login, current returned logged-in user"))

  (let [invalid-credentials (assoc credentials :password "hunter2")
        response
        (-> (session app)
          (request "/api/users/login"
                   :request-method :post
                   :content-type "application/json"
                   :body (cheshire/generate-string invalid-credentials))
          :response)]
    (is (= 401 (:status response))
        "logging in with incorrect password should fail")))

(deftest test-login
  (with-rollback-transaction [t-conn db/conn]
    (user/create! fixtures/user-values)
    (exercise-login-routes
      (select-keys fixtures/user-values #{:username :password}))))

(deftest test-user-update-failures
  (with-rollback-transaction [t-conn db/conn]
    (let [credentials-for #(select-keys % #{:username :password})
          original (create-test-user!)
          moderator (create-test-moderator!)
          expected (conj original {:password "rising tackle"
                                   :email "tbogard@garou.org"})]
      ; attempt to update while logged out
      (let [response
            (-> (session app)
              (request (format "/api/users/%d" (:user-id expected))
                       :request-method :put
                       :content-type "application/json"
                       :body (cheshire/generate-string expected))
              :response)]
        (is (= 401 (:status response))
            "attempting to update user while logged out should fail"))

      ; attempt to update nonexistent user
      (let [response
            (-> (session app)
              ; log in first...
              (request "/api/users/login"
                       :request-method :post
                       :content-type "application/json"
                       :body (cheshire/generate-string
                               (credentials-for fixtures/user-values)))
              ; ...and then update a nonexistent user
              (request (format "/api/users/%d" -1)
                       :request-method :put
                       :content-type "application/json"
                       :body (cheshire/generate-string expected))
              :response)]
        (is (= 404 (:status response))
            "attempting to update nonexistent user should fail"))

      ; attempt to update impermissible user
      (let [response
            (-> (session app)
              ; log in first...
              (request "/api/users/login"
                       :request-method :post
                       :content-type "application/json"
                       :body (cheshire/generate-string
                               (credentials-for fixtures/user-values)))
              ; ...and then update an impermissible user
              (request (format "/api/users/%d" (:user-id moderator))
                       :request-method :put
                       :content-type "application/json"
                       :body (cheshire/generate-string
                               (assoc moderator :username "syabuki")))
              :response)]
        (is (= 401 (:status response))
            "attempting to update a user without proper permissions should fail"))

      ; attempt to set unavailable username
      (let [bad-username (assoc expected :username (:username moderator))
            response
            (-> (session app)
              ; log in first...
              (request "/api/users/login"
                       :request-method :post
                       :content-type "application/json"
                       :body (cheshire/generate-string
                               (credentials-for fixtures/user-values)))
              ; ...and then update sending an unavailable username
              (request (format "/api/users/%d" (:user-id original))
                       :request-method :put
                       :content-type "application/json"
                       :body (cheshire/generate-string bad-username))
              :response)]
        (is (= 400 (:status response))
            "attempting to update to unavailable username should fail"))

      ; attempt to set unavailable email
      (let [bad-email (assoc expected :email (:email moderator))
            response
            (-> (session app)
              ; log in first...
              (request "/api/users/login"
                       :request-method :post
                       :content-type "application/json"
                       :body (cheshire/generate-string
                               (credentials-for fixtures/user-values)))
              ; ...and then update sending an unavailable email
              (request (format "/api/users/%d" (:user-id original))
                       :request-method :put
                       :content-type "application/json"
                       :body (cheshire/generate-string bad-email))
              :response)]
        (is (= 400 (:status response))
            "attempting to update to unavailable email should fail")))))

(deftest test-user-update-self
  (with-rollback-transaction [t-conn db/conn]
    (let [credentials-for #(select-keys % #{:username :password})
          original (create-test-user!)
          expected (conj original {:password "rising tackle"
                                   :email "tbogard@garou.org"})]
      (let [response
            (-> (session app)
              ; log in first...
              (request "/api/users/login"
                       :request-method :post
                       :content-type "application/json"
                       :body (cheshire/generate-string
                               (credentials-for fixtures/user-values)))
              ; ...and then update self
              (request (format "/api/users/%d" (:user-id original))
                       :request-method :put
                       :content-type "application/json"
                       :body (cheshire/generate-string expected))
              :response)
            actual (response->clj response)]
        ; since update requires authorization, assume a user/private response
        (is (= 200 (:status response))
            "response should be 200")
        (is (has-values? (without-timestamps (user/private expected)) actual)
            "response body should be the updated user, ignoring timestamps"))

      (let [response
            (-> (session app)
              (request "/api/users/lookup" :params {:email (:email expected)})
              :response)]
        (is (= 200 (:status response))
            "should be able to look up user by new email"))

      (let [response (-> (session app)
                       (request "/api/users/lookup"
                                :params {:email (:email original)})
                       :response)]
        (is (= 404 (:status response))
            "should not be able to look up user by old email"))

      (testing "could log in with updated password"
        (exercise-login-routes (credentials-for expected))))))

;; Implicitly tests any user updating a user with a lower user level.
(deftest test-admin-update-other
  (with-rollback-transaction [t-conn db/conn]
    (let [credentials-for #(select-keys % #{:username :password})
          original (create-test-user!)
          moderator (create-test-moderator!) ; will do the update
          expected (conj original {:password "rising tackle"
                                   :email "tbogard@garou.org"})]
      (let [response
            (-> (session app)
              ; log in as moderator...
              (request "/api/users/login"
                       :request-method :post
                       :content-type "application/json"
                       :body (cheshire/generate-string
                               (credentials-for fixtures/moderator-values)))
              ; ...and then update the user
              (request (format "/api/users/%d" (:user-id original))
                       :request-method :put
                       :content-type "application/json"
                       :body (cheshire/generate-string expected))
              :response)
            actual (response->clj response)]
        ; since update requires authorization, assume a user/private response
        (is (= 200 (:status response))
            "response should be 200")
        (is (has-values? (without-timestamps (user/private expected)) actual)
            "response body should be the updated user, ignoring timestamps"))

      (let [response
            (-> (session app)
              (request "/api/users/lookup" :params {:email (:email expected)})
              :response)]
        (is (= 200 (:status response))
            "should be able to look up user by new email"))

      (let [response
            (-> (session app)
              (request "/api/users/lookup" :params {:email (:email original)})
              :response)]
        (is (= 404 (:status response))
            "should not be able to look up user by old email")))))
