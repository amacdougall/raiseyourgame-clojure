(ns raiseyourgame.test.annotations.db-test
  "Database-level tests which ensure that the YeSQL queries defined in the
  resources/sql directory match the database schema."
  (:require [raiseyourgame.db.core :as db]
            [raiseyourgame.db.migrations :as migrations]
            [raiseyourgame.test.helpers :refer [has-values has-approximate-time with-rollback-transaction]]
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
                          (map db/create-annotation<!))
            expectations (map vector annotation-values annotations)]
        (is (not (empty? annotations))
            "should build multiple annotations")
        (is (every? (fn [[expected actual]] (has-values expected actual))
                    (map vector annotation-values annotations))
            "all annotations should have fixture values")
        (is (every? #(= user_id (:user_id %)) annotations)
            "all annotations should have correct user id")
        (is (every? #(= video_id (:video_id %)) annotations)
            "all annotations should have correct video id")
        (is (every? #(has-approximate-time (t/now) (from-date (:created_at %))) annotations)
            "all annotations should be created_at current time")
        (is (every? #(has-approximate-time (t/now) (from-date (:updated_at %))) annotations)
            "all annotations should be updated_at current time")))))
