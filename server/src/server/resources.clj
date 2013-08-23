(ns server.resources
  (:require [liberator.core :refer [resource defresource]]
            [cheshire.core :as cheshire]
            [server.db :as db]))

(defresource users []
  :available-media-types ["application/json"]
  :exists?
  (fn [_]
    {:users (db/all-users)})
  :handle-ok
  (fn [context]
    (cheshire/generate-string (:users context))))

(defresource user [id]
  :allowed-methods [:get :put :post]
  :available-media-types ["application/json"]
  :exists?
  (fn [_]
    {:user (db/get-user (. Integer parseInt id))})
  :handle-ok
  (fn [context]
    (cheshire/generate-string (:user context)))
  :put!
  (fn [context]
    (let [body (slurp (get-in context [:request :body]))
          data (cheshire/parse-string body)]
      (db/update-user (-> context :user :id) data)))
  :post!
  (fn [context]
    (let [body (slurp (get-in context [:request :body]))
          data (cheshire/parse-string body)]
      (db/insert-user data))))
