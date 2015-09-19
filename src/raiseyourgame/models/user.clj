(ns raiseyourgame.models.user
  (:require [raiseyourgame.db.core :as db]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [bugsbio.squirrel :refer [to-sql to-clj]]
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
      (-> details
        (to-sql)
        (db/create-user<! @db/conn)
        (to-clj))
      (catch SQLException e nil))))

(defn lookup
  "Given a map containing at least one of int :id, string :username, or string
  :email, returns the matching user, or nil. Keys are checked and used in that
  order."
  [{:keys [id username email]}]
  (let [result-set
        (cond (not (nil? id)) (db/get-user-by-id {:id id})
              (not (nil? username)) (db/get-user-by-username {:username username})
              (not (nil? email)) (db/get-user-by-email {:email email})
              :default '())]
    (when-not (empty? result-set)
      (to-clj (first result-set)))))

(defn valid-password?
  "True if the supplied password is correct."
  [user password]
  (and user password (hashers/check password (:password user))))

(defn json->user
  "Given a JSON string, return a user."
  [raw-json]
  (cheshire/parse-string raw-json ->kebab-case-keyword))
