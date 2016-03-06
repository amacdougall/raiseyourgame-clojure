(ns raiseyourgame.subscriptions
  (:require [re-frame.core :refer [register-sub]])
  (:require-macros [reagent.ratom :refer [reaction]]))

(register-sub
  :target-type-query
  (fn [db _]
    (reaction (get-in @db [:target :type]))))

(register-sub
  :users-query
  (fn [db _]
    (reaction (get-in @db [:target :users]))))