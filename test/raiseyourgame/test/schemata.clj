(ns raiseyourgame.test.schemata
  "Schemata used in the generation of test data."
  (:require [schema.core :as s]))

;; A schema to generate hashes for user creation requests.
(s/defschema NewUser
  {:username (s/constrained String #(not (empty? %)))
   :password (s/constrained String #(not (empty? %)))
   :email (s/constrained String #(not (empty? %)))
   :name String
   :profile String})

;; A schema to generate hashes for video creation requests.
(s/defschema NewVideo
  {:user-id Long
   :url String
   :length Long
   :title String
   :blurb String
   :description String})
