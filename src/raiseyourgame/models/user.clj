(ns raiseyourgame.models.user
  (:require [raiseyourgame.db.core :as db]
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
      (when (= 1 (db/create-user! details @db/conn))
        (first (db/get-user-by-username {:username username})))
      (catch SQLException e nil))))

(defn lookup
  "Given a map containing at least one of :id, :email, or :username, returns
  the matching user, or nil. Keys are preferred in that order."
  [{:keys [id email username]}]
  (let [exists? (complement empty?)]
  (first (cond (exists? id) (db/get-user-by-id {:id id})
               (exists? email) (db/get-user-by-email {:email email})
               (exists? username) (db/get-user-by-username {:username username})))))

(defn valid-password?
  "True if the supplied password is correct."
  [user password]
  (and user password (hashers/check password (:password user))))
