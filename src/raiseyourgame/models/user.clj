(ns raiseyourgame.models.user
  "Namespace containing database and domain logic for user maps."
  (:require [raiseyourgame.db.core :as db]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [bugsbio.squirrel :refer [to-sql to-clj]]
            [cheshire.core :as cheshire]
            [buddy.hashers :as hashers]
            [taoensso.timbre :refer [debug]])
  (:import java.sql.SQLException))

;; Note that the raiseyourgame.db.core namespace deals in YeSQL queries, which
;; require SQL-style snake_case params. Use to-sql for those.

(defn public
  "Returns a user map suitable for public display via the API."
  [user]
  (dissoc user :password :email))

(defn private
  "Returns a user map suitable for private view by the user and admins."
  [user]
  (dissoc user :password))

;; Unlike others, the lookup function of each model returns a single element.
(defn lookup
  "Given a map containing at least one of int :user-id, string :username, or
  string :email, returns the matching user, or nil. Keys are checked and used
  in that order."
  [{:keys [user-id username email]}]
  (let [result-set
        (cond (not (nil? user-id)) (db/find-users-by-user-id (to-sql {:user-id user-id}))
              (not (nil? username)) (db/find-users-by-username {:username username})
              (not (nil? email)) (db/find-users-by-email {:email email})
              :default '())]
    (when-not (empty? result-set)
      (to-clj (first result-set)))))

(defn valid-password?
  "True if the supplied password is correct."
  [user password]
  (and user password (hashers/check password (:password user))))

(defn can-update-user?
  "True if the supplied user has permission to update the target user. Users
  may only update themselves, or users with a lower user-level."
  [user target]
  (or (= user target)
      (> (:user-level user) (:user-level target))))

(defn username-available?
  "True if the supplied username is not already in use."
  [username]
  (nil? (lookup {:username username})))

(defn email-available?
  "True if the supplied email is not already in use."
  [email]
  (nil? (lookup {:email email})))

(defn create!
  "Creates a user based on a params object containing the following keys:
  :username, :email, :password, :name (optional), :profile (optional). The
  password will be stored hashed in the database; the original is discarded.
  Users are created with user_level 0; moderators and admins must be promoted
  after creation.

  On success, returns the newly created user. On failure, returns nil. One
  likely reason for failure is that a username or email has already been used;
  please check this condition using can-create-user? before attempting to
  create a user."
  [{:keys [password username] :as params}]
  (let [hashed-password (hashers/encrypt password)
        details (assoc params :password hashed-password)]
    (try
      (-> details
        (to-sql)
        (db/create-user<! @db/conn)
        (to-clj))
      (catch SQLException e nil))))

(defn update!
  "Given a user model map, updates the database row with that id using those
  values. If the supplied password does not match the current hashed password,
  the incoming password will be hashed and stored. Fails when attempting to
  change username or email to an unavailable value.

  (let [updated-user (assoc user :username 'Ann')]
  update! updated-user)

  Given a user model map and a transition function, applies the function to the
  map and updates the user in the database.

  (update! user #(assoc :username 'Ann'))

  Given a user model map, a transition function, and a variable number of
  arguments, applies the function to the map, with the additional arguments,
  and updates the user in the database. For simpler updates, this is more
  convenient than providing a transition function.

  (update! user assoc :username 'Ann')

  In all cases, returns the updated user if successful, nil otherwise.

  If an incomplete user map is supplied, the resulting SQLException will also
  cause this function to return nil; don't do that."
  ;; NOTE: If the issue comes up, we can always raise an uncaught exception.
  ([user]
   (try
     (let [original (lookup user)
           ; this awkward construction hashes the password if new
           user (if (not= (:password original) (:password user))
                  (update-in user [:password] hashers/encrypt)
                  user)
           result (db/update-user! (to-sql user))]
       ; result will be the rows affected
       (if (< 0 result) user nil))
     (catch SQLException e nil)))
  ([user f]
   (update! (f user)))
  ([user f & args]
   (update! (apply (partial f user) args))))
