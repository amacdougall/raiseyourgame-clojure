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
