(ns raiseyourgame.subscriptions
  (:require [re-frame.core :refer [register-sub]])
  (:require-macros [reagent.ratom :refer [reaction]]))

(register-sub
  :current-user-query
  (fn [db _]
    (reaction (:current-user @db))))

(register-sub
  :login-credentials-query
  (fn [db _]
    (reaction (:login-credentials @db))))

(register-sub
  :login-errors-query
  (fn [db _]
    (reaction (:login-errors @db))))

(register-sub
  :target-type-query
  (fn [db _]
    (reaction (get-in @db [:target :type]))))

(register-sub
  :users-query
  (fn [db _]
    (reaction (get-in @db [:target :users]))))
