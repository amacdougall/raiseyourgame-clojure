(ns raiseyourgame.models.user
  (:require [raiseyourgame.db.core :as db]
            [camel-snake-kebab.core :refer [->snake_case_keyword ->snake_case]]
            [cheshire.core :as cheshire]
            [buddy.hashers :as hashers])
  (:import java.sql.SQLException))

(defn create-user!
  "Creates a user based on a params object containing the following keys:
  :username, :email, :password, :name (optional), :profile (optional). The
  password will be stored hashed in the database; the original is discarded.
  Users are created with user_level 0; moderators and admins must be promoted
  after creation.

  On success, returns the newly created user. On failure, returns nil. One
  likely reason for failure is that a username or email has already been used;
  please check this condition before attempting to create a user."
  [{:keys [password username] :as params}]
  (let [hashed-password (hashers/encrypt password)
        details (assoc params :password hashed-password :user_level 0)]
    (try
      (db/create-user<! details @db/conn)
      (catch SQLException e nil))))

(defn lookup
  "Given a map containing at least one of int :id, string :email, or string
  :username, returns the matching user, or nil. Keys are checked and used in
  that order."
  [{:keys [id email username]}]
  (first (cond (not (nil? id)) (db/get-user-by-id {:id id})
               (seq email) (db/get-user-by-email {:email email})
               (seq username) (db/get-user-by-username {:username username}))))

(defn valid-password?
  "True if the supplied password is correct."
  [user password]
  (and user password (hashers/check password (:password user))))

(defn json->user
  "Given a JSON string, return a user with keyword keys."
  [raw-json]
  (cheshire/parse-string raw-json ->snake_case_keyword))
