(ns raiseyourgame.views
  (:require [raiseyourgame.routes :as routes]
            [re-frame.core :refer [subscribe dispatch]]
            [re-com.core :as rc]))

(defn login-view []
  (let [credentials (subscribe [:login-credentials-query])
        errors (subscribe [:login-errors-query])
        values (atom {})]
    (fn []
      [:div
       [:h1 "Log in here."]
       [:form
        [:label {:for :username} "User"]
        ; TODO: also populate local values on Chrome autofill!
        [rc/input-text
         :attr {:id :username, :name :username}
         :model (or (:username @credentials) "")
         :placeholder "Username or email"
         :on-change (fn [s]
                      (swap! values #(assoc % :username s)))]
        [:label {:for :password} "Password"]
        [rc/input-text
         :attr {:id :password, :name :password, :type :password}
         :model (or (:password @credentials) "")
         :placeholder ""
         :on-change (fn [s]
                      (swap! values #(assoc % :password s)))]
        [rc/button
         :label "Log In"
         :on-click (fn [event]
                     (.preventDefault event)
                     (dispatch [:login (:username @values) (:password @values)]))]]])))

(defn home-view []
  [:div "Home stuff."])

(defn users-view []
  (let [users (subscribe [:users-query])
        render-user
        (fn [user]
          [:div [:p (str "User name: " (:username user))]])]
    (fn []
      (into [:div
             [:h1 "User list"]]
            (map render-user @users)))))

(defn main-view []
  (let [target-type (subscribe [:target-type-query])
        current-user (subscribe [:current-user-query])]
    (fn []
      [:div {:class "main"}
       [:div {:class "nav"}
        [:div [:a {:href (routes/home)} "Home"]]
        [:div [:a {:href (routes/users)} "Users"]]
        [:div [:a "Videos"]]]
       (if (nil? @current-user)
         [login-view]
         (condp = @target-type
           nil    [home-view]
           :users [users-view]))])))
