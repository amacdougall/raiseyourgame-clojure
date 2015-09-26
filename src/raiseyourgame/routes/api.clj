(ns raiseyourgame.routes.api
  (:require [raiseyourgame.db.core :as db]
            [raiseyourgame.schemata :refer :all]
            [raiseyourgame.models.user :as user]
            [raiseyourgame.models.video :as video]
            [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [taoensso.timbre :refer [debug]]))

(defn- safe-user [user]
  (dissoc user :password :email))

(defapi api-routes
  (ring.swagger.ui/swagger-ui
    "/swagger-ui")
  ;JSON docs available at the /swagger.json route
  (swagger-docs
    {:info {:title "Raise Your API"}})

  (context* "/api" []
    (context* "/users" []
      ;; GET routes
      ;; NOTE: Route order matters! Only check the /:user-id route after others
      ;; have failed to match.
      (GET* "/lookup" []
            :return User
            :query-params [{user-id :- Long nil}
                           {username :- String nil}
                           {email :- String nil}]
            :summary "One of: user-id, username, password."
            (let [criterion (cond
                              (not (nil? user-id)) {:user-id user-id}
                              (not (nil? username)) {:username username}
                              (not (nil? email)) {:email email})]
              (if criterion
                (if-let [user (user/lookup criterion)]
                  (ok (safe-user user))
                  (not-found "No user matched your request."))
                (bad-request "Invalid request. Must supply one of the following
                             querystring parameters: id, username, email."))))

      (GET* "/current" request
            :return User
            (if-let [user (get-in request [:session :identity])]
              (ok (dissoc user :password))
              (not-found)))
      
      (GET* "/:user-id" []
            :return User
            :path-params [user-id :- Long]
            :summary "Numeric user id."
            (if-let [user (user/lookup {:user-id user-id})]
              (ok (safe-user user))
              (not-found "No user matched your request.")))

      ;; POST routes
      (POST* "/login" request
             :return User
             :body-params [{email :- String ""}
                           {username :- String ""}
                           password :- String]
             :summary "Username or email, and unhashed password."
             (let [user (user/lookup {:email email, :username username})]
               (if (user/valid-password? user password)
                 (-> (ok (dissoc user :password))
                   (assoc :session (assoc (:session request) :identity user)))
                 (unauthorized)))))

    (context* "/videos" []
      (GET* "/:video-id" []
            :return Video
            :path-params [video-id :- Long]
            :summary "Numeric video id."
            (if-let [video (video/lookup {:video-id video-id})]
              (ok video)
              (not-found "No video matched your request."))))))
