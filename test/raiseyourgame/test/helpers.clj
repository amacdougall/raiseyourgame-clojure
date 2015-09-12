(ns raiseyourgame.test.helpers
  (:require [clj-time.core :as t]))

(defn has-values
  "True if the target map has every key-value pair defined in the exemplar map."
  [exemplar candidate]
  (every? (fn [k] (= (candidate k) (exemplar k))) (keys exemplar)))

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
