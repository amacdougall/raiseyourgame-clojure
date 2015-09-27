(ns raiseyourgame.test.annotations.model-test
  (:require [raiseyourgame.models.user :as user]
            [raiseyourgame.models.video :as video]
            [raiseyourgame.models.annotation :as annotation]
            [raiseyourgame.db.core :as db]
            [raiseyourgame.test.fixtures :as fixtures]
            [raiseyourgame.test.helpers :refer :all]
            [clj-time.core :as t]
            [clj-time.coerce :refer [from-date]]
            [clojure.test :refer :all]
            [taoensso.timbre :refer [debug]]
            [conman.core :refer [with-transaction]])
  (:import java.sql.SQLException))

(use-fixtures
  :once
  (fn [f]
    (when (nil? @db/conn) (db/connect!))
    (f)))

;; Returns a [annotations video user] vector.
(defn- create-test-annotations! []
  (let [{:keys [user-id] :as user} (user/create! fixtures/user-values)
        video-values (assoc fixtures/video-values :user-id user-id)
        {:keys [video-id] :as video} (video/create! video-values)
        ; Build annotations from fixture values, merging user/video ids
        annotations (->> fixtures/annotation-values
                      (map #(assoc % :user-id user-id :video-id video-id))
                      (map annotation/create!))]
    [annotations video user]))

(deftest test-annotation-creation
  (with-rollback-transaction [t-conn db/conn]
    (let [[annotations _ _] (create-test-annotations!)]
      (is (collection-has-values? fixtures/annotation-values annotations)))))

(deftest test-annotation-lookup
  (with-rollback-transaction [t-conn db/conn]
    (let [[annotations _ _] (create-test-annotations!)
          annotation-id (:annotation-id (first annotations))]
      (is (has-values? (first fixtures/annotation-values)
                      (annotation/lookup {:annotation-id annotation-id}))
          "lookup by annotation id should succeed")
      (is (nil? (annotation/lookup {:annotation-id -1}))
          "lookup by unknown annotation id should fail"))))

(deftest test-find-by-video-id
  (with-rollback-transaction [t-conn db/conn]
    (let [[annotations {video-id :video-id} _] (create-test-annotations!)
          results (annotation/find-by-video-id video-id)]
      (is (seq? results)
          "find by video id should return results")
      (is (collection-has-values? fixtures/annotation-values annotations)
          "find by video id should include the expected video"))))
