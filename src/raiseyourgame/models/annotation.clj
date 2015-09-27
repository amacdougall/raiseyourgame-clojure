(ns raiseyourgame.models.annotation
  "Namespace containing database and domain logic for annotation maps."
  (:require [raiseyourgame.db.core :as db]
            [bugsbio.squirrel :refer [to-sql to-clj]]
            [taoensso.timbre :refer [debug]])
  (:import java.sql.SQLException))

;; Note that the raiseyourgame.db.core namespace deals in YeSQL queries, which
;; require SQL-style snake_case params. Use to-sql for those.

(defn create!
  "Creates a annotation based on a params object containing the following keys:
  :video-id, :user-id, :text, :timecode.

  On success, returns the newly created annotation. On failure, returns nil."
  [params]
  (try
    (-> params
      (to-sql)
      (db/create-annotation<! @db/conn)
      (to-clj))
    (catch SQLException e nil)))

;; Only user can really be looked up by more than one unique key; I kept the
;; same interface for all the others for consistency. The lookup methods of all
;; models return a single element. The find-x-by-y methods return sequences.
(defn lookup
  "Given a map with a :annotation-id key, returns the annotation with the
  supplied annotation id, or nil if none was found."
  [criteria]
  (let [results (db/find-annotations-by-annotation-id (to-sql criteria))]
    (when-not (empty? results)
      (to-clj (first results)))))

(defn find-by-video-id
  "Returns all annotations with the supplied video id."
  [video-id]
  (map to-clj (db/find-annotations-by-video-id (to-sql {:video-id video-id}))))

(defn update!
  "Given a annotation model map, updates the database row with that id using
  those values.

  (let [updated-annotation (assoc annotation :text 'This is the best part.')]
    (update! updated-annotation))

  Given a annotation model map and a transition function, applies the function to the
  map and updates the annotation in the database.

  (update! annotation #(assoc :message 'This is the best part.'))

  In both cases, returns the updated annotation if successful, nil otherwise.
  
  If an incomplete annotation map is supplied, mayhem will ensue. Be ready to catch
  SQLExceptions if you're doing something innovative."
  ([video]
   (let [result (db/update-video! (to-sql video))]
     ; result will be the rows affected
     (if (< 0 result) video nil)))
  ([video f]
   (update! (f video))))
