(ns raiseyourgame.routes
  (:require [re-frame.core :as re-frame]
            [secretary.core :as secretary :refer-macros [defroute]]))

(defroute home "/admin" []
  (re-frame/dispatch [:display-home]))

(defroute users "/admin/users" []
  (re-frame/dispatch [:display-user-list]))

(defn href
  "Given a Secretary route, generates a map containing a :href with the route
  URL, and an :on-click function which prevents the browser's default navigation
  behavior, and invokes the Secretary route instead.

  This map can be used in a Hiccup [:a] element:

  [:a (link routes/users) 'Users']

  Or if more properties are desired:

  [:a (merge (link routes/users) {:class :cool-link}) 'Cool Users']"
  [route]
  (let [url (route)]
    {:href url
     :on-click (fn [event]
                 (.preventDefault event)
                 (secretary/dispatch! url))}))
