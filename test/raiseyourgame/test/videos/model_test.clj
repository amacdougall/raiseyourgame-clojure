(ns raiseyourgame.test.videos.model-test
  (:require [raiseyourgame.models.user :as user]
            [raiseyourgame.models.video :as video]
            [raiseyourgame.db.core :as db]
            [raiseyourgame.test.fixtures :as fixtures]
            [raiseyourgame.test.helpers :refer [has-values with-rollback-transaction has-approximate-time]]
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

;; Returns a [video user] vector.
(defn- create-test-video! []
  (let [user (user/create! fixtures/user-values)
        video (video/create!
                (assoc fixtures/video-values :user-id (:user-id user)))]
    [video user]))

(deftest test-video-creation
  (with-rollback-transaction [t-conn db/conn]
    (let [[video user] (create-test-video!)]
      (is (has-values fixtures/video-values video)
          "created video should have supplied values")
      (is (= (:user-id user) (:user-id video))
          "created video should have correct user-id")
      (is (has-approximate-time (t/now) (from-date (:created-at video)))
          "video created-at should be set to current time")
      (is (has-approximate-time (t/now) (from-date (:updated-at video)))
          "video updated-at should be set to current time"))))

(deftest test-video-lookup
  (with-rollback-transaction [t-conn db/conn]
    (let [[video user] (create-test-video!)]
      (is (has-values fixtures/video-values
                      (video/lookup {:video-id (:video-id video)}))
          "lookup by video id should succeed")
      (is (nil? (video/lookup {:video-id -1}))
          "lookup by unknown video id should fail"))))

(deftest test-video-update
  (with-rollback-transaction [t-conn db/conn]
    (let [old-blurb (:blurb fixtures/video-values)
          new-blurb "Bad Mr. Frosty wins the fat!"
          [video _] (create-test-video!)]
      (is (not (nil? (video/update! (assoc video :blurb new-blurb))))
          "updating video returns video")
      (let [updated-video (video/lookup {:video-id (:video-id video)})]
        (testing "can update video"
          (is (not (nil? updated-video)))
          (is (= new-blurb (:blurb updated-video))
              "updated video should have new values on lookup")
          (is (has-approximate-time (t/now) (from-date (:updated-at video)))))))))
