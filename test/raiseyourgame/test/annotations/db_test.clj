(ns raiseyourgame.test.annotations.db-test
  "Database-level tests which ensure that the YeSQL queries defined in the
  resources/sql directory match the database schema."
  (:require [raiseyourgame.db.core :as db]
            [raiseyourgame.db.migrations :as migrations]
            [raiseyourgame.test.helpers :refer :all]
            [raiseyourgame.test.fixtures :as fixtures]
            [bugsbio.squirrel :refer [to-sql to-clj]]
            [clojure.test :refer :all]
            [clj-time.core :as t]
            [clj-time.coerce :refer [from-date]])
  (:import java.sql.SQLException))

(use-fixtures
  :once
  (fn [f]
    (when (nil? @db/conn) (db/connect!))
    (f)))

(let [user-values (to-sql fixtures/user-values)
      video-values (to-sql fixtures/video-values)
      annotation-values (map to-sql fixtures/annotation-values)]
  (deftest test-annotation-creation
    (with-rollback-transaction [t-conn db/conn]
      (let [{user_id :user_id} (db/create-user<! user-values)
            {video_id :video_id} (db/create-video<! (assoc video-values :user_id user_id))
            ; Build annotations from fixture values, merging user/video ids
            annotations (->> annotation-values
                          (map #(assoc % :user_id user_id :video_id video_id))
                          (map db/create-annotation<!))]
        (is (not (empty? annotations))
            "should build multiple annotations")
        (is (every? (fn [[expected actual]] (has-values? expected actual))
                    (map vector annotation-values annotations))
            "all annotations should have fixture values")
        (is (every? #(= user_id (:user_id %)) annotations)
            "all annotations should have correct user id")
        (is (every? #(= video_id (:video_id %)) annotations)
            "all annotations should have correct video id")
        (is (every? #(has-approximate-time (t/now) (from-date (:created_at %))) annotations)
            "all annotations should be created_at current time")
        (is (every? #(has-approximate-time (t/now) (from-date (:updated_at %))) annotations)
            "all annotations should be updated_at current time"))))

  (deftest test-annotation-retrieval
    (with-rollback-transaction [t-conn db/conn]
      (let [{user_id :user_id} (db/create-user<! user-values)
            {video_id :video_id} (db/create-video<! (assoc video-values :user_id user_id))
            ; Build annotations from fixture values, merging user/video ids
            annotations (->> annotation-values
                          (map #(assoc % :user_id user_id :video_id video_id))
                          (map db/create-annotation<!))]

        (testing "looking up annotation by annotation_id"
          (let [id (:annotation_id (first annotations))
                annotation (first (db/find-annotations-by-annotation-id {:annotation_id id}))]
            (is (not (nil? annotation))
                "can retrieve annotation by annotation_id")
            (is (has-values? (first annotation-values) annotation)
                "annotation looked up by annotation_id has correct values")
            (is (= user_id (:user_id annotation))
                "annotation looked up by annotation_id has correct user_id")
            (is (= video_id (:video_id annotation))
                "annotation looked up by annotation_id has correct video_id")))

        (testing "looking up annotations by video_id"
          (let [annotations (db/find-annotations-by-video-id {:video_id video_id})]
            (is (every? (fn [[expected actual]] (has-values? expected actual))
                        (map vector annotation-values annotations))
                "all annotations should have fixture values")
            (is (every? #(= video_id (:video_id %)) annotations)
                "all annotations should have the correct video id")))))))
