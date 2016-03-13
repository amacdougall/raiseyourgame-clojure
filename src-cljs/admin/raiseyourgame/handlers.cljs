(ns raiseyourgame.handlers
  (:require [raiseyourgame.remote :as remote]
            [raiseyourgame.db :refer [initial-state]]
            [re-frame.core :refer [register-handler]]))

;; Handler run at app startup. Loads current user, if any. Resets the db to
;; db/initial-state.
(register-handler
  :initialize
  (fn [db]
    (remote/load-current-user)
    initial-state))

(register-handler
  :current-user-loaded
  (fn [db [_ current-user]]
    (assoc db :current-user current-user)))

(register-handler
  :display-home
  (fn [db]
    (assoc db :target nil)))

(register-handler
  :display-user-list
  (fn [db]
    (remote/load-users)
    (assoc db :target {:type :users, :users nil})))

(register-handler
  :users-loaded
  (fn [db [_ users]]
    (assoc db :target {:type :users, :users users})))
