(ns raiseyourgame.test.fixtures
  "Namespace containing values and helper functions for test data setup.
  Functions which add values to the database should be run within a
  transaction and rolled back; nothing in this namespace provides for that.")

(def user-values
  {:username "tbogard"
   :password "buster wolf"
   :name "Terry Bogard"
   :profile "Are you okay?"
   :email "tbogard@hakkyokuseiken.org"
   :user-level 0})

(def moderator-values
  {:username "skusanagi"
   :password "eye of the metropolis"
   :name "Saishu Kusanagi"
   :profile "Yoasobi wa kiken ja zo."
   :email "skusanagi@magatama.org"
   :user-level 1})

;; User id must be merged in by test code.
(def video-values
  {:url "http://www.example.com/v/100"
   :title "Benimaru Nikkaido vs Goro Daimon"
   :blurb "Speed versus power in an epic clash!"
   :description "In round one, Benimaru keeps Daimon at range with normals,
                capitalizing on moments of inattention to deal damage with
                combos. Later, Daimon changes strategy to lure him out with
                great patience, and evens things up with deadly mixups."})
