(ns raiseyourgame.subscriptions
  (:require [re-frame.core :refer [register-sub]])
  (:require-macros [reagent.ratom :refer [reaction]]))

;; Given a query function, returns a function which expects the app db and a
;; re-frame subscription values vector [sid & args], and returns a reaction.
;;
;; This helper function makes it more efficient to register subscriptions in
;; terms of pure query functions.
(defn- reactionize [f]
  (fn [db values]
    (reaction (f @db values))))

(defn current-user-query [db _]
  (:current-user db))
(register-sub :current-user-query (reactionize current-user-query))

(defn form-values-query [db [_ form-id]]
  (get-in db [:forms form-id :values]))
(register-sub :form-values-query (reactionize form-values-query))

(defn form-errors-query [db [_ form-id]]
  (get-in db [:forms form-id :errors]))
(register-sub :form-errors-query (reactionize form-errors-query))

(defn target-type-query [db _]
  (get-in db [:target :type]))
(register-sub :target-type-query (reactionize target-type-query))

(defn users-query [db _]
  (get-in db [:target :users]))
(register-sub :users-query (reactionize users-query))
