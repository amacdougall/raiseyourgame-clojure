(ns raiseyourgame.core
  (:require [cognitect.transit :as transit]
            [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [register-handler dispatch dispatch-sync register-sub subscribe]]
            [secretary.core :as secretary]
            [ajax.core :refer [GET POST]])
  (:require-macros [reagent.ratom :refer [reaction]]
                   [secretary.core :refer [defroute]])
  (:import [goog History]
           [goog.history EventType]))

(def initial-state {:target nil})

(register-handler
  :initialize-db
  (fn [db]
    (merge db initial-state)))

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

(defn display-user-list [db]
  (load-users)
  (assoc db :target {:type :users, :users nil}))

(register-handler
  :display-user-list
  display-user-list)

(defn handle-users-loaded [db [sid users]]
  (assoc db :target {:type :users, :users users}))

(register-handler
  :users-loaded
  handle-users-loaded)

(defn users-query
  [db, [sid]]
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
  (dispatch-sync [:initialize-db])
  (dispatch [:display-user-list])
  (reagent/render [users-view]
                  (.getElementById js/document "app")))

(main)
