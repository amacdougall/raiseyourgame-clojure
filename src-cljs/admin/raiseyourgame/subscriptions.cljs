(ns raiseyourgame.subscriptions
  (:require [re-frame.core :refer [register-sub]])
  (:require-macros [reagent.ratom :refer [reaction]]))

(register-sub
  :current-user-query
  (fn [db _]
    (reaction (:current-user @db))))

(register-sub
  :form-values-query
  (fn [db form-id]
    (reaction (get-in @db [form-id :values]))))

(register-sub
  :form-errors-query
  (fn [db form-id]
    (reaction (get-in @db [form-id :errors]))))

(register-sub
  :target-type-query
  (fn [db _]
    (reaction (get-in @db [:target :type]))))

(register-sub
  :users-query
  (fn [db _]
    (reaction (get-in @db [:target :users]))))
