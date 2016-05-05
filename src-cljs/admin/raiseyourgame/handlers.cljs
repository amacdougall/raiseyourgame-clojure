;; This namespace contains re-frame handler functions; when evaluated, it also
;; registers each function. The function names precisely match the event names.
;;
;; The defn ... register-handler convention in this file may seem awkward at
;; first, but allowing external access to named handler functions makes unit
;; testing easy.
(ns raiseyourgame.handlers
  (:require [raiseyourgame.remote :as remote]
            [raiseyourgame.db :refer [initial-state]]
            [re-frame.core :refer [register-handler dispatch]]
            [com.rpl.specter :as s]))

;; Handler run at app startup. Loads current user, if any. Resets the db to
;; db/initial-state.
(defn initialize [db]
  (remote/load-current-user)
  initial-state)
(register-handler :initialize initialize)

(defn current-user-loaded [db [_ current-user]]
  (assoc db :current-user current-user))
(register-handler :current-user-loaded current-user-loaded)

(defn update-form-value [db [_ form-id k v]]
  (assoc-in db [:forms form-id :values k] v))
(register-handler :update-form-value update-form-value)

(defn update-form-error [db [_ form-id k v]]
  (assoc-in db [:forms form-id :errors k] v))
(register-handler :update-form-error update-form-error)

(defn login-form-submit [db _]
  (-> db
    (get-in [:forms :login :values])
    (select-keys [:username :password])
    (remote/login))
  db)
(register-handler :login-form-submit login-form-submit)

(defn login-successful [db [_ user]]
  (->> db
    (s/setval [:current-user] user)
    (s/transform [:forms] #(dissoc % :login))))
(register-handler :login-successful login-successful)

;; Handle login error by adding an error flag to the login form data.
(defn login-error [db [_ error]]
  (condp = (:status error)
    400 (dispatch [:update-form-error :login :bad-data? true])
    401 (dispatch [:update-form-error :login :login-failed? true])
    500 (dispatch [:update-form-error :login :system-error? true]))
  db)
(register-handler :login-error login-error)

;; Handle logout by making a logout API request.
(defn logout [db _]
  (remote/logout)
  db)
(register-handler :logout logout)

;; Handle logout success by clearing the current user from the app db.
(defn logout-successful [db]
  (dispatch [:display-home])
  (assoc db :current-user nil))
(register-handler :logout-successful logout-successful)

(defn logout-failed [db error]
  (.log js/console "Logout failed! Error: %o" error))
(register-handler :logout-failed logout-failed)

(defn display-home [db]
  (assoc db :target nil))
(register-handler :display-home display-home)

(defn display-user-list [db]
  (remote/load-users)
  (assoc db :target {:type :users, :users nil}))
(register-handler :display-user-list display-user-list)

(defn users-loaded [db [_ users]]
  (assoc db :target {:type :users, :users users}))
(register-handler :users-loaded users-loaded)
