(ns raiseyourgame.test.helpers
  (:require [clj-time.core :as t]
            [clojure.java.jdbc :as jdbc]
            [cheshire.core :as cheshire]
            [peridot.core :refer [session request]]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]))

(defn json->clj [raw-json]
  (cheshire/parse-string raw-json ->kebab-case-keyword))

;; If the body of the supplied response is a Java InputStream, reads its string
;; value and converts it using json->clj. Otherwise, returns the body
;; unchanged. In general, it should be possible to throw a Ring response at
;; this function and get back usable Clojure data.
(defn response->clj [{body :body}]
  (if (instance? java.io.InputStream body)
    (json->clj (slurp body))
    body))

(defn has-values?
  "True if the candidate map has every key-value pair defined in the exemplar map."
  [exemplar candidate]
  (every? (fn [k] (= (candidate k) (exemplar k))) (keys exemplar)))

(defn collection-has-values?
  "True if the elements in the candidate collection match the elements in the
  exemplar collection, using has-values? on each element pair."
  [exemplars candidates]
  (every? (fn [[e c]] (has-values? e c))
          (partition 2 (interleave exemplars candidates))))

;; Use this function when comparing timestamps that may not be precisely
;; identical. One second's leeway seems fine, since we aren't testing the
;; database itself.
(defn has-approximate-time
  "True if the two times are within a second of one another. Expects two clj-time instances."
  [exemplar candidate]
  (let [timespan (t/interval
                   (t/minus exemplar (t/seconds 1))
                   (t/plus exemplar (t/seconds 1)))]
    (t/within? timespan candidate)))

(defn credentials-for
  "Returns a hash containing only the :username and :password keys for the supplied user."
  [user]
  (select-keys user #{:username :password}))

(defn login-request
  "Given a Peridot session and a user object, generates a Ring request
  representing a login request for that user."
  [session user]
  (request session "/api/users/login"
           :request-method :post
           :content-type "application/json"
           :body (cheshire/generate-string (credentials-for user))))

(defn pg-collation
  "Given a string, returns the string as Postgres interprets it under the us_EN
  collation.

  Strings are customarily sorted in different ways in different locales.
  Postgres embodies this concept in the term \"collation\". In its us_EN
  collation, non-alpha characters, including - and _, are completely ignored.
  Therefore, to verify that a given sort order has been applied to a string
  column, we need to duplicate that collation."
  [s]
  (.toLowerCase (clojure.string/replace s #"[^a-zA-Z0-9]+" "")))

(defn sorted-by
  ""
  ([rows column] (sorted-by rows column :asc))
  ([rows column direction]
   (->> rows
     (map (comp pg-collation column))
     (partition 2 1)
     (map (fn [[a b]] (compare a b)))
     (every? (condp = direction
               :asc (partial > 1)
               :desc (partial < -1))))))

(defmacro with-rollback-transaction [args & body]
  `(clojure.java.jdbc/with-db-transaction [~(first args) (deref ~(second args))]
     (jdbc/db-set-rollback-only! ~(first args)) ; force rollback at end of transaction
     (binding [~(second args) (atom ~(first args))]
       (with-redefs [buddy.hashers/encrypt clojure.string/reverse
                     buddy.hashers/check #(= %1 (clojure.string/reverse %2))]
         ~@body))))
