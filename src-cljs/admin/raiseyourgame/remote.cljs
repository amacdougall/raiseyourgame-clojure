(ns raiseyourgame.remote
  (:require [re-frame.core :refer [dispatch]]
            [ajax.core :refer [GET POST]]))

(defn load-users []
  (GET "/api/users"
       {:response-format :transit
        :handler
        (fn [{:keys [page per-page users]}]
          (dispatch [:users-loaded users]))
        :error-handler
        (fn [error]
          ; TODO: better error handling overall. Middleware?
          (.log js/console "%s %s: %s"
                (:status error)
                (:status-text error)
                (:response error)))}))
