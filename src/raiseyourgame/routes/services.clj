(ns raiseyourgame.routes.services
  (:require [raiseyourgame.db.core :as db]
            [raiseyourgame.models.user :as user]
            [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [taoensso.timbre :refer [debug]]))

(s/defschema User {:id Long
                   :username String
                   :email (s/maybe String)
                   :name String
                   :profile String
                   :user_level Long
                   :created_at java.util.Date
                   :updated_at java.util.Date
                   :last_login (s/maybe java.util.Date)})

(defn- safe-user [user]
  (-> user
    (dissoc :password)
    (assoc :email nil)))

(defapi service-routes
  (ring.swagger.ui/swagger-ui
    "/swagger-ui")
  ;JSON docs available at the /swagger.json route
  (swagger-docs
    {:info {:title "Raise Your API"}})

  (context* "/api" []
    (context* "/users" []
      :tags ["users"]

      (GET* "/lookup" []
            :return User
            :query-params [{id :- Long nil}
                           {username :- String nil}
                           {email :- String nil}]
            :summary ""
            (let [criterion (cond
                              (not (nil? id)) {:id id}
                              (not (nil? username)) {:username username}
                              (not (nil? email)) {:email email})]
              (if criterion
                (if-let [user (user/lookup criterion)]
                  (ok (safe-user user))
                  (not-found "No user matched your request."))
                (bad-request "Invalid request. Must supply one of the following
                             querystring parameters: id, username, email."))))

      (GET* "/available/:username" []
            :return Boolean
            :path-params [username :- String]
            :summary "true if supplied username is available."
            (ok (empty? (db/get-user-by-username {:username username}))))

      (POST* "/login" req
             :return User
             :body-params [{email :- String ""}
                           {username :- String ""}
                           password :- String]
             :summary "Username or email, and unhashed password."
             (let [user (user/lookup {:email email, :username username})]
               (if (user/valid-password? user password)
                 (-> (ok (dissoc user :password))
                   (assoc :session (assoc (:session req) :identity user)))
                 (unauthorized)))))))
