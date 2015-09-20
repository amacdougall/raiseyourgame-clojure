(ns raiseyourgame.test.videos.db-test
  "Database-level tests which ensure that the YeSQL queries defined in the
  resources/sql directory match the database schema."
  (:require [raiseyourgame.db.core :as db]
            [raiseyourgame.db.migrations :as migrations]
            [raiseyourgame.test.helpers :refer [has-values has-approximate-time with-rollback-transaction]]
            [raiseyourgame.test.fixtures :as fixtures]
            [bugsbio.squirrel :refer [to-sql to-clj]]
            [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [clj-time.core :as t]
            [clj-time.coerce :refer [from-date]])
  (:import java.sql.SQLException))

(use-fixtures
  :once
  (fn [f]
    (when (nil? @db/conn) (db/connect!))
    (f)))

(let [user-values (to-sql fixtures/user-values)
      video-values (to-sql fixtures/video-values)]
  (deftest test-video-creation
    (with-rollback-transaction [t-conn db/conn]
      (let [{user_id :user_id} (db/create-user<! user-values)
            video (db/create-video<! (assoc video-values :user_id user_id))]
        (is (not (nil? video))
            "video creation should return the created video")
        (is (has-values video-values video)
            "video created from params should have those values")
        (is (= user_id (:user_id video))
            "video created with user_id should have that user_id")
        (is (has-approximate-time (t/now) (from-date (:created_at video)))
            "video created_at should be set to current time")
        (is (has-approximate-time (t/now) (from-date (:updated_at video)))
            "video updated_at should be set to current time"))))

  (deftest test-video-retrieval
    (with-rollback-transaction [t-conn db/conn]
      (let [{user_id :user_id} (db/create-user<! user-values)
            {video_id :video_id} (db/create-video<! (assoc video-values :user_id user_id))]
        (testing "can retrieve video by video_id"
          (let [video (first (db/get-video-by-video-id {:video_id video_id}))]
            (is (= video_id (:video_id video))
                "video looked up by video_id should have that video_id")
            (is (= user_id (:user_id video))
                "video looked up by video_id should have appropriate user_id")))
        (testing "no results for unknown video_id"
          (is (empty? (db/get-video-by-video-id {:video_id -1}))))))))
