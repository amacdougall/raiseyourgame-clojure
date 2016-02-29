(ns raiseyourgame.views
  (:require [re-frame.core :refer [subscribe]]))

(defn users-view []
  (fn []
    (let [users (subscribe [:users-query])
          render-user (fn [user]
                        [:div [:p (str "User name: " (:username user))]])]
      (fn []
        (into [:div
               [:h1 "User list"]]
              (map render-user @users))))))
