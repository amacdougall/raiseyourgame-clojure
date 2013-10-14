(ns raiseyourgame.resources
  (:require [liberator.core :refer [resource defresource]]
            [cheshire.core :as cheshire]
            [raiseyourgame.db :as db]
            [clojure.java.io :as io]
            [cemerick.austin.repls :refer [browser-connected-repl-js]]
            [net.cgrand.enlive-html :as enlive]
            [ring.util.mime-type :refer [ext-mime-type]]))

(def default-media-types ["application/json" "application/edn;q=0.9"])

(defn inject-repl-js [f]
  (let [action (enlive/append
                 (enlive/html [:script (browser-connected-repl-js)]))
        template (enlive/template f [] [:body] action)]
    ;; template returns a seq of strings; apply str to concatenate
    ;; TODO: this approach may not hold up as index.html gets bigger?
    (apply str (template))))

;; Given a resource context containing a :data key, generates an appropriate
;; representation based on the negotiated media type. We could leave this
;; entirely to Liberator's discretion, but it uses the default Clojure JSON
;; writer, which cannot handle SQL timestamps. Cheshire generates JS-parseable
;; date strings, bless its heart.
;;
;; Of course, this means that when an :exists? function adds data to the
;; context, it must be under the key :data.
(defn represent [context]
  (condp = (get-in context [:representation :media-type])
    "application/json" (cheshire/generate-string (:data context))
    "application/edn" (:data context)))

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

  (defresource index
    :available-media-types ["text/html"]
    :exists?
    (fn [context]
      (let [f (io/file static-dir "index.html")]
        [(.exists f) {::file f}]))
    ;; TODO: this is kind of a hack: we should use the real file
    :handle-ok (fn [{f ::file}] (inject-repl-js f))
    ;; DEBUG: always provide new last-modified timestamp in dev mode
    :last-modified (fn [{f ::file}] (new java.util.Date))))

(defn users
  ([]
   (fn [req]
     (resource
       :available-media-types default-media-types
       :exists?
       (fn [_]
         {:data (db/all-users)})
       :handle-ok represent
       :post!
       (fn [context]
         (let [body (slurp (get-in context [:request :body]))
               data (cheshire/parse-string body)]
           (db/insert-user data))))))
  ([id]
   (fn [req]
     (resource
       :available-media-types default-media-types
       :exists?
       (fn [_]
         {:data (db/get-user (. Integer parseInt id))})
       :handle-ok represent
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
       ; default to JSON, but permit EDN
       :available-media-types default-media-types
       :exists?
       (fn [_]
         {:data (db/all-videos)})
       :handle-ok represent
       :post!
       (fn [context]
         (let [body (slurp (get-in context [:request :body]))
               data (cheshire/parse-string body)]
           (db/insert-video data))))))
  ([id]
   (fn [req]
     (resource
       :available-media-types default-media-types
       :exists?
       (fn [_]
         {:data (db/get-video (. Integer parseInt id))})
       :handle-ok represent
       :put!
       (fn [context]
         (let [body (slurp (get-in context [:request :body]))
               data (cheshire/parse-string body)]
           (db/update-video (-> context :video :id) data)))
       :delete!
       (fn [context]
         (db/delete-video (-> context :video :id)))))))
