(ns server.resources
  (:require [liberator.core :refer [resource defresource]]
            [cheshire.core :as cheshire]
            [server.db :as db]
            [clojure.java.io :as io]
            [cemerick.austin.repls :refer [browser-connected-repl-js]]
            [net.cgrand.enlive-html :as enlive]
            [ring.util.mime-type :refer [ext-mime-type]]))

(let [static-dir (io/file "resources/public")]
  ;; Since this is NOT a parameterized resource, we assign it to a route by
  ;; providing it directly instead of invoking it. This may be a weakness in
  ;; the API, really.
  (defresource static
    :available-media-types
    (fn [context]
      (let [path (get-in context [:request :route-params :*])]
        (if-let [mime-type (ext-mime-type path)]
          [mime-type]
          [])))
    :exists?
    (fn [context]
      (let [path (get-in context [:request :route-params :*])]
        (let [f (io/file static-dir path)]
          [(.exists f) {::file f}])))
    :handle-ok (fn [{f ::file}] f)
    :last-modified (fn [{f ::file}] (.lastModified f)))

  ;; TODO: this creates the template once at server start; in dev mode, at
  ;; least, we want to reload it on each request, or at least when the
  ;; file changes.
  (enlive/deftemplate index-template
    (io/file static-dir "index.html")
    []
    [:body] (enlive/append
              (enlive/html [:script (browser-connected-repl-js)])))

  (defresource index
    :available-media-types ["text/html"]
    :exists?
    (fn [context]
      (let [f (io/file static-dir "index.html")]
        [(.exists f) {::file f}]))
    ;; TODO: this is kind of a hack: we should use the real file
    :handle-ok (fn [{f ::file}] (apply str (index-template)))
    ;; DEBUG: always provide new last-modified timestamp in dev mode
    :last-modified (fn [{f ::file}] (new java.util.Date))))

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
