(ns raiseyourgame.test.schemata
  "Schemata used in the generation of test data."
  (:require [schema.core :refer :all]))

;; A schema to generate hashes for user creation requests.
(defschema NewUser
  {:username (constrained String #(not (empty? %)))
   :password (constrained String #(not (empty? %)))
   :email (constrained String #(not (empty? %)))
   :name String
   :profile String})

;; A schema to generate hashes for video creation requests.
(defschema NewVideo
  {:user-id Long
   :url String
   :length Long
   :title String
   :blurb String
   :description String})
