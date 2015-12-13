(ns raiseyourgame.test.fixtures
  "Namespace containing values and helper functions for test data setup.
  Functions which add values to the database should be run within a
  transaction and rolled back; nothing in this namespace provides for that."
  (:require [raiseyourgame.models.user :as user]
            [raiseyourgame.models.video :as video]))

(def user-values
  {:username "tbogard"
   :password "buster wolf"
   :name "Terry Bogard"
   :profile "Are you okay?"
   :email "tbogard@hakkyokuseiken.org"})

(def user-values-two
  {:username "gdaimon"
   :password "heaven to hell drop"
   :name "Goro Daimon"
   :profile "Judo master"
   :email "gdaimon@judo.org"})

(def moderator-values
  {:username "skusanagi"
   :password "eye of the metropolis"
   :name "Saishu Kusanagi"
   :profile "Yoasobi wa kiken ja zo."
   :email "skusanagi@magatama.org"})

(def moderator-values-two
  {:username "kkapwhan"
   :password "phoenix flattener"
   :name "Kim Kaphwan"
   :profile "Aku wa urusan!"
   :email "kkaphwan@taekwondo.org"})

(def admin-values
  {:username "rbernstein"
   :password "genocide cutter"
   :name "Rugal Bernstein"
   :profile "Tournament host"
   :email "rbernstein@blacknoah.org"})

(def admin-values-two
  {:username "wkrauser"
   :password "kaiser wave"
   :name "Wolfgang Krauser"
   :profile "I will chisel your tombstone!"
   :email "wkrauser@kampfringen.org"})

;; User id must be merged in by test code.
(def video-values
  {:url "http://www.example.com/v/100"
   :title "Benimaru Nikkaido vs Goro Daimon"
   :blurb "Speed versus power in an epic clash!"
   :description "In round one, Benimaru keeps Daimon at range with normals, capitalizing on moments of inattention to deal damage with combos. Later, Daimon changes strategy to lure him out with great patience, and evens things up with deadly mixups."})

;; User ids and video ids must be merged in by test code.
(def annotation-values
  [{:timecode 1000
    :text "This annotation was made at 1000ms elapsed."}
   {:timecode 2000
    :text "This annotation was made at 2000ms elapsed."}
   {:timecode 3000
    :text "This annotation was made at 3000ms elapsed."}])

(defn create-test-user! []
  "Creates the user described in user-values at the standard user-level. Adds
  the user to the database and returns it."
  (user/create! user-values))

(defn create-test-user-two! []
  "Creates the user described in user-values-two at the standard
  user-level. Adds the user to the database and returns it."
  (user/create! user-values-two))

(defn create-test-moderator! []
  "Creates the user described in moderator-values at the moderator user-level. Adds
  the user to the database and returns it."
  (-> moderator-values
    (user/create!) ; all users are created with user-level 0
    (user/update! assoc :user-level (:moderator user/user-levels))))

(defn create-test-moderator-two! []
  "Creates the user described in moderator-values-two at the moderator user-level. Adds
  the user to the database and returns it."
  (-> moderator-values-two
    (user/create!) ; all users are created with user-level 0
    (user/update! assoc :user-level (:moderator user/user-levels))))

(defn create-test-admin! []
  "Creates the user described in admin-values at the admin user-level. Adds
  the user to the database and returns it."
  (-> admin-values
    (user/create!) ; all users are created with user-level 0
    (user/update! assoc :user-level (:admin user/user-levels))))

(defn create-test-admin-two! []
  "Creates the user described in admin-values-two at the admin user-level. Adds
  the user to the database and returns it."
  (-> admin-values-two
    (user/create!) ; all users are created with user-level 0
    (user/update! assoc :user-level (:admin user/user-levels))))

(defn create-test-video! []
  "Creates the video described in video-values, owned by the user described in
  user-values. Adds both to the database and returns them as a [video user]
  vector."
  (let [user (user/create! user-values)
        video (video/create! (assoc video-values :user-id (:user-id user)))]
    [video user]))
