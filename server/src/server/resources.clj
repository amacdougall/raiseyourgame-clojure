(ns server.resources
  (:require [liberator.core :refer [resource defresource]]
            [cheshire.core :as cheshire]
            [server.db :as db]))

(defn users
  ([]
   (fn [req]
     (resource
       :available-media-types ["application/json"]
       :exists?
       (fn [_]
         {:users (db/all-users)})
       :handle-ok
       (fn [context]
         (cheshire/generate-string (:users context)))
       :post!
       (fn [context]
         (let [body (slurp (get-in context [:request :body]))
               data (cheshire/parse-string body)]
           (db/insert-user data))))))
  ([id]
   (fn [req]
     (resource
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
       :delete!
       (fn [context]
         (db/delete-user (-> context :user :id)))))))

(defn videos
  ([]
   (fn [req]
     (resource
       :available-media-types ["application/json"]
       :exists?
       (fn [_]
         {:videos (db/all-videos)})
       :handle-ok
       (fn [context]
         (cheshire/generate-string (:videos context)))
       :post!
       (fn [context]
         (let [body (slurp (get-in context [:request :body]))
               data (cheshire/parse-string body)]
           (db/insert-video data))))))
  ([id]
   (fn [req]
     (resource
       :available-media-types ["application/json"]
       :exists?
       (fn [_]
         {:video (db/get-video (. Integer parseInt id))})
       :handle-ok
       (fn [context]
         (cheshire/generate-string (:video context)))
       :put!
       (fn [context]
         (let [body (slurp (get-in context [:request :body]))
               data (cheshire/parse-string body)]
           (db/update-video (-> context :video :id) data)))
       :delete!
       (fn [context]
         (db/delete-video (-> context :video :id)))))))
