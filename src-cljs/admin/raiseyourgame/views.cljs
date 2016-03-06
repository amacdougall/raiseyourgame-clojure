(ns raiseyourgame.views
  (:require [re-frame.core :refer [subscribe]]))

(defn home-view []
  [:div "Home stuff."])

(defn users-view []
  (let [users (subscribe [:users-query])
        render-user (fn render-user [user]
                      [:div [:p (str "User name: " (:username user))]])]
    (fn users-view-renderer []
      (into [:div
             [:h1 "User list"]]
            (map render-user @users)))))

(defn main-view []
  (let [target-type (subscribe [:target-type-query])]
    (fn main-view-renderer []
      [:div {:class "main"}
       [:div {:class "nav"}
        [:div "Home"]
        [:div "Users"]]
       (condp = @target-type
         nil
         [home-view]
         :users
         [users-view])])))