(ns raiseyourgame.views
  (:require [raiseyourgame.routes :as routes :refer [href]]
            [re-frame.core :refer [subscribe]]))

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
        [:div [:a (href routes/home) "Home"]]
        [:div [:a (href routes/users) "Users"]]
        [:div [:a "Videos"]]]
       (condp = @target-type
         nil
         [home-view]
         :users
         [users-view])])))
