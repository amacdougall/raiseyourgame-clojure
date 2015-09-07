(ns raiseyourgame.test.users
  (:require [raiseyourgame.db.core :as db]
            [clojure.test :refer :all] ))

(use-fixtures
  :once
  (fn [f]
    ;; add users to database? other test setup?
    (f)))
