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
           (unauthorized "You must be logged in to create a video.")))

  ; update
  (PUT* "/:video-id" request
        :return Video
        :path-params [video-id :- Long]
        :body [incoming Video]
        :return Video
        :summary "JSON body representation desired video information."
        (let [current (:identity (:session request))
              target (video/lookup {:video-id video-id})
              desired (merge target incoming)]
          (cond
            ; if nobody is logged in, 401
            (nil? current)
            (unauthorized "You must be logged in to update a video.")
            ; if target is not found, 404
            (nil? target)
            (not-found "No video matched your request.")
            ; if logged-in user has insufficient permissions, 403
            (not (user/can-update-video? current target))
            (forbidden "You do not have sufficient permissions to update this video.")
            :else
            (if-let [video (video/update! desired)]
              (ok video)
              ; refine this message if common failure types emerge
              (internal-server-error "The update could not be performed as requested."))))))
