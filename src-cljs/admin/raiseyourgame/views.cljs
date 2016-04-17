(ns raiseyourgame.views
  (:require [raiseyourgame.routes :as routes]
            [re-frame.core :refer [subscribe dispatch]]
            [re-com.core :as rc]))

(defn status-bar [current-user]
  [:div {:class "status-bar"} ; TODO: BEMify
   (if current-user
     [:div
      [:div "Logged in as " [:a {:href "http://www.example.com"} (:username current-user)]]
      [:div [:a {:href (routes/logout)} "Log out"]]]
     [:div [:a {:href "http://www.example.com"} "Log in"]])])

(defn login []
  (let [values (subscribe [:form-values-query :login])
        errors (subscribe [:form-errors-query :login])]
    (fn []
      [:div
       [:h1 "Log in here."]
       [:form
        [:label {:for :username} "User"]
        [rc/input-text
         :attr {:id :username, :name :username}
         :model (or (:username @values) "")
         :placeholder "Username or email"
         :change-on-blur? false
         :on-change #(dispatch [:update-form-value :login :username %])]
        [:label {:for :password} "Password"]
        [rc/input-text
         :attr {:id :password, :name :password, :type :password}
         :model (or (:password @values) "")
         :placeholder ""
         :change-on-blur? false
         :on-change #(dispatch [:update-form-value :login :password %])]
        (when (:login-failed? @errors)
          [:div "Error! This login totes failed."])
        [rc/button
         :label "Log In"
         :on-click (fn [event]
                     (.preventDefault event)
                     (dispatch [:login-form-submit]))]]])))

(defn home []
  [:div "Home stuff."])

(defn users []
  (let [user-list (subscribe [:users-query])
        render-user #([:div [:p (str "User name: " (:username %))]])]
    (fn []
      (into [:div
             [:h1 "User list"]]
            (map render-user @user-list)))))

(defn main []
  (let [target-type (subscribe [:target-type-query])
        current-user (subscribe [:current-user-query])]
    (fn []
      [:div {:class "main"}
       (status-bar @current-user)
       [:div {:class "nav"}
        [:div [:a {:href (routes/home)} "Home"]]
        [:div [:a {:href (routes/users)} "Users"]]
        [:div [:a "Videos"]]]
       (if (nil? @current-user)
         [login]
         (condp = @target-type
           nil    [home]
           :users [users]))])))
