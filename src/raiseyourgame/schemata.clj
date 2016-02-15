(ns raiseyourgame.schemata
  (:require [schema.core :as s]))

(s/defschema User
  {:user-id Long
   :active Boolean
   :username String
   ; password may be used in update requests... but nowhere else.
   (s/optional-key :password) String
   (s/optional-key :email) String
   :name String
   :profile String
   :user-level Long
   :created-at java.util.Date
   :updated-at java.util.Date
   :last-login (s/maybe java.util.Date)})

(s/defschema Video
  {:video-id Long
   :user-id Long
   :active Boolean
   :url String
   :length (s/maybe Long)
   :title String
   :blurb (s/maybe String)
   :description (s/maybe String)
   :times-started Long
   :times-completed Long
   :times-upvoted Long
   :created-at java.util.Date
   :updated-at java.util.Date})
