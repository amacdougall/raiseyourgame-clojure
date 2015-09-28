(ns raiseyourgame.test.videos.model-test
  (:require [raiseyourgame.models.user :as user]
            [raiseyourgame.models.video :as video]
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

;; Returns a [video user] vector.
(defn- create-test-video! []
  (let [user (user/create! fixtures/user-values)
        video (video/create!
                (assoc fixtures/video-values :user-id (:user-id user)))]
    [video user]))

(deftest test-video-creation
  (with-rollback-transaction [t-conn db/conn]
    (let [[video user] (create-test-video!)]
      (is (has-values? fixtures/video-values video)
          "created video should have supplied values")
      (is (has-approximate-time (t/now) (from-date (:created-at video)))
          "video created-at should be set to current time")
      (is (has-approximate-time (t/now) (from-date (:updated-at video)))
          "video updated-at should be set to current time"))))

(deftest test-video-lookup
  (with-rollback-transaction [t-conn db/conn]
    (let [[video user] (create-test-video!)]
      (is (has-values? fixtures/video-values
                       (video/lookup {:video-id (:video-id video)}))
          "lookup by video id should succeed")
      (is (nil? (video/lookup {:video-id -1}))
          "lookup by unknown video id should fail"))))

(deftest test-find-by-user-id
  (with-rollback-transaction [t-conn db/conn]
    (let [[video user] (create-test-video!)
          results (video/find-by-user-id (:user-id user))]
      (is (seq? results)
          "find by user id should return results")
      (is (some (partial has-values? video) results)
          "find by user id should include the expected video"))))

(deftest test-video-update
  (with-rollback-transaction [t-conn db/conn]
    (let [old-blurb (:blurb fixtures/video-values)
          new-blurb "Bad Mr. Frosty wins the fat!"
          [video _] (create-test-video!)]
      (is (map? (video/update! (assoc video :blurb new-blurb)))
          "updating video returns video")
      (let [updated-video (video/lookup {:video-id (:video-id video)})]
        (is (map? updated-video)
            "looking up updated video should succeed")
        (is (= new-blurb (:blurb updated-video))
            "updated video should have new values on lookup")
        ; we're creating and updating this almost instantly, so this test is
        ; not actually terribly useful.
        (is (has-approximate-time (t/now) (from-date (:updated-at updated-video)))
            "updated video should have correct updated-at timestamp")))))
