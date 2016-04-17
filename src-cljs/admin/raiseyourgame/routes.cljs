(ns raiseyourgame.routes
  (:require [re-frame.core :as re-frame]
            [secretary.core :as secretary :refer-macros [defroute]]))

(defroute home "/admin" []
  (re-frame/dispatch [:display-home]))

(defroute users "/admin/users" []
  (re-frame/dispatch [:display-user-list]))

(defroute logout "/admin/logout" []
  (re-frame/dispatch [:logout]))
