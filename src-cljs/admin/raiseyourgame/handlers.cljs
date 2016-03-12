(ns raiseyourgame.handlers
  (:require [raiseyourgame.remote :as remote]
            [re-frame.core :refer [register-handler]]))

(defn- clear-target [db]
  (assoc db :target nil))

(register-handler
  :initialize-db
  clear-target)

(register-handler
  :display-home
  clear-target)

(register-handler
  :display-user-list
  (fn [db]
    (remote/load-users)
    (assoc db :target {:type :users, :users nil})))

(register-handler
  :users-loaded
  (fn [db [_ users]]
    (assoc db :target {:type :users, :users users})))
