(ns raiseyourgame.routes.api.videos
  (:require [raiseyourgame.db.core :as db]
            [raiseyourgame.schemata :refer :all]
            [raiseyourgame.models.user :as user]
            [raiseyourgame.models.video :as video]
            [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [taoensso.timbre :refer [debug]]))

;; Routes to be included in the "/api/videos" context.
(defroutes* videos-routes
  ; lookup by id
  (GET* "/:video-id" []
        :return Video
        :path-params [video-id :- Long]
        :summary "Numeric video id."
        (if-let [video (video/lookup {:video-id video-id})]
          (ok video)
          (not-found "No video matched your request.")))

  ; create
  (POST* "/" request
         :body-params [user-id :- Long
                       url :- String
                       title :- String
                       blurb :- String
                       description :- String]
         (if (:identity (:session request))
           ; Return private representation of the video, with Location header.
           (let [video (video/create! (:body-params request))
                 location (format "/api/videos/%d" (:video-id video))
                 response (created video)]
             (assoc-in response [:headers "Location"] location))
           ; If no user is logged in, return 401
           (unauthorized "You must be logged in to create a video."))))
