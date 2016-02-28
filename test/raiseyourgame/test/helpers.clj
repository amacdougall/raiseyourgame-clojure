(ns raiseyourgame.test.helpers
  (:require [clj-time.core :as t]
            [clojure.java.jdbc :as jdbc]
            [cheshire.core :as cheshire]
            [cognitect.transit :as transit]
            [peridot.core :as peridot]
            [taoensso.timbre :refer [debug]]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]])
  (:import [java.io ByteArrayOutputStream ByteArrayInputStream]))

;; A Peridot session, but with an "accept" header specifying Transit.
(defn session [app]
  (-> (peridot/session app)
    (peridot/header "accept" "application/transit+json")))

(defn json->clj [raw-json]
  (cheshire/parse-string raw-json ->kebab-case-keyword))

(defn transit-read
  "Given an InputStream, reads its contents as Transit, returning Clojure
  data."
  [in]
  (transit/read (transit/reader in :json)))

(defn transit-write
  "Given any Clojure data, writes its contents as Transit, returning a
  string containing the Transit-encoded data."
  [x]
  (let [out (ByteArrayOutputStream. 4096)]
    (-> out (transit/writer :json) (transit/write x))
    (.toString out)))

;; If the body of the supplied response is a Java InputStream, reads its string
;; value and converts it using a technique appropriate for the content type.
;; Otherwise, returns the body unchanged. In general, it should be possible to
;; throw a Ring response at this function and get back usable Clojure data.
(defn response->clj [response]
  (let [body (:body response)
        content-type ((:headers response) "Content-Type")]
    (if (instance? java.io.InputStream body)
      ; content type string can be "type; encoding", so use re-find
      (condp re-find content-type
        #"^application/json"
        (json->clj (slurp body))
        #"^application/transit\+json"
        (transit-read body))
      body)))

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
  "Given a Peridot session and a user object, applies a Peridot request
  representing a login request for that user, and returns the response."
  [session user]
  (peridot/request session "/api/users/login"
                   :request-method :post
                   :content-type "application/transit+json"
                   :body (transit-write (credentials-for user))))

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
