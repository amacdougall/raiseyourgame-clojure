(ns raiseyourgame.routes.api
  (:require [raiseyourgame.db.core :as db]
            [raiseyourgame.schemata :refer :all]
            [raiseyourgame.models.user :as user]
            [raiseyourgame.models.video :as video]
            [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [taoensso.timbre :refer [debug]]))

(defapi api-routes
  (ring.swagger.ui/swagger-ui
    "/swagger-ui")
  ;JSON docs available at the /swagger.json route
  (swagger-docs
    {:info {:title "Raise Your API"}})

  (context* "/api" []
    (context* "/users" []
      ;; user routes
      ; NOTE: Route order matters! Only check the /:user-id route after others
      ; have failed to match.
      (GET* "/lookup" request
            :return User
            :query-params [{user-id :- Long nil}
                           {username :- String nil}
                           {email :- String nil}]
            :summary "One of: user-id, username, password."
            (let [current (:identity (:session request))
                  criterion (cond
                              (not (nil? user-id)) {:user-id user-id}
                              (not (nil? username)) {:username username}
                              (not (nil? email)) {:email email})
                  user (user/lookup criterion)]
              (cond
                ; if no criteria, 400
                (nil? criterion)
                (bad-request "Invalid request. Must supply one of the following
                             querystring parameters: id, username, email.")

                ; if no user found, or user is inactive and no login, 404
                (or (nil? user)
                    (and (not (:active user)) (nil? current)))
                (not-found "No user matched your request.")

                ; if user is active, or current login is admin, 200
                (or (:active user)
                    (>= (:user-level current) (:admin user/user-levels)))
                (ok (user/public user)))))

      (GET* "/current" request
            :return User
            (if-let [user (get-in request [:session :identity])]
              (ok (user/private user))
              (not-found)))

      (GET* "/:user-id" request
            :return User
            :path-params [user-id :- Long]
            :summary "Numeric user id."
            (let [current (:identity (:session request))
                  user (user/lookup {:user-id user-id})]
              (cond
                ; if no user found, or user is inactive and no login, 404
                (or (nil? user)
                    (and (not (:active user)) (nil? current)))
                (not-found "No user matched your request.")

                ; if user is active, or current login is admin, 200
                (or (:active user)
                    (>= (:user-level current) (:admin user/user-levels)))
                (ok (user/public user)))))

      ; create
      (POST* "/" request
             :body-params [username :- String
                           password :- String
                           name :- String
                           {profile :- String nil}
                           email :- String]
             :return User
             :summary "JSON body representing initial user information."
             ;; Return private representation of the user, with Location header.
             (cond
               ; if username is not available, 400
               (not (user/username-available? username))
               (bad-request (format "username %s is not available" username))
               (not (user/email-available? email))
               (bad-request (format "email %s is not available" email))
               :else
               (let [user (user/create! (:body-params request))
                     location (format "/api/users/%d" (:user-id user))
                     response (created (user/private user))]
                 (assoc-in response [:headers "Location"] location))))

      ; update
      (PUT* "/:user-id" request
            :path-params [user-id :- Long]
            :body [incoming User] ; desired values
            :return User
            :summary "JSON body representing desired user information."
            ;; Return private representation of the updated user.
            (let [current (:identity (:session request))
                  target (user/lookup {:user-id user-id})
                  desired (-> target
                            (merge incoming)
                            (assoc :user-id user-id))]
              (cond
                ; if nobody is logged in, 401
                (nil? current)
                (unauthorized "You must be logged in to update a user.")
                ; if target is not found, 404
                (nil? target)
                (not-found (format "No user with id %d exists." user-id))
                ; if logged-in user has insufficient permissions, 403
                (not (user/can-update-user? current target))
                (forbidden "If you do not have admin privileges, you can only
                           update yourself.")
                ; if username is being changed to an unavilable one, 400
                (not (or (= (:username incoming) (:username target))
                         (user/username-available? (:username incoming))))
                (bad-request (format "username %s is not available" (:username incoming)))
                ; if email is being changed to an unavilable one, 400
                (not (or (= (:email incoming) (:email target))
                         (user/email-available? (:email incoming))))
                (bad-request (format "email %s is not available" (:email incoming)))
                :else
                (if-let [user (user/update! desired)]
                  (ok (user/private user))
                  ; refine this message if common failure types emerge
                  (internal-server-error "The update could not be performed as requested.")))))

      ; remove
      (DELETE* "/:user-id" request
               :path-params [user-id :- Long]
               :summary "ID of the user to be removed."
               ;; Return 204 No Content response, or 401/403 as appropriate
               (let [current (:identity (:session request))
                     target (user/lookup {:user-id user-id})]
                 (cond
                   ; if nobody is logged in, 401
                   (nil? current)
                   (unauthorized "You must be logged in to remove a user.")
                   ; if target is not found, 404
                   (nil? target)
                   (not-found (format "No user with id %d existed in the first place." user-id))
                   ; if logged-in user has insufficient permissions, 403
                   (not (user/can-remove-user? current target))
                   (forbidden "You do not have permission to remove this user.")
                   :else
                   (if (user/remove! target)
                     (no-content) ; this is actually success: 204 No Content
                     (internal-server-error "The user could not be removed as requested.")))))

      ; login
      (POST* "/login" request
             :return User
             :body-params [{email :- String ""}
                           {username :- String ""}
                           password :- String]
             :summary "Username or email, and unhashed password."
             (let [user (user/lookup {:email email, :username username})]
               (if (user/valid-password? user password)
                 (-> (ok (user/private user))
                   (assoc :session (assoc (:session request) :identity user)))
                 (unauthorized))))
      
      ; videos by user
      (GET* "/:user-id/videos" []
            :return [Video]
            :path-params [user-id :- Long]
            :summary "Numeric user id."
            ; Even if the result set is empty, we want to return a 200 OK
            ; response. Client should be ready for an empty list.
            (ok (video/find-by-user-id user-id))))
    
    (context* "/videos" []
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
             ;; Return private representation of the video, with Location header.
             (let [video (video/create! (:body-params request))
                   location (format "/api/videos/%d" (:video-id video))
                   response (created video)]
               (assoc-in response [:headers "Location"] location))))))
