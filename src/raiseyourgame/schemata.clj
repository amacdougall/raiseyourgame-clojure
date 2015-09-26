(ns raiseyourgame.schemata
  (:require [schema.core :refer :all]))

(defschema User
  {:user-id Long
   :active Boolean
   :username String
   (optional-key :email) String
   :name String
   :profile String
   :user-level Long
   :created-at java.util.Date
   :updated-at java.util.Date
   :last-login (maybe java.util.Date)})

(defschema Video
  {:video-id Long
   :user-id Long
   :active Boolean
   :url String
   :length (maybe Long)
   :title String
   :blurb (maybe String)
   :description (maybe String)
   :times-started Long
   :times-completed Long
   :times-upvoted Long
   :created-at java.util.Date
   :updated-at java.util.Date})
