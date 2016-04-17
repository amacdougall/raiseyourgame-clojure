(ns raiseyourgame.subscriptions-test
  (:require [raiseyourgame.db :refer [initial-state]]
            [raiseyourgame.subscriptions :as sub]
            [cljs.test :refer-macros [deftest is testing run-tests async]]))

(deftest test-current-user-query
  (let [user {:username "Alan"}
        db {:current-user user}]
    (is (= (sub/current-user-query db [:current-user-query]) user)))
  (let [db {:current-user nil}]
    (is (= (sub/current-user-query db [:current-user-query]) nil))))

(deftest test-form-values-query
  (let [values {:name "Alan"}
        db {:forms {:login {:values values}}}]
    (is (= (sub/form-values-query db [:form-values-query :login])
           values))))

(deftest test-form-errors-query
  (let [errors {:bad-data? true}
        db {:forms {:login {:errors errors}}}]
    (is (= (sub/form-errors-query db [:form-errors-query :login])
           errors))))

(deftest test-target-type-query
  (let [db {:target {:type :users}}]
    (is (= (sub/target-type-query db [:target-type-query])
           :users))))

(deftest test-users-query
  (let [users [{:username "Ryo"} {:username "Robert"}]
        db {:target {:type :users, :users users}}]
    (is (= (sub/users-query db [:users-query])
           users))))
