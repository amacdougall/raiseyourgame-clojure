(ns raiseyourgame.models.user
  "Namespace containing database and domain logic for user maps."
  (:require [raiseyourgame.db.core :as db]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [bugsbio.squirrel :refer [to-sql to-clj]]
            [cheshire.core :as cheshire]
            [buddy.hashers :as hashers]
            [taoensso.timbre :refer [debug]])
  (:import java.sql.SQLException))

(def user-levels {:user 0, :moderator 1, :admin 2})

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

(defn is-moderator?
  "True if the supplied user has moderator privileges."
  [user]
  (and user
       (>= (:user-level user) (:moderator user-levels))))

(defn is-admin?
  "True if the supplied user has admin privileges."
  [user]
  (and user
       (>= (:user-level user) (:admin user-levels))))

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

(defn get-users
  "Returns a vec of users. Takes an options hash with the following keys:

  :per-page - The number of users to return. Default 30.
  :page - The page at which to begin. Acts as multiplier of :per-page, so if
  per-page is 20 and page is 3, the returned vec will skip the first 40
  results and return the next 20. Default 1."
  ([] (get-users {})) ;; handle zero-arg call by passing an empty hash
  ([{:keys [per-page page]
     :or {per-page 30, page 1}}]
   (let [result-set (db/get-users (to-sql {:offset (* per-page (- page 1))
                                           :limit per-page}))]
     (when-not (empty? result-set)
       (map to-clj result-set)))))

(defn valid-password?
  "True if the supplied password is correct."
  [user password]
  (and user password (hashers/check password (:password user))))

(defn can-view-user?
  "True if the supplied user has permission to view the target user. Active
  users can always be viewed; inactive users can be viewed only by admins."
  [user target]
  (or (:active target)
      (is-admin? user)))

(defn can-view-private-data?
  "True if the supplied user has permission to view the target's non-public
  information, such as email. Always false when user is nil."
  [user target]
  (and user
       (or (= user target)
           (is-admin? user)
           (> (:user-level user) (:user-level target)))))

(defn can-update-user?
  "True if the supplied user has permission to update the target user. Always
  false when user is nil."
  [user target]
  (and user
       (or (= user target)
           (is-admin? user)
           (> (:user-level user) (:user-level target)))))

(defn can-remove-user?
  "Given a user and a target, returns true if the user has permission to remove
  the target. Always false when user is nil."
  [user target]
  (and (not (= user target)) ; do not let users delete themselves
       (is-admin? user)))

(defn can-view-video?
  "Given a user and a video, returns true if the user has permission to view
  the video. Anonymous and standard users may view videos that have no
  restrictions; only the owner, mods, and admins may view videos that are
  locked or drafts. If user is nil, true only for unrestricted videos."
  [user video]
  (cond
    ; if video is inactive, only permit admins to view
    (not (:active video))
    (is-admin? user)
    ; if video is locked or is a draft, only permit admins and mods to view
    (or (:draft video) (:locked video))
    (or (= (:user-id user) (:user-id video))
        (is-moderator? user))
    ; if not inactive, locked, or draft, return true
    :else true))

(defn can-update-video?
  "Given a user and a video, returns true if the user has permission to update
  the video."
  [user video]
  (or (= (:user-id user) (:user-id video))
      (is-admin? user)))

(defn can-remove-video?
  "Given a user and a video, returns true if the user has permission to remove
  the video."
  [user video]
  (or (= (:user-id user) (:user-id video))
      (is-admin? user)))

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

(defn remove!
  "Removes the supplied user by setting the user's :active property to false.
  This should have the effect of making the user invisible to the public API,
  and therefore to the website, apps, etc; but it is the responsibility of API
  handlers to enforce this property.

  As with update!, returns the removed user."
  [user]
  (update! user assoc :active false))
