(ns raiseyourgame.routes
  (:require [re-frame.core :refer [dispatch]]
            [secretary.core :as secretary :refer-macros [defroute]]))

(defroute home "/admin" []
  (dispatch [:display-home]))

(defroute users "/admin/users" []
  (dispatch [:display-user-list]))
