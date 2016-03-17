(ns raiseyourgame.handlers
  (:require [raiseyourgame.remote :as remote]
            [raiseyourgame.db :refer [initial-state]]
            [re-frame.core :refer [register-handler dispatch]]
            [com.rpl.specter :as s]))

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
  :update-form-value
  (fn [db [_ form-id k v]]
    (assoc-in db [:forms form-id :values k] v)))

(register-handler
  :update-form-error
  (fn [db [_ form-id k v]]
    (assoc-in db [:forms form-id :errors k] v)))

(register-handler
  :login
  (fn [db _]
    (-> db
      (get-in [:forms :login :values])
      (select-keys [:username :password])
      (remote/login))
    db))

(register-handler
  :login-successful
  (fn [db [_ user]]
    (->> db
      (s/setval [:current-user] user)
      (s/transform [:forms] #(dissoc % :login)))))

(register-handler
  :login-error
  (fn [db [_ error]]
    (.log js/console "Login error: %o" error)
    (condp = (:status error)
      400 (dispatch [:update-form-error :login :bad-data? true])
      401 (dispatch [:update-form-error :login :login-failed? true])
      500 (dispatch [:update-form-error :login :system-error? true]))
    db))

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
