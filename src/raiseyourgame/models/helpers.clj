(ns raiseyourgame.models.helpers
  (:require [bugsbio.squirrel :refer [to-sql to-clj]]))

(defn result-set->clj [result-set]
  (when-not (empty? result-set)
    (to-clj result-set)))
