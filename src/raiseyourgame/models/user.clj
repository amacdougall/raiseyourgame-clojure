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

(defn create!
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
        details (assoc params :password hashed-password)]
    (try
      (-> details
        (to-sql)
        (db/create-user<! @db/conn)
        (to-clj))
      (catch SQLException e nil))))

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

(defn update!
  "Given a user model map, updates the database row with that id using those
  values. If the supplied password does not match the current hashed password,
  the incoming password will be hashed and stored.

  (let [updated-user (assoc user :username 'Ann')]
    update! updated-user)

  Given a user model map and a transition function, applies the function to the
  map and updates the user in the database.

  (update! user #(assoc :username 'Ann'))

  In both cases, returns the updated user if successful, nil otherwise.
  
  If an incomplete user map is supplied, mayhem will ensue. Be ready to catch
  SQLExceptions if you're doing something innovative."
  ([user]
   (let [original (lookup user)
         ; this awkward construction hashes the password if new
         user (if (not= (:password original) (:password user))
                (update-in user [:password] hashers/encrypt)
                user)
         result (db/update-user! (to-sql user))]
     ; result will be the rows affected
     (if (< 0 result) user nil)))
  ([user f]
   (update! (f user))))

(defn valid-password?
  "True if the supplied password is correct."
  [user password]
  (and user password (hashers/check password (:password user))))
