(ns raiseyourgame.routes.api
  (:require [raiseyourgame.db.core :as db]
            [raiseyourgame.models.user :as user]
            [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [taoensso.timbre :refer [debug]]))

(s/defschema User {:id Long
                   :username String
                   (s/optional-key :email) String
                   :name String
                   :profile String
                   :user-level Long
                   :created-at java.util.Date
                   :updated-at java.util.Date
                   :last-login (s/maybe java.util.Date)})

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
                 (unauthorized))))

      (GET* "/current" request
            :return User
            (if-let [user (get-in request [:session :identity])]
              (ok (dissoc user :password))
              (not-found))))))
