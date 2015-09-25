(ns raiseyourgame.test.videos.api-test
  (:require [raiseyourgame.db.core :as db]
            [raiseyourgame.db.migrations :as migrations]
            [raiseyourgame.test.helpers :refer [has-values with-rollback-transaction]]
            [raiseyourgame.models.user :as user]
            [raiseyourgame.models.video :as video]
            [raiseyourgame.handler :refer [app]]
            [raiseyourgame.test.fixtures :as fixtures]
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

(deftest test-get-by-id
  (with-rollback-transaction [t-conn db/conn]
    (let [[video _] (create-test-video!)]
      (testing "with correct id"
        (let [path (format "/api/videos/%d" (:video-id video))
              response (-> (session app) (request path) :response)]
          (is (= 200 (:status response))
              "getting known video by id should return 200")
          (let [result (video/json->video (slurp (:body response)))]
            (is (has-values fixtures/video-values result)
                "resulting video has expected values"))))
      (testing "with unknown id"
        (let [path "/api/videos/0"
              response (-> (session app) (request path) :response)]
          (is (= 404 (:status response))
              "getting video by nonexistent id should return 404"))))))
