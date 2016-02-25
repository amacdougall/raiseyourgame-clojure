(ns raiseyourgame.core
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [register-handler dispatch dispatch-sync register-sub subscribe]]
            [secretary.core :as secretary]
            [ajax.core :refer [GET POST]])
  (:require-macros [reagent.ratom :refer [reaction]]
                   [secretary.core :refer [defroute]])
  (:import [goog History]
           [goog.history EventType]))

(def app-db (atom {:target nil}))

(defn load-users []
  (GET "/api/users"
       {:handler
        (fn [users]
          ; TODO: convert back to keyword form (actually just use Transit/EDN)
          ; TODO: this is in {:page n, :per-page m, :users coll}, remember?
          ; So extract the users.
          (dispatch [:users-loaded users]))
        :error-handler
        (fn [error]
          ; TODO: better error handling overall. Middleware?
          (.log js/console "%s %s: %s"
                (:status error)
                (:status-text error)
                (:response error)))}))

(defn display-user-list [db]
  (load-users)
  (assoc db :target {:type :users, :users nil}))

(register-handler
  :display-user-list
  display-user-list)

(defn handle-users-loaded [db users]
  (.log js/console "(handle-users-loaded)")
  (assoc db :target {:type :users, :users users}))

(register-handler
  :users-loaded
  handle-users-loaded)

(defn users-query
  [db, [sid cid]]
  (.log js/console "(users-query)")
  (assert (= sid :users-query))
  (reaction (get-in @db [:target :users])))

(register-sub :users-query users-query)

(defn users-view []
  (fn []
    (let [users (subscribe [:users-query])
          render-user (fn [user]
                        [:div [:p (str "User name: " (:username user))]])]
      (fn []
        (into [:div
               [:h1 "User list"]]
              (map render-user @users))))))

(defn main []
  (reagent/render [users-view]
                  (.getElementById js/document "app")))

(main)
