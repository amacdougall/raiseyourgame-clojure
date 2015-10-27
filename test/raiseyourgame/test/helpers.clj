(ns raiseyourgame.test.helpers
  (:require [clj-time.core :as t]
            [clojure.java.jdbc :as jdbc]
            [cheshire.core :as cheshire]
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

(defmacro with-rollback-transaction [args & body]
  `(clojure.java.jdbc/with-db-transaction [~(first args) (deref ~(second args))]
     (jdbc/db-set-rollback-only! ~(first args)) ; force rollback at end of transaction
     (binding [~(second args) (atom ~(first args))]
       ~@body)))
