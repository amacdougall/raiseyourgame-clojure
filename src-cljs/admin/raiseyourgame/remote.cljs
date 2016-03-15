(ns raiseyourgame.remote
  (:require [re-frame.core :refer [dispatch]]
            [ajax.core :refer [GET POST]]))

;; TODO: better error handling?
(defn- default-error-handler [error]
  (.log js/console "%s %s: %s"
        (:status error)
        (:status-text error)
        (:response error)))

(defn load-current-user
  "Loads the currently logged-in user. Dispatches :current-user-loaded with the
  result, which will be either a user map or nil."
  []
  (GET "/api/users/current"
       {:response-format :transit
        :handler
        (fn [user]
          (dispatch [:current-user-loaded user]))
        :error-handler
        (fn [error]
          (if (= (:status error) 404)
            (dispatch [:current-user-loaded nil])
            (default-error-handler error)))}))

(defn login
  "Given credentials map with :username and :password values, attempts to log
  in. Dispatches :login-successful on success; :login-error otherwise."
  [credentials]
  (POST "/api/users/login"
        {:format :transit
         :response-format :transit
         :headers {"Content-Type" "application/transit+json"}
         :params credentials
         :handler #(dispatch [:login-successful %])
         :error-handler #(dispatch [:login-error %])}))

(defn load-users []
  (GET "/api/users"
       {:response-format :transit
        :handler
        (fn [{:keys [page per-page users]}]
          (dispatch [:users-loaded users]))
        :error-handler default-error-handler}))
