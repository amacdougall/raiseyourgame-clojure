(ns raiseyourgame.test.db.users-test
  (:require [raiseyourgame.db.core :as db]
            [raiseyourgame.db.migrations :as migrations]
            [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [clj-time.core :as t]
            [clj-time.coerce :refer [from-date]]
            [conman.core :refer [with-transaction]]
            [environ.core :refer [env]]))

(use-fixtures
  :once
  (fn [f]
    (db/connect!)
    (migrations/migrate ["migrate"])
    (f)))

(defn- has-values
  "True if the target map has every key-value pair defined in the exemplar map."
  [exemplar candidate]
  (every? (fn [k] (= (candidate k) (exemplar k))) (keys exemplar)))

;; Use this function when comparing timestamps that may not be precisely
;; identical. One second's leeway seems fine, since we aren't testing the
;; database itself.
(defn- has-approximate-time
  "True if the two times are within a second of one another. Expects two clj-time instances."
  [exemplar candidate]
  (let [timespan (t/interval
                   (t/minus exemplar (t/seconds 1))
                   (t/plus exemplar (t/seconds 1)))]
    (t/within? timespan candidate)))

(deftest test-user-creation
  (with-transaction [t-conn db/conn]
    ; always rolls back the transaction
    (jdbc/db-set-rollback-only! t-conn)
    (is (= 1 (db/create-user!
               {:username "skurosawa"
                :password "willbehashed"
                :name "Sho Kurosawa"
                :profile "Endless Rain"
                :email "sho@monsterockband.com"
                :user_level 0})))
    (let [user (first (db/get-user-by-email {:email "sho@monsterockband.com"}))]
      (is (has-values
            {:username "skurosawa"
             :password "willbehashed"
             :name "Sho Kurosawa"
             :profile "Endless Rain"
             :email "sho@monsterockband.com"
             :user_level 0
             :last_login nil}
            user))
      (is (has-approximate-time (t/now) (from-date (:created_at user))))
      (is (has-approximate-time (t/now) (from-date (:updated_at user)))))))
