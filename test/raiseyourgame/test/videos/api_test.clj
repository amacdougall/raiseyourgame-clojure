(ns raiseyourgame.test.videos.api-test
  (:require [raiseyourgame.db.core :as db]
            [raiseyourgame.db.migrations :as migrations]
            [raiseyourgame.test.helpers :refer :all]
            [raiseyourgame.models.user :as user]
            [raiseyourgame.models.video :as video]
            [raiseyourgame.handler :refer [app]]
            [raiseyourgame.test.fixtures :as fixtures]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [peridot.core :refer [request]]
            [conman.core :refer [with-transaction]]))

(use-fixtures
  :once
  (fn [f]
    (when (nil? @db/conn) (db/connect!))
    (f)))

;; When testing updates, timestamps can confuse the issue.
(defn- without-timestamps [video]
  (dissoc video :updated-at :created-at))

(defn- update-request [session video]
  (request session (format "/api/videos/%d" (:video-id video))
           :request-method :put
           :content-type "application/transit+json"
           :body (transit-write video)))

(defn- remove-request [session video]
  (request session (format "/api/videos/%d" (:video-id video))
           :request-method :delete))

(deftest test-video-create
  (with-rollback-transaction [t-conn db/conn]
    (let [user (user/create! fixtures/user-values)
          body (transit-write
                 (assoc fixtures/video-values :user-id (:user-id user)))]
      (testing "without login"
        (let [response (-> (session app)
                         (request "/api/videos"
                                  :request-method :post
                                  :content-type "application/transit+json"
                                  :body body)
                         :response)
              video (response->clj response)]
          (is (= 401 (:status response))
              "attempting to create video while logged out should fail")))
      (testing "with login"
        (let [response (-> (session app)
                         (login-request fixtures/user-values)
                         (request "/api/videos"
                                  :request-method :post
                                  :content-type "application/transit+json"
                                  :body body)
                         :response)
              video (response->clj response)]
          ; The HTTP 201 Created response includes a Location header where the new
          ; resource can be found, and includes the resource itself in the body.
          ; The header must be a string, not a keyword; and since strings are
          ; not fn-able, get-in is more convenient.
          (is (= 201 (:status response))
              "response should be 201 Created")
          (is (has-values? fixtures/video-values video)
              "response body should be the created video")
          (is (string? (get-in response [:headers "Location"]))
              "response should include a Location header")
          (is (= (format "/api/videos/%d" (:video-id video))
                 (get-in response [:headers "Location"]))
              "Location header should match the resource URL"))))))

(deftest test-find-by-video-id
  (with-rollback-transaction [t-conn db/conn]
    (let [[video _] (fixtures/create-test-video!)]
      (testing "with correct video-id"
        (let [path (format "/api/videos/%d" (:video-id video))
              response (-> (session app) (request path) :response)]
          (is (= 200 (:status response))
              "should return 200")
          (let [result (response->clj response)]
            (is (has-values? fixtures/video-values result)
                "video has expected values"))))
      (testing "with nonexistent video-id"
        (let [path "/api/videos/0"
              response (-> (session app) (request path) :response)]
          (is (= 404 (:status response))
              "should return 404"))))))

(deftest test-find-by-user-id
  (with-rollback-transaction [t-conn db/conn]
    (let [[video user] (fixtures/create-test-video!)]
      (testing "with correct user-id"
        (let [path (format "/api/users/%d/videos" (:user-id user))
              response (-> (session app) (request path) :response)]
          (is (= 200 (:status response))
              "should return 200")
          ; can't compare against video directly, because we aren't
          ; converting the SQL datetime strings into java.util.Dates
          ; or anything. We should probably do this eventually.
          (is (some (partial has-values? fixtures/video-values)
                    (response->clj response)))))
      (testing "with nonexistent user-id"
        (let [path "/api/users/0/videos"
              response (-> (session app) (request path) :response)]
          (is (= 200 (:status response))
              "should return 200, even though no results were found")
          (is (empty? (response->clj response))
              "result set should be an empty list"))))))

(deftest test-video-update-as-owner
  (with-rollback-transaction [t-conn db/conn]
    (let [[video user] (fixtures/create-test-video!)
          desired (assoc video :title "New title")
          expected (without-timestamps desired)]
      (let [response (-> (session app)
                       (login-request fixtures/user-values)
                       (update-request desired)
                       :response)
            actual (response->clj response)]
        (is (= 200 (:status response))
            "response should be 200")
        (is (has-values? expected actual)
            "response body should be the updated video")))))

(deftest test-video-update-as-admin
  (with-rollback-transaction [t-conn db/conn]
    (let [[video user] (fixtures/create-test-video!)
          admin (fixtures/create-test-admin!)
          desired (assoc video :title "New title")
          expected (without-timestamps desired)]
      (let [response (-> (session app)
                       (login-request fixtures/admin-values)
                       (update-request desired)
                       :response)
            actual (response->clj response)]
        (is (= 200 (:status response))
            "response should be 200")
        (is (has-values? expected actual)
            "response body should be the updated video")))))

(deftest test-video-update-failures
  (with-rollback-transaction [t-conn db/conn]
    (let [[video user] (fixtures/create-test-video!)
          user-two (fixtures/create-test-user-two!)
          desired (assoc video :title "New title")
          expected (without-timestamps desired)]
      (testing "while logged out"
        (let [response (-> (session app)
                         (update-request desired)
                         :response)
              actual (response->clj response)]
          (is (= 401 (:status response))
              "attempting to modify video while logged out should fail")))
      (testing "as unprivileged user"
        (let [response (-> (session app)
                         (login-request fixtures/user-values-two)
                         (update-request desired)
                         :response)
              actual (response->clj response)]
          (is (= 403 (:status response))
              "unprivileged users should be forbidden to update video"))))))

(deftest test-video-remove-as-owner
  (with-rollback-transaction [t-conn db/conn]
    (let [[video user] (fixtures/create-test-video!)]
      (let [response (-> (session app)
                       (login-request fixtures/user-values)
                       (remove-request video)
                       :response)]
        (is (= 204 (:status response))
            "response should be 204"))
      (let [response (-> (session app)
                       (request (format "/api/videos/%d" (:video-id video)))
                       :response)]
        (is (= 404 (:status response))
            "removed videos should be invisible to public API")))))

(deftest test-video-remove-as-admin
  (with-rollback-transaction [t-conn db/conn]
    (let [[video user] (fixtures/create-test-video!)
          admin (fixtures/create-test-admin!)]
      (let [response (-> (session app)
                       (login-request fixtures/admin-values)
                       (remove-request video)
                       :response)]
        (is (= 204 (:status response))
            "response should be 204")))))

(deftest test-video-remove-failures
  (with-rollback-transaction [t-conn db/conn]
    (let [[video user] (fixtures/create-test-video!)
          user-two (fixtures/create-test-user-two!)]
      (testing "while logged out"
        (let [response (-> (session app)
                         (remove-request video)
                         :response)]
          (is (= 401 (:status response))
              "attempting to remove video while logged out should fail")))
      (testing "as unprivileged user"
        (let [response (-> (session app)
                         (login-request fixtures/user-values-two)
                         (remove-request video)
                         :response)]
          (is (= 403 (:status response))
              "unprivileged users should be forbidden to remove video"))))))
