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
            [peridot.core :refer [session request]]
            [cheshire.core :as cheshire]
            [conman.core :refer [with-transaction]]))

(use-fixtures
  :once
  (fn [f]
    (when (nil? @db/conn) (db/connect!))
    (f)))

;; Returns a [video user] vector.
(defn- create-test-video! []
  (let [user (user/create! fixtures/user-values)
        video (video/create!
                (assoc fixtures/video-values :user-id (:user-id user)))]
    [video user]))

(deftest test-video-create
  (with-rollback-transaction [t-conn db/conn]
    (let [{user-id :user-id} (user/create! fixtures/user-values)
          body (cheshire/generate-string
                 (assoc fixtures/video-values :user-id user-id))
          response (-> (session app)
                     (request "/api/videos"
                              :request-method :post
                              :content-type "application/json"
                              :body body)
                     :response)
          video (response->clj response)]
      ; The HTTP 201 Created response includes a Location header where the new
      ; resource can be found, and includes the resource itself in the body.
      ; NOTE: strings are not fn-able, so get-in is more convenient.
      (is (= 201 (:status response))
          "response should be 200 Created")
      (is (has-values? fixtures/video-values video)
          "response body should be the created video")
      (is (string? (get-in response [:headers "Location"]))
          "response should include a Location header")
      (is (= (format "/api/videos/%d" (:video-id video))
             (get-in response [:headers "Location"]))
          "Location header should match the resource URL"))))

(deftest test-find-by-video-id
  (with-rollback-transaction [t-conn db/conn]
    (let [[video _] (create-test-video!)]
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
    (let [[video user] (create-test-video!)]
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