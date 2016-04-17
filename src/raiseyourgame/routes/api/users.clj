(ns raiseyourgame.routes.api.users
  (:require [raiseyourgame.db.core :as db]
            [raiseyourgame.schemata :refer :all]
            [raiseyourgame.models.user :as user]
            [raiseyourgame.models.video :as video]
            [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [taoensso.timbre :refer [debug]]))

;; Routes to be included in the "/api/users" context.
(defroutes users-routes
  ; NOTE: Route order matters! Only check the /:user-id route after others
  ; have failed to match.
  (GET "/" request
       :return {:page Long
                :per-page Long
                :users [User]}
       ; These defaults are repeated in raiseyourgame.models.user/get-users.
       ; I am not sure if this is a bad thing or not, but if it becomes a
       ; hassle, we can read both from some central namespace.
       :query-params [{page :- Long 1}
                      {per-page :- Long 30}
                      {order-by :- String "user-id"}
                      {sort-direction :- String "asc"}]
       :summary "Returns a user list in the specified form."
       (let [current (:identity (:session request))]
         (if-not (user/can-list-users? current)
           ; if current user does not have permission to view user lists, 403
           (forbidden "You do not have permission to view lists of users.")
           ; otherwise, look up users
           (let [apply-privacy
                 (fn [user]
                   (if (user/can-view-private-data? current user)
                     (user/private user)
                     (user/public user)))

                 users
                 (user/get-users {:page page
                                  :per-page per-page
                                  :order-by (keyword order-by)
                                  :sort-direction (keyword sort-direction)})]
             (if (empty? users)
               ; if no users were actually found, 404
               (not-found "This query did not return any users.")
               ; otherwise, return user list
               (ok {:page page
                    :per-page per-page
                    :users (map apply-privacy users)}))))))

  (GET "/lookup" request
       :return User
       :query-params [{user-id :- Long nil}
                      {username :- String nil}
                      {email :- String nil}]
       :summary "Returns a user based on the criterion supplied. Prefers
                user-id, then username, then email."
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

           ; if no user found, or current user does not have permission to
           ; view, return 404. We intentionally return 404 instead of 403 to
           ; hide the very existence of the resource from unprivileged users.
           (or (nil? user)
               (not (user/can-view-user? current user)))
           (not-found "No user matched your request.")

           :else
           (if (user/can-view-private-data? current user)
             (ok (user/private user))
             (ok (user/public user))))))

  (GET "/current" request
       :return User
       (if-let [current (:identity (:session request))]
         (ok (user/private current))
         (not-found)))

  (GET "/:user-id" request
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
           (if (and current (user/can-view-private-data? current user))
             (ok (user/private user))
             (ok (user/public user))))))

  ; create
  (POST "/" request
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
  (PUT "/:user-id" request
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
  (DELETE "/:user-id" request
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
  (POST "/login" request
        :return User
        :body-params [username :- String
                      password :- String]
        :summary "Username and unhashed password."
        (let [user (user/lookup {:username username})]
          (if (user/valid-password? user password)
            (-> (ok (user/private user))
              (assoc :session (assoc (:session request) :identity user)))
            (unauthorized))))

  (POST "/logout" request
        :summary "Logs out the current user, if any."
        (let [current (:identity (:session request))]
          (if current
            (-> (no-content)
              (assoc :session (assoc (:session request) :identity nil)))
            (not-found))))

  ; videos by user
  (GET "/:user-id/videos" []
       :return [Video]
       :path-params [user-id :- Long]
       :summary "Numeric user id."
       ; Even if the result set is empty, we want to return a 200 OK
       ; response. Client should be ready for an empty list.
       (ok (video/find-by-user-id user-id))))
